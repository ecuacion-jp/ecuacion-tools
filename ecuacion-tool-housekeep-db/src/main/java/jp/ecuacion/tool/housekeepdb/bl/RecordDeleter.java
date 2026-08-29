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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DeleteTargetInfo;
import jp.ecuacion.tool.housekeepdb.util.LogUtil;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil.SqlFragment;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.event.Level;

/**
 * Deletes (or soft-deletes) the one record in a {@link DeleteTargetInfo} table identified by a
 * key value, shared by {@link HousekeepMainTableDeleter} (target table) and
 * {@link HousekeepRelatedTableDeleter} (related table) since both delete a single record the same
 * way once they've settled on which table / key column / key value to use.
 */
public class RecordDeleter {

  private final DetailLogger detailLogger;

  /**
   * Creates the deleter.
   *
   * @param detailLogger the logger to write progress to
   */
  public RecordDeleter(DetailLogger detailLogger) {
    this.detailLogger = detailLogger;
  }

  /**
   * Deletes (or soft-deletes) the one record in {@code target}'s table whose key column equals
   * {@code keyValue}, accumulating the affected row count into {@code tableRecordDeleted} and
   * logging the outcome.
   *
   * @param conn the DB connection of the current task
   * @param target the table to delete (or soft-delete) a record in
   * @param isSoftDelete whether the current task soft-deletes (as opposed to hard-deletes) -
   *     this is a property of the housekeep task as a whole, not of {@code target} itself, so it
   *     is not part of {@link DeleteTargetInfo}
   * @param protocol database kind like 'postgresql', used to type the soft-delete update
   *     timestamp - see {@link SqlUtil#getTimestampNowValue}
   * @param keyValue the value identifying the one record to delete (or soft-delete); read back
   *     from the DB, not typed into the excel config, so it's bound as a JDBC parameter rather
   *     than embedded as SQL literal text - see {@code BoundCondition}'s class Javadoc
   * @param tableRecordDeleted accumulates the delete count per table, keyed by table name
   * @param indents the indent depth of the log lines
   */
  public void deleteOrSoftDeleteOne(Connection conn, DeleteTargetInfo target, boolean isSoftDelete,
      String protocol, Object keyValue, Map<String, Integer> tableRecordDeleted, int indents)
      throws SQLException {

    List<SqlConditionInterface> updateSetList = new ArrayList<>();
    if (isSoftDelete) {
      updateSetList.add(target.getSoftDeleteColumnInfo().getBoundCondition(Boolean.TRUE));

      if (!StringUtils.isEmpty(target.getSoftDeleteUpdateTimestampColumn())) {
        updateSetList.add(target.getSoftDeleteUpdateTimestampColumnInfo()
            .getBoundTimestampNowCondition(protocol));
      }

      if (!StringUtils.isEmpty(target.getSoftDeleteUpdateUserIdColumn())) {
        updateSetList.add(target.getSoftDeleteUpdateUserIdColumnAndValueInfo());
      }
    }

    SqlFragment set = SqlUtil.getUpdateSet(updateSetList);
    String softDeleteSql = "update " + target.getTargetTable() + set.sql();
    String hardDeleteSql = "delete from " + target.getTargetTable();

    List<SqlConditionInterface> whereList = new ArrayList<>();
    whereList.add(target.getDeleteKeyColumnInfo().getBoundCondition(keyValue));

    // When hard-deleting and a soft-delete column is specified, also add a condition that
    // the column is true.
    if (!isSoftDelete && !StringUtils.isEmpty(target.getSoftDeleteColumn())) {
      whereList.add(target.getSoftDeleteColumnInfo().getBoundCondition(Boolean.TRUE));
    }

    SqlFragment where = SqlUtil.getWhere(whereList);
    String sql = (isSoftDelete ? softDeleteSql : hardDeleteSql) + where.sql();

    PreparedStatement delStmt = LogUtil.getStatement(detailLogger, conn, sql,
        SqlUtil.concatBindValues(set, where), "delete", indents);
    int count = delStmt.executeUpdate();

    tableRecordDeleted.merge(target.getTargetTable(), count, Integer::sum);

    delStmt.close();

    LogUtil.logDeleteLines(detailLogger, target.getTargetTable(), count,
        target.getDeleteKeyColumnInfo().getColumn() + " = " + keyValue, Level.TRACE, indents);
  }
}
