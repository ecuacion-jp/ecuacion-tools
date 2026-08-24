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

import static org.assertj.core.api.Assertions.assertThat;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Runs {@link AbstractHousekeepDbTaskletTest} against a real PostgreSQL instance launched by
 * {@code io.zonky.test:embedded-postgres} (no Docker required, works the same in GitHub Actions
 * and on an offline Jenkins build server), because the SQL built by {@link HousekeepDbTasklet}
 * relies on PostgreSQL-specific syntax (e.g. the {@code 'timestamp' - column > 'n days'} interval
 * subtraction) that an HSQLDB/H2 stand-in would not reproduce faithfully.
 */
@DisplayName("HousekeepDbTasklet (postgresql)")
class HousekeepDbTaskletPostgresqlTest extends AbstractHousekeepDbTaskletTest {

  @SuppressWarnings("null")
  private static EmbeddedPostgres postgres;
  private static int port;

  @BeforeAll
  static void startPostgres() throws IOException {
    postgres = EmbeddedPostgres.start();
    port = postgres.getPort();
  }

  @AfterAll
  static void stopPostgres() throws IOException {
    postgres.close();
  }

  @Override
  protected String protocol() {
    return "postgresql";
  }

  @Override
  protected String[] dbConnectionRow(String id) {
    return new String[] {id, "org.postgresql.Driver", "postgresql", "localhost",
        String.valueOf(port), "postgres", "", "postgres", "postgres"};
  }

  @Override
  protected Connection newConnection() throws SQLException {
    return DriverManager
        .getConnection("jdbc:postgresql://localhost:" + port + "/postgres?user=postgres");
  }

  @Override
  protected String timestampColumnType() {
    return "timestamptz";
  }

  @Override
  protected String localTimestampColumnType() {
    return "timestamp";
  }

  @Override
  protected String timestampDaysAgoExpr(int daysAgo) {
    return "now() - interval '" + daysAgo + " days'";
  }

  // -------------------------------------------------------------------------
  // schema (postgresql only)
  // -------------------------------------------------------------------------

  /**
   * Covers the {@code ?currentSchema=} JDBC URL parameter, which
   * {@link HousekeepDbTasklet} appends for PostgreSQL only - MySQL / MariaDB have no equivalent,
   * so this cannot live in {@link AbstractHousekeepDbTaskletTest}.
   */
  @Nested
  @DisplayName("\"Connection URL: Schema\" setting")
  class SchemaSetting {

    @Test
    @DisplayName("an unqualified table name resolves inside the configured schema")
    void resolvesTableInsideConfiguredSchema() throws Exception {
      execute("create schema hk_schema");
      // Same table name in both schemas: only the one in hk_schema may be housekept.
      execute("create table hk_schema.sc_scoped (num1 integer primary key)");
      execute("create table public.sc_scoped (num1 integer primary key)");
      execute("insert into hk_schema.sc_scoped values (1)");
      execute("insert into public.sc_scoped values (1)");

      String[] connectionRow = dbConnectionRow("conn1");
      connectionRow[6] = "hk_schema";

      Path excel = buildExcelFile(List.<String[]>of(connectionRow),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "sc_scoped", "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from hk_schema.sc_scoped")).isZero();
      assertThat(countRows("select count(*) from public.sc_scoped")).isEqualTo(1);
    }
  }
}
