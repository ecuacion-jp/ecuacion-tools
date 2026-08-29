/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.ecuacion.tool.housekeepdb.bl;

import static jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean.RelatedTableProcessPatternEnum.deleteRelatedTableRecord;
import static jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean.RelatedTableProcessPatternEnum.skipTargetTableRecordDeletion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.tool.housekeepdb.bean.ColumnInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean;
import jp.ecuacion.tool.housekeepdb.util.LogUtil;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil.SqlFragment;
import org.slf4j.event.Level;

/**
 * Checks and deletes (or soft-deletes) related-table records tied to one target-table record, on
 * behalf of {@link HousekeepMainTableDeleter}.
 */
public class HousekeepRelatedTableDeleter {

  private static final int IDT_3 = 3;
  private static final int IDT_4 = 4;
  private static final int IDT_5 = 5;

  private final DetailLogger detailLogger;
  private final RecordDeleter recordDeleter;

  /**
   * Creates the deleter.
   *
   * @param detailLogger the logger to write progress to
   * @param recordDeleter deletes (or soft-deletes) the single related-table record identified by
   *     a linking value, once this class has resolved which one that is
   */
  public HousekeepRelatedTableDeleter(DetailLogger detailLogger, RecordDeleter recordDeleter) {
    this.detailLogger = detailLogger;
    this.recordDeleter = recordDeleter;
  }

  /**
   * Skip deleting if specified related-table record exists.
   *
   * <p>Returning true means that record is skipped to delete.</p>
   *
   * <p>When the task is a soft-delete one, a related record already soft-deleted doesn't count as
   *     "existing" - only a not-yet-soft-deleted row blocks the main record's deletion (the
   *     related-table row's {@code softDeleteColumn} is required in that case - see
   *     {@link RelatedTableInfoBean}'s class Javadoc). For a hard-delete task, plain existence is
   *     checked as before.</p>
   *
   * @param connection the DB connection of the current task
   * @param info the housekeep task settings
   * @param mainSqlRs the current row of the target table's select result
   */
  public boolean needsSkipFromRelatedTableDataCheck(Connection connection, HousekeepInfoBean info,
      ResultSet mainSqlRs) throws SQLException {
    List<RelatedTableInfoBean> relatedSkipList = info.getRelatedRecordTableInfoList().stream()
        .filter(bean -> bean.getRelatedTableProcessPattern() == skipTargetTableRecordDeletion)
        .toList();

    for (RelatedTableInfoBean relatedBean : relatedSkipList) {
      // value below is read back from the DB (not typed into the excel config), so it's bound as
      // a JDBC parameter rather than embedded as SQL literal text - see BoundCondition's class
      // Javadoc.
      Object value = mainSqlRs.getObject(relatedBean.getTargetTableColumn());

      List<SqlConditionInterface> whereList = new ArrayList<>();
      whereList.add(relatedBean.getRelatedTableIdColumnInfo().getBoundCondition(value));

      if (info.isSoftDelete()) {
        whereList.add(relatedBean.getSoftDeleteColumnInfo().getBoundCondition(Boolean.FALSE));
      }

      LogUtil.dlogWithIndent(detailLogger, Level.DEBUG, "Find records from related table.", IDT_3);
      SqlFragment where = SqlUtil.getWhere(whereList);
      String selectSql =
          "select count(*) count from " + relatedBean.getRelatedTable() + where.sql();
      PreparedStatement stmt = LogUtil.getStatement(detailLogger, connection, selectSql,
          where.bindValues(), "related table select", IDT_3);
      ResultSet rs = stmt.executeQuery();

      rs.next();
      Integer integer = rs.getInt("count");
      if (integer > 0) {
        return true;
      }
    }

    return false;
  }

  /**
   * Deletes (or soft-deletes) the related-table records linked to the current target-table
   * record.
   *
   * @param conn the DB connection of the current task
   * @param info the housekeep task settings
   * @param mainSqlRs the current row of the target table's select result
   * @param tableRecordDeleted accumulates the delete count per related table, keyed by table name
   */
  public void deleteRelatedData(Connection conn, HousekeepInfoBean info, ResultSet mainSqlRs,
      Map<String, Integer> tableRecordDeleted) throws SQLException {
    List<RelatedTableInfoBean> list = info.getRelatedRecordTableInfoList().stream()
        .filter(bean -> bean.getRelatedTableProcessPattern() == deleteRelatedTableRecord).toList();

    for (RelatedTableInfoBean relatedInfo : list) {
      // The target-table column value this related-table row is linked by. Read back from the DB
      // (not typed into the excel config), so it's bound as a JDBC parameter rather than embedded
      // as SQL literal text - see BoundCondition's class Javadoc.
      Object linkValue = mainSqlRs.getObject(relatedInfo.getTargetTableColumn());

      // First retrieve the related row via the linking value, both to confirm it still exists
      // (it may already be gone as the side effect of an earlier related-table delete, e.g. a
      // cascading foreign key) and to read back the exact value to delete by.
      ColumnInfoBean fkCol = relatedInfo.getRelatedTableIdColumnInfo();
      SqlFragment linkWhere = SqlUtil.getWhere(fkCol.getBoundCondition(linkValue));
      String sqlTargetSelect = "select " + fkCol.getColumn() + " from "
          + relatedInfo.getRelatedTable() + linkWhere.sql();

      String sqlName = "related table select";
      try (
          PreparedStatement stmt = LogUtil.getStatement(detailLogger, conn, sqlTargetSelect,
              linkWhere.bindValues(), sqlName, IDT_3);
          ResultSet rs = stmt.executeQuery();) {

        boolean recordFound = rs.next();

        String logMsg = !recordFound ? "Record not found."
            : "Record(s) found. " + fkCol.getColumn() + " = " + linkValue;
        LogUtil.dlogWithIndent(detailLogger, Level.DEBUG, logMsg, IDT_4);

        if (!recordFound) {
          // Nothing to delete - already gone, e.g. via an earlier related-table delete cascading
          // onto this one.
          continue;
        }

        // Delete (or soft-delete) the related-table record whose key column holds the value just
        // read back above.
        final Object val = rs.getObject(fkCol.getColumn());
        recordDeleter.deleteOrSoftDeleteOne(conn, relatedInfo, info.isSoftDelete(),
            info.getDbConnectionInfo().getProtocol(), val, tableRecordDeleted, IDT_5);
      }
    }
  }
}
