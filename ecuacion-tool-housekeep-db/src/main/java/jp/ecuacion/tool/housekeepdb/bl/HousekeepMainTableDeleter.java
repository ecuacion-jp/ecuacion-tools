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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueStringBean;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.util.LogUtil;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

/**
 * Connects to the DB configured for one housekeep task, walks its target table in batches of up
 * to {@code maxSelectLines} rows, and deletes (or soft-deletes) each expired record - delegating
 * the related-table skip check and cleanup to {@link HousekeepRelatedTableDeleter}.
 */
public class HousekeepMainTableDeleter {

  private static final int IDT_1 = 1;
  private static final int IDT_2 = 2;
  private static final int IDT_3 = 3;

  private final DetailLogger detailLogger;
  private final int maxSelectLines;
  private final HousekeepRelatedTableDeleter relatedTableDeleter;

  /**
   * Creates the deleter.
   *
   * @param detailLogger the logger to write progress to
   * @param maxSelectLines the number of rows selected and committed per loop iteration
   */
  public HousekeepMainTableDeleter(DetailLogger detailLogger, int maxSelectLines) {
    this.detailLogger = detailLogger;
    this.maxSelectLines = maxSelectLines;
    this.relatedTableDeleter = new HousekeepRelatedTableDeleter(detailLogger);
  }

  /**
   * Deletes (or soft-deletes) all records the given task targets, connecting to the DB it
   * specifies.
   *
   * @param dbConnectionInfoMap db connection settings by ID, keyed as read from the excel file
   * @param info the housekeep task to execute
   */
  public void execute(Map<String, DbConnectionInfoBean> dbConnectionInfoMap,
      HousekeepInfoBean info) throws ClassNotFoundException, SQLException {

    String logMsg = "DB Connection ID: " + info.getDbConnectionInfoId() + " / "
        + (info.isSoftDelete() ? "Soft Delete" : "Hard Delete") + " / " + "Table Name: "
        + info.getTable();
    LogUtil.dlogWithIndent(detailLogger, Level.INFO, logMsg, IDT_1);

    Map<String, Integer> tableRecordDeleted = new LinkedHashMap<>();

    // DB Connection settings
    try (Connection conn = connectionSettings(dbConnectionInfoMap, info)) {

      // Paging cursor: the id of the last record walked. Records are walked in id order and
      // each batch continues after this value, so the loop terminates by reaching the end of
      // the table rather than by failing to delete anything.
      @Nullable
      Object lastProcessedId = null;
      boolean recordFound = false;
      boolean recordDeleted = false;

      // Process in batches of maxSelectLines even when there are many records.
      while (true) {
        LogUtil.dlogWithIndent(detailLogger, Level.INFO, "Find records from target table.", IDT_1);

        // Retrieve IDs up to maxSelectLines rows, continuing after the previous batch.
        String selectSql = getMainSelectSql(info, lastProcessedId);

        try (PreparedStatement stmt =
            LogUtil.getStatement(detailLogger, conn, selectSql, "target table select", IDT_1)) {
          ResultSet rs = stmt.executeQuery();

          // Flag to determine whether the query found any records.
          boolean isQueryResultCountZero = true;

          // Process each retrieved record one by one.
          while (rs.next()) {
            isQueryResultCountZero = false;
            recordFound = true;

            Object idValue = rs.getObject(info.getIdColumnInfo().getColumn());
            String idCol = info.getIdColumnInfo().getColumn();
            LogUtil.dlogWithIndent(detailLogger, Level.DEBUG,
                "Record found. " + idCol + " = " + idValue, IDT_2);

            // Advance the cursor before the skip check below. Skipped records are not deleted,
            // so they would otherwise be re-selected by every following batch and the records
            // after them never reached.
            lastProcessedId = idValue;

            // Check for data that should be skipped.
            if (relatedTableDeleter.needsSkipFromRelatedTableDataCheck(conn, info, rs)) {
              LogUtil.dlogWithIndent(detailLogger, Level.DEBUG, "Not a housekeep target. Skipped",
                  IDT_3);
              continue;
            }

            recordDeleted = true;

            relatedTableDeleter.deleteRelatedData(conn, info, rs, tableRecordDeleted);
            deleteTargetData(conn, info, idValue, tableRecordDeleted);
          }

          // Terminate when the end of the target table is reached.
          if (isQueryResultCountZero) {
            break;
          }

          conn.commit();
        }
      }

      logMsg = !recordFound ? "Record not found."
          : recordDeleted ? "Record(s) deleted."
              : "Record(s) found, but no deletable one(s) only.";
      LogUtil.dlogWithIndent(detailLogger, Level.INFO, logMsg, IDT_2);
    }

    tableRecordDeleted.keySet().stream().forEach(table -> LogUtil.dlogWithIndent(detailLogger,
        Level.INFO, "Delete lines | table: " + table + ", count: " + tableRecordDeleted.get(table),
        IDT_1));
  }

