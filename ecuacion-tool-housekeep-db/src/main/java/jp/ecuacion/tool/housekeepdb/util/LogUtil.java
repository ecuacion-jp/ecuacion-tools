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
package jp.ecuacion.tool.housekeepdb.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.slf4j.event.Level;

/**
 * Provides the indented {@link DetailLogger} logging shared by the DB-facing bl classes.
 */
public class LogUtil {

  /**
   * Prevents other classes from instantiating it.
   */
  private LogUtil() {

  }

  private static final String INDENT_STRING = "  ";

  /**
   * Logs {@code message} indented {@code indents} levels deep.
   *
   * @param detailLogger the logger to write to
   * @param logLevel the level to log at
   * @param message the message to log
   * @param indents the indent depth
   */
  public static void dlogWithIndent(DetailLogger detailLogger, Level logLevel, String message,
      int indents) {
    String indentsString = "";
    for (int i = 0; i < indents; i++) {
      indentsString += INDENT_STRING;
    }

    detailLogger.log(logLevel, indentsString + message);
  }

  /**
   * Prepares {@code sql}, logging it at {@code TRACE} first.
   *
   * @param detailLogger the logger to write to
   * @param conn the connection to prepare the statement on
   * @param sql the sql to prepare
   * @param sqlName a short label identifying what the sql does, for the log line
   * @param indents the indent depth of the log line
   * @return the prepared statement
   */
  public static PreparedStatement getStatement(DetailLogger detailLogger, Connection conn,
      String sql, String sqlName, int indents) throws SQLException {

    dlogWithIndent(detailLogger, Level.TRACE, sqlName + " SQL: " + sql, indents);

    return conn.prepareStatement(sql);
  }

  /**
   * Logs how many rows a delete (or soft-delete update) statement affected.
   *
   * @param detailLogger the logger to write to
   * @param table the table the statement ran against
   * @param count the number of rows affected
   * @param condition the where-condition text, for the log line
   * @param logLevel the level to log at
   * @param indents the indent depth of the log line
   */
  public static void logDeleteLines(DetailLogger detailLogger, String table, int count,
      String condition, Level logLevel, int indents) {
    if (logLevel != null) {
      dlogWithIndent(detailLogger, logLevel,
          table + ": " + count + " record(s) deleted. (" + condition + ")", indents);
    }
  }
}
