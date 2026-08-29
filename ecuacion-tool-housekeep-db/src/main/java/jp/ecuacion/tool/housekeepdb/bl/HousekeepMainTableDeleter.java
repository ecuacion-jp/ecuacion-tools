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
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueStringBean;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.util.LogUtil;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil.SqlFragment;
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
  private final RecordDeleter recordDeleter;
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
    this.recordDeleter = new RecordDeleter(detailLogger);
    this.relatedTableDeleter = new HousekeepRelatedTableDeleter(detailLogger, recordDeleter);
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
        SqlFragment selectSql = getMainSelectSql(info, lastProcessedId);

        try (PreparedStatement stmt = LogUtil.getStatement(detailLogger, conn, selectSql.sql(),
            selectSql.bindValues(), "target table select", IDT_1)) {
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
            recordDeleter.deleteOrSoftDeleteOne(conn, info, info.isSoftDelete(),
                info.getDbConnectionInfo().getProtocol(), idValue, tableRecordDeleted, IDT_3);
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
   * @return select statement, with the WHERE clause's bind values (if any)
   */
  private SqlFragment getMainSelectSql(HousekeepInfoBean info, @Nullable Object lastProcessedId) {
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
      whereList.add(info.getSoftDeleteColumnInfo().getBoundCondition(Boolean.FALSE));

    } else {
      // If hard delete and "soft-delete column name" is specified, add to the WHERE clause.
      if (StringUtils.isNotEmpty(info.getSoftDeleteColumn())) {
        whereList.add(info.getSoftDeleteColumnInfo().getBoundCondition(Boolean.TRUE));
      }
    }

    if (lastProcessedId != null) {
      // Keyset paging: continue after the last record walked. Both this comparison and the
      // "order by" below use the id column, so the batches walk the table exactly once.
      // lastProcessedId was read back from the DB (not typed into the excel config), so it's
      // bound as a JDBC parameter rather than embedded as SQL literal text - see
      // BoundCondition's class Javadoc.
      whereList.add(info.getIdColumnInfo().getBoundGreaterThanCondition(lastProcessedId));
    }

    SqlFragment where = SqlUtil.getWhere(whereList);

    String sql = "select * from " + info.getTable() + where.sql() + " order by "
        + info.getIdColumnInfo().getColumn() + " limit " + maxSelectLines;
    return new SqlFragment(sql, where.bindValues());
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