  /**
   * Builds the select statement finding the next batch of housekeep target records.
   *
   * @param info the housekeep task settings
   * @param lastProcessedId the id of the last record walked, or {@code null} for the first batch
   * @return select statement
   */
  private String getMainSelectSql(HousekeepInfoBean info, @Nullable Object lastProcessedId) {
    // Build the WHERE clause.
    List<SqlConditionInterface> whereList = new ArrayList<>();

    whereList.addAll(
        info.getWhereConditionInfoList().stream().map(e -> e.getConditionColumnInfo()).toList());

    if (info.timestampColumnDefines()) {
      whereList.add(new ColumnAndValueStringBean(
          SqlUtil.getExpirationCondition(info.getDbConnectionInfo().getProtocol(),
              info.getTimestampColumn(), info.getDeleteTargetInDays())));
    }

    if (info.isSoftDelete()) {
      // To avoid updating already-processed records, target only rows where the soft-delete
      // flag is not set.
      whereList.add(new ColumnAndValueInfoBean(info.getSoftDeleteColumn(), false, "false"));

    } else {
      // If hard delete and "soft-delete column name" is specified, add to the WHERE clause.
      if (StringUtils.isNotEmpty(info.getSoftDeleteColumn())) {
        whereList.add(new ColumnAndValueInfoBean(info.getSoftDeleteColumn(), false, "true"));
      }
    }

    if (lastProcessedId != null) {
      // Keyset paging: continue after the last record walked. Both this comparison and the
      // "order by" below use the id column, so the batches walk the table exactly once.
      ColumnAndValueInfoBean idInfo = info.getIdColumnInfo().getColumnAndValueInfo(lastProcessedId);
      whereList.add(new ColumnAndValueStringBean(
          idInfo.getColumn() + " > " + idInfo.surroundWithQuotationMarks()));
    }

    String where = SqlUtil.getWhere(whereList);

    return "select * from " + info.getTable() + where + " order by "
        + info.getIdColumnInfo().getColumn() + " limit " + maxSelectLines;
  }

  private Connection connectionSettings(Map<String, DbConnectionInfoBean> dbConnectionInfoMap,
      HousekeepInfoBean info) throws ClassNotFoundException, SQLException {
    DbConnectionInfoBean dbInfo = dbConnectionInfoMap.get(info.getDbConnectionInfoId());
    if (dbInfo == null) {
      new Violations().add(new BusinessViolation("MSG_ERR_DB_CONNECITON_INFO_ID_NOT_EXIST",
          info.getDbConnectionInfoId())).throwIfAny();
    }

    Objects.requireNonNull(dbInfo);

    Class.forName(dbInfo.getDriverName());
    Connection conn = DriverManager.getConnection(getDbConnectionUrl(dbInfo), dbInfo.getUsername(),
        dbInfo.getPassword());
    conn.setAutoCommit(false);
    return conn;
  }

  private void deleteTargetData(Connection conn, HousekeepInfoBean info, Object idValue,
      Map<String, Integer> tableRecordDeleted) throws SQLException {

    List<SqlConditionInterface> updateSetList = new ArrayList<>();
    if (info.isSoftDelete()) {
      updateSetList.add(info.getSoftDeleteColumnInfo().getColumnAndValueInfo("true"));

      if (!StringUtils.isEmpty(info.getSoftDeleteUpdateTimestampColumn())) {
        updateSetList.add(info.getSoftDeleteUpdateTimestampColumnInfo()
            .getTimestampColumnNowInfo(info.getDbConnectionInfo().getProtocol()));
      }

      if (!StringUtils.isEmpty(info.getSoftDeleteUpdateUserIdColumn())) {
        updateSetList.add(info.getSoftDeleteUpdateUserIdColumnAndValueInfo());
      }
    }

    String softDeleteSql = "update " + info.getTable() + SqlUtil.getUpdateSet(updateSetList);
    String hardDeleteSql = "delete from " + info.getTable();

    List<SqlConditionInterface> whereList = new ArrayList<>();
    whereList.add(info.getIdColumnInfo().getColumnAndValueInfo(idValue));

    // When hard-deleting and a soft-delete column is specified, also add a condition that
    // the column is true.
    if (!info.isSoftDelete() && !StringUtils.isEmpty(info.getSoftDeleteColumn())) {
      whereList.add(info.getSoftDeleteColumnInfo().getColumnAndValueInfo("true"));
    }

    String sql = info.isSoftDelete() ? softDeleteSql : hardDeleteSql;
    sql = sql + SqlUtil.getWhere(whereList);

    PreparedStatement delStmt =
        LogUtil.getStatement(detailLogger, conn, sql, "main table delete", IDT_3);
    int count = delStmt.executeUpdate();

    // merge() also covers the case where the statement affected no rows, which happens when the
    // record was already removed as a side effect of deleting a related-table record (a cascading
    // foreign key, say). Accumulating through get() turned that into a NullPointerException.
    tableRecordDeleted.merge(info.getTable(), count, Integer::sum);

    delStmt.close();

    LogUtil.logDeleteLines(detailLogger, info.getTable(), count,
        info.getIdColumnInfo().getColumnAndValueInfo(idValue).getCondition(), Level.TRACE, IDT_3);
  }

  private String getDbConnectionUrl(DbConnectionInfoBean dbInfo) {
    // "currentSchema" is a postgresql-specific JDBC URL parameter; MySQL / MariaDB have no
    // equivalent (there, "database" and "schema" are the same thing).
    String param =
        dbInfo.getProtocol().equals("postgresql") && StringUtils.isNotEmpty(dbInfo.getSchema())
            ? "?currentSchema=" + dbInfo.getSchema()
            : "";
    return "jdbc:" + dbInfo.getProtocol() + "://" + dbInfo.getServer() + ":" + dbInfo.getPort()
        + "/" + dbInfo.getDatabase() + param;
  }
}
