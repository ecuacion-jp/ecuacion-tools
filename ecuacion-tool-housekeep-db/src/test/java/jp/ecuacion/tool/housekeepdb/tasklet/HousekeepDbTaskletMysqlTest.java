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
package jp.ecuacion.tool.housekeepdb.tasklet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

/**
 * Runs {@link AbstractHousekeepDbTaskletTest} against H2's MySQL compatibility mode.
 *
 * <p>Unlike PostgreSQL (see {@link HousekeepDbTaskletPostgresqlTest}), no true embedded MySQL
 *     server is available (one that needs neither Docker nor a downloaded native binary), so
 *     {@link H2AsMysqlDriver} is registered to forward {@code jdbc:mysql:} connections to an
 *     in-memory H2 database running with {@code MODE=MySQL}. This still exercises
 *     {@link HousekeepDbTasklet} end-to-end through its real {@code protocol = "mysql"} code
 *     path; only the engine executing the resulting SQL is a stand-in.</p>
 */
@DisplayName("HousekeepDbTasklet (mysql)")
class HousekeepDbTaskletMysqlTest extends AbstractHousekeepDbTaskletTest {

  @SuppressWarnings("null")
  private static String dbName;

  @BeforeAll
  static void registerDriver() throws ClassNotFoundException {
    Class.forName(H2AsMysqlDriver.class.getName());
    dbName = "housekeepdb_mysql_test_" + UUID.randomUUID().toString().replace("-", "");
  }

  @Override
  protected String protocol() {
    return "mysql";
  }

  @Override
  protected String[] dbConnectionRow(String id) {
    return new String[] {id, H2AsMysqlDriver.class.getName(), "mysql", "localhost", "3306",
        dbName, "", "sa", "sa"};
  }

  @Override
  protected Connection newConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:mysql://localhost:3306/" + dbName, "sa", "sa");
  }

  @Override
  protected String timestampColumnType() {
    return "datetime";
  }

  @Override
  protected String timestampDaysAgoExpr(int daysAgo) {
    return "now() - interval '" + daysAgo + "' day";
  }
}
