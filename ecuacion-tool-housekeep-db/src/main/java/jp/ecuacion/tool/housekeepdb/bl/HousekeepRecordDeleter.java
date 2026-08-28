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
import jp.ecuacion.tool.housekeepdb.bean.ColumnInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

/**
 * Connects to the DB configured for one housekeep task and deletes (or soft-deletes) its expired
 * records, in batches of up to {@code maxSelectLines} rows.
 */
public class HousekeepRecordDeleter {

  private static final int IDT_1 = 1;
  private static final int IDT_2 = 2;
  private static final int IDT_3 = 3;
  private static final int IDT_4 = 4;
  private static final int IDT_5 = 5;

  private final DetailLogger detailLogger;
  private final int maxSelectLines;

  /**
   * Creates the deleter.
   *
   * @param detailLogger the logger to write progress to
   * @param maxSelectLines the number of rows selected and committed per loop iteration
   */
  public HousekeepRecordDeleter(DetailLogger detailLogger, int maxSelectLines) {
    this.detailLogger = detailLogger;
    this.maxSelectLines = maxSelectLines;
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
    dlogWithIndent(Level.INFO, logMsg, IDT_1);

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
        dlogWithIndent(Level.INFO, "Find records from target table.", IDT_1);

        // Retrieve IDs up to maxSelectLines rows, continuing after the previous batch.
        String selectSql = getMainSelectSql(info, lastProcessedId);

        try (PreparedStatement stmt =
            getStatement(conn, selectSql, "target table select", IDT_1)) {
          ResultSet rs = stmt.executeQuery();

          // Flag to determine whether the query found any records.
          boolean isQueryResultCountZero = true;

          // Process each retrieved record one by one.
          while (rs.next()) {
            isQueryResultCountZero = false;
            recordFound = true;

            Object idValue = rs.getObject(info.getIdColumnInfo().getColumn());
            String idCol = info.getIdColumnInfo().getColumn();
            dlogWithIndent(Level.DEBUG, "Record found. " + idCol + " = " + idValue, IDT_2);

            // Advance the cursor before the skip check below. Skipped records are not deleted,
            // so they would otherwise be re-selected by every following batch and the records
            // after them never reached.
            lastProcessedId = idValue;

            // Check for data that should be skipped.
            if (needsSkipFromRelatedTableDataCheck(conn, info, rs)) {
              dlogWithIndent(Level.DEBUG, "Not a housekeep target. Skipped", IDT_3);
              continue;
            }

            recordDeleted = true;

            deleteRelatedData(conn, info, idValue, tableRecordDeleted);
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
      dlogWithIndent(Level.INFO, logMsg, IDT_2);
    }

    tableRecordDeleted.keySet().stream().forEach(table -> dlogWithIndent(Level.INFO,
        "Delete lines | table: " + table + ", count: " + tableRecordDeleted.get(table), IDT_1));
  }

  private void dlogWithIndent(Level logLevel, String message, int indents) {
    final String indentString = "  ";

    String indentsString = "";
    for (int i = 0; i < indents; i++) {
      indentsString += indentString;
    }

    detailLogger.log(logLevel, indentsString + message);
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

  private PreparedStatement getStatement(Connection conn, String sql, String sqlName, int indents)
      throws SQLException {

    dlogWithIndent(Level.TRACE, sqlName + " SQL: " + sql, indents);

    return conn.prepareStatement(sql);
  }

  /**
   * Skip deleting if specified related-table record exists.
   *
   * <p>Returning true means that record is skipped to delete.</p>
   */
  private boolean needsSkipFromRelatedTableDataCheck(Connection connection, HousekeepInfoBean info,
      ResultSet mainSqlRs) throws SQLException {
    List<RelatedTableInfoBean> relatedSkipList = info.getRelatedRecordTableInfoList().stream()
        .filter(bean -> bean.getRelatedTableProcessPattern() == skipTargetTableRecordDeletion)
        .toList();

    for (RelatedTableInfoBean relatedBean : relatedSkipList) {
      Object value = mainSqlRs.getObject(relatedBean.getTargetTableColumn());

      dlogWithIndent(Level.DEBUG, "Find records from related table.", IDT_3);
      String selectSql = "select count(*) count from " + relatedBean.getRelatedTable() + " where "
          + relatedBean.getRelatedTableIdColumnInfo().getColumnAndValueInfo(value).getCondition();
      PreparedStatement stmt = getStatement(connection, selectSql, "related table select", IDT_3);
      ResultSet rs = stmt.executeQuery();

      rs.next();
      Integer integer = rs.getInt("count");
      if (integer > 0) {
        return true;
      }
    }

    return false;
  }

  @SuppressWarnings("null")
  private void deleteRelatedData(Connection conn, HousekeepInfoBean info, Object id,
      Map<String, Integer> tableRecordDeleted) throws SQLException {
    List<RelatedTableInfoBean> list = info.getRelatedRecordTableInfoList().stream()
        .filter(bean -> bean.getRelatedTableProcessPattern() == deleteRelatedTableRecord).toList();

    for (RelatedTableInfoBean relatedInfo : list) {
      if (!tableRecordDeleted.containsKey(relatedInfo.getRelatedTable())) {
        tableRecordDeleted.put(relatedInfo.getRelatedTable(), 0);
      }

      // Organize a delete (or update in case of soft delete) statement of a record linked to the id
      // of the target table.

      // Put parameters of the set clause in a update statement
      List<SqlConditionInterface> updateSetList = new ArrayList<>();

      if (info.isSoftDelete()) {
        // '<softDeleteColumn> = true'
        updateSetList.add(relatedInfo.getSoftDeleteColumnInfo().getColumnAndValueInfo("true"));

        // '<SoftDeleteUpdateTimestampColumn> = now()'
        if (!StringUtils.isEmpty(relatedInfo.getSoftDeleteUpdateTimestampColumn())) {
          updateSetList.add(relatedInfo.getSoftDeleteUpdateTimestampColumnInfo()
              .getTimestampColumnNowInfo(info.getDbConnectionInfo().getProtocol()));
        }

        // <SoftDeleteUpdateUserIdColumn = 'xxx'
        if (!StringUtils.isEmpty(relatedInfo.getSoftDeleteUpdateUserIdColumn())) {
          updateSetList.add(relatedInfo.getSoftDeleteUpdateUserIdColumnAndValueInfo());
        }
      }

      // First retrieve the target column value from the target table.
      ColumnInfoBean fkCol = relatedInfo.getRelatedTableIdColumnInfo();
      String sqlTargetSelect =
          "select " + fkCol.getColumn() + " from " + relatedInfo.getRelatedTable() + " where "
              + fkCol.getColumnAndValueInfo(id).getCondition();

      String sqlName = "related table select";
      try (PreparedStatement stmt = getStatement(conn, sqlTargetSelect, sqlName, IDT_3);
          ResultSet rs = stmt.executeQuery();) {

        boolean recordFound = rs.next();

        String logMsg = !recordFound ? "Record not found."
            : "Record(s) found. " + fkCol.getColumnAndValueInfo(id).getCondition();
        dlogWithIndent(Level.DEBUG, logMsg, IDT_4);

        // where clause
        final Object val = rs.getObject(fkCol.getColumn());
        List<SqlConditionInterface> whereList = new ArrayList<>();
        whereList.add(relatedInfo.getRelatedTableIdColumnInfo().getColumnAndValueInfo(val));

        // When hard-deleting and a soft-delete column is specified, also add a condition that
        // the column is true to the WHERE clause.
        if (!info.isSoftDelete() && !StringUtils.isEmpty(relatedInfo.getSoftDeleteColumn())) {
          whereList.add(relatedInfo.getSoftDeleteColumnInfo().getColumnAndValueInfo("true"));
        }

        // Delete records in the related table whose column contains the retrieved value.
        String softDeleteSql =
            "update " + relatedInfo.getRelatedTable() + SqlUtil.getUpdateSet(updateSetList);
        String hardDeleteSql = "delete from " + relatedInfo.getRelatedTable();

        String sql = info.isSoftDelete() ? softDeleteSql : hardDeleteSql;
        sql = sql + SqlUtil.getWhere(whereList);

        PreparedStatement delStmt = getStatement(conn, sql, "related table delete", IDT_5);
        int count = delStmt.executeUpdate();
        tableRecordDeleted.put(relatedInfo.getRelatedTable(),
            tableRecordDeleted.get(relatedInfo.getRelatedTable()) + count);

        delStmt.close();

        logDeleteLines(relatedInfo.getRelatedTable(), count,
            relatedInfo.getRelatedTableIdColumnInfo().getColumnAndValueInfo(val).getCondition(),
            Level.TRACE, IDT_5);
      }
    }
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

    PreparedStatement delStmt = getStatement(conn, sql, "main table delete", IDT_3);
    int count = delStmt.executeUpdate();

    // merge() also covers the case where the statement affected no rows, which happens when the
    // record was already removed as a side effect of deleting a related-table record (a cascading
    // foreign key, say). Accumulating through get() turned that into a NullPointerException.
    tableRecordDeleted.merge(info.getTable(), count, Integer::sum);

    delStmt.close();

    logDeleteLines(info.getTable(), count,
        info.getIdColumnInfo().getColumnAndValueInfo(idValue).getCondition(), Level.TRACE, IDT_3);
  }

  private void logDeleteLines(String table, int count, String condition, Level logLevel,
      int indents) {
    if (logLevel != null) {
      dlogWithIndent(logLevel, table + ": " + count + " record(s) deleted. (" + condition + ")",
          indents);
    }
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
