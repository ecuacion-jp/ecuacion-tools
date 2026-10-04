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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.util.SplibLogUtil;
import jp.ecuacion.splib.core.util.SplibLogUtil.LogKeyValue;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.util.AppLogUtil;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil.SqlFragment;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

/**
 * Connects to the DB configured for one abnormal data check task and finds the records matching
 * its conditions - records that shouldn't exist, so finding any is reported as a warning
 * instead of deleting them.
 */
public class HousekeepAbnormalDataChecker {

  /** The maximum number of IDs of found records listed in the result (and so the email). */
  public static final int MAX_LISTED_IDS = 10;

  private final DetailLogger detailLogger;

  /**
   * Holds the records an abnormal data check task found.
   *
   * @param taskId the task ID
   * @param table the table checked
   * @param idColumn the ID column of {@code table}
   * @param count the number of records found
   * @param ids the IDs of the first (in ID order) records found, up to {@link #MAX_LISTED_IDS}
   */
  public record Result(String taskId, String table, String idColumn, int count, List<Object> ids) {

    /**
     * Returns whether more records were found than {@link #ids()} lists.
     *
     * @return {@code true} if more records were found than listed
     */
    public boolean hasMore() {
      return count > ids.size();
    }
  }

  /**
   * Creates the checker.
   *
   * @param detailLogger the logger to write progress to
   */
  public HousekeepAbnormalDataChecker(DetailLogger detailLogger) {
    this.detailLogger = detailLogger;
  }

  /**
   * Finds the records the given abnormal data check task targets, connecting to the DB it
   * specifies. Nothing is updated.
   *
   * @param dbConnectionInfoMap db connection settings by ID, keyed as read from the excel file
   * @param info the abnormal data check task to execute
   * @return the records found, {@code null} if none found
   */
  public @Nullable Result execute(Map<String, DbConnectionInfoBean> dbConnectionInfoMap,
      HousekeepInfoBean info) throws ClassNotFoundException, SQLException {

    SplibLogUtil.logKeyValueList(detailLogger, Level.DEBUG, 2,
        List.of(new LogKeyValue("DB Connection ID", String.valueOf(info.getDbConnectionInfoId())),
            new LogKeyValue("Process Kind", "Abnormal Data Check")));

    String idColumn = info.getIdColumnInfo().getColumn();
    SqlFragment where = SqlUtil.getWhere(getConditions(info));

    try (Connection conn =
        HousekeepMainTableDeleter.connectionSettings(dbConnectionInfoMap, info)) {

      int count;
      String countSql = "select count(*) from " + info.getTable() + where.sql();
      try (PreparedStatement stmt = AppLogUtil.getStatement(detailLogger, conn, countSql,
          where.bindValues(), "abnormal data check count", 2);
          ResultSet rs = stmt.executeQuery()) {
        rs.next();
        count = rs.getInt(1);
      }

      if (count == 0) {
        AppLogUtil.log(detailLogger, Level.INFO, "No record found in " + info.getTable() + ".",
            2);
        return null;
      }

      List<Object> ids = new ArrayList<>();
      String idSql = "select " + idColumn + " from " + info.getTable() + where.sql()
          + " order by " + idColumn + " limit " + MAX_LISTED_IDS;
      try (PreparedStatement stmt = AppLogUtil.getStatement(detailLogger, conn, idSql,
          where.bindValues(), "abnormal data check id select", 2);
          ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          ids.add(rs.getObject(idColumn));
        }
      }

      Result result = new Result(info.getTaskId(), info.getTable(), idColumn, count, ids);
      AppLogUtil.log(detailLogger, Level.WARN,
          "Record(s) that should not exist found : " + count + " record(s) in "
              + info.getTable() + " (" + idColumn + ": " + getIdsText(result) + ")",
          2);

      return result;
    }
  }

  /**
   * Builds the WHERE conditions of the check: those common to every task, plus - when a
   * soft-delete column is configured - excluding the records already soft-deleted.
   */
  private List<SqlConditionInterface> getConditions(HousekeepInfoBean info) {
    List<SqlConditionInterface> whereList =
        HousekeepMainTableDeleter.getSearchAndExpirationConditions(info);

    if (StringUtils.isNotEmpty(info.getSoftDeleteColumn())) {
      whereList.add(info.getSoftDeleteColumnInfo().getBoundCondition(false));
    }

    return whereList;
  }

  /**
   * Returns the IDs listed in {@code result} as a comma-separated text, followed by a note when
   * more records were found than listed.
   *
   * @param result the result of an abnormal data check task
   * @return the IDs text
   */
  public static String getIdsText(Result result) {
    return String.join(", ", result.ids().stream().map(String::valueOf).toList())
        + (result.hasMore() ? ", ... (and more)" : "");
  }
}
