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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * A JDBC driver test double that forwards {@code jdbc:mysql:} connections to an in-memory H2
 * database running in MySQL compatibility mode.
 *
 * <p>No true embedded MySQL server (one that needs neither Docker nor a downloaded native
 *     binary) exists, unlike {@code io.zonky.test:embedded-postgres} for PostgreSQL. Registering
 *     this driver under the {@code jdbc:mysql:} scheme lets {@link HousekeepDbTasklet} be
 *     exercised end-to-end through its real {@code protocol = "mysql"} code path (see
 *     {@link HousekeepDbTaskletMysqlTest}), while the SQL it builds actually runs against H2.</p>
 */
public class H2AsMysqlDriver implements Driver {

  static {
    try {
      DriverManager.registerDriver(new H2AsMysqlDriver());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @Nullable Connection connect(@SuppressWarnings("null") String url,
      @SuppressWarnings("null") Properties info) throws SQLException {
    if (!acceptsURL(url)) {
      return null;
    }

    String dbName = url.substring(url.lastIndexOf('/') + 1);
    return DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
        info);
  }

  @Override
  public boolean acceptsURL(@SuppressWarnings("null") String url) {
    return url != null && url.startsWith("jdbc:mysql:");
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(@SuppressWarnings("null") String url,
      @SuppressWarnings("null") Properties info) {
    return new DriverPropertyInfo[0];
  }

  @Override
  public int getMajorVersion() {
    return 1;
  }

  @Override
  public int getMinorVersion() {
    return 0;
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }
}
