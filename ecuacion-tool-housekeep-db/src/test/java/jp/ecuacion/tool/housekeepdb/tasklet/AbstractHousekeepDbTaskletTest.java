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
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.WhereConditionInfoBean;
import jp.ecuacion.tool.housekeepdb.lang.LangExcel;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * Integration tests for {@link HousekeepDbTasklet}, common to every supported database.
 *
 * <p>Subclasses provide the actual database ({@link HousekeepDbTaskletPostgresqlTest} for
 *     PostgreSQL, {@link HousekeepDbTaskletMysqlTest} for MySQL / MariaDB) plus the small set of
 *     DDL fragments that aren't portable across the two SQL dialects.</p>
 */
abstract class AbstractHousekeepDbTaskletTest {

  /** Database kind, as written to the "protocol" column / the Info sheet's "database" row. */
  protected abstract String protocol();

  /** A single "DB Connection Settings" row pointing at the test database. */
  protected abstract String[] dbConnectionRow(String id);

  /** Opens a direct JDBC connection to the test database, for test setup/verification SQL. */
  protected abstract Connection newConnection() throws SQLException;

  /** Column type usable for a timestamp column in this dialect's DDL. */
  protected abstract String timestampColumnType();

  /** SQL expression yielding a point in time {@code daysAgo} days before now. */
  protected abstract String timestampDaysAgoExpr(int daysAgo);

  protected void execute(String sql) throws SQLException {
    try (Connection conn = newConnection(); Statement st = conn.createStatement()) {
      st.execute(sql);
    }
  }

  protected int countRows(String sql) throws SQLException {
    try (Connection conn = newConnection(); Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      rs.next();
      return rs.getInt(1);
    }
  }

  @SuppressWarnings("null")
  private static void runTasklet(Path excelFile) throws Exception {
    RepeatStatus status = new HousekeepDbTasklet(excelFile.toString(), 1000)
        .execute(mock(StepContribution.class), mock(ChunkContext.class));

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
  }

  private Path buildExcelFile(List<String[]> dbConnectionRows, List<String[]> housekeepRows,
      List<String[]> relatedRows, List<String[]> searchRows) throws IOException {
    LangExcel lang = new LangExcel(Locale.of("en"));

    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      writeSheet(wb, "Info", new String[] {"item", "value"},
          List.<String[]>of(new String[] {"locale", "en"}, new String[] {"format-version", "1.3.0"},
              new String[] {"database", protocol()}));
      writeSheet(wb, lang.get(LangExcel.DB_CONNECTION_SETTINGS),
          lang.getHeaderLabels(DbConnectionInfoBean.HEADER_LABEL_KEYS), dbConnectionRows);
      writeSheet(wb, lang.get(LangExcel.HOUSEKEEP_DB_SETTINGS),
          lang.getHeaderLabels(HousekeepInfoBean.HEADER_LABEL_KEYS), housekeepRows);
      writeSheet(wb, lang.get(LangExcel.RELATED_TABLE_SETTINGS),
          lang.getHeaderLabels(RelatedTableInfoBean.HEADER_LABEL_KEYS), relatedRows);
      writeSheet(wb, lang.get(LangExcel.SEARCH_CONDITION_SETTINGS),
          lang.getHeaderLabels(WhereConditionInfoBean.HEADER_LABEL_KEYS), searchRows);

      Path path = Files.createTempFile("housekeep-db-test-", ".xlsx");
      try (OutputStream os = Files.newOutputStream(path)) {
        wb.write(os);
      }
      return path;
    }
  }

  private static void writeSheet(XSSFWorkbook wb, String sheetName, String[] headers,
      List<String[]> rows) {
    Sheet sheet = wb.createSheet(sheetName);

    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      headerRow.createCell(i).setCellValue(headers[i]);
    }

    int rowNum = 1;
    for (String[] row : rows) {
      Row excelRow = sheet.createRow(rowNum++);
      for (int i = 0; i < row.length; i++) {
        @Nullable String value = row[i];
        if (value != null) {
          excelRow.createCell(i).setCellValue(value);
        }
      }
    }
  }

  // -------------------------------------------------------------------------
  // hard delete
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("hard delete")
  class HardDelete {

    @Test
    @DisplayName("with no filter columns configured, deletes every row in the target table")
    void deletesAllRowsWhenUnfiltered() throws Exception {
      execute("create table hd_basic (num1 integer primary key, char1 varchar(20))");
      execute("insert into hd_basic values (1, 'a'), (2, 'b')");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(
              new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "hd_basic", "num1",
                  "(none)", null, null, null, null, null, null, null, null}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from hd_basic")).isZero();
    }

    @Test
    @DisplayName("with a soft-delete column configured, only purges rows whose flag is already "
        + "true (idColumnLiteralSymbol quotes(') exercises the string-quoting path)")
    void hardDeleteWithSoftDeleteColumnOnlyPurgesFlaggedRows() throws Exception {
      execute(
          "create table hd_withflag (code varchar(20) primary key, rem_flg boolean default false)");
      execute("insert into hd_withflag values ('flagged', true), ('not-flagged', false)");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(
              new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "hd_withflag",
                  "code", "quotes(')", null, null, null, "rem_flg", null, null, null, null}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from hd_withflag where code = 'flagged'")).isZero();
      assertThat(countRows("select count(*) from hd_withflag where code = 'not-flagged'"))
          .isEqualTo(1);
    }
  }

  // -------------------------------------------------------------------------
  // soft delete
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("soft delete")
  class SoftDelete {

    @Test
    @DisplayName("sets the delete flag instead of removing the row")
    void setsFlagInsteadOfDeleting() throws Exception {
      execute("create table sd_basic (num1 integer primary key, rem_flg boolean default false)");
      execute("insert into sd_basic values (1, false), (2, false)");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Soft Delete", "SOFT_DELETE", "sd_basic",
              "num1", "(none)", null, null, null, "rem_flg", null, null, null, null}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from sd_basic")).isEqualTo(2);
      assertThat(countRows("select count(*) from sd_basic where num1 = 1 and rem_flg = true"))
          .isEqualTo(1);
      assertThat(countRows("select count(*) from sd_basic where num1 = 2 and rem_flg = true"))
          .isEqualTo(1);
    }

    @Test
    @DisplayName("a soft-deleted row is excluded from subsequent runs (rem_flg = false guard)")
    void alreadySoftDeletedRowIsSkippedOnRerun() throws Exception {
      execute("create table sd_rerun (num1 integer primary key, rem_flg boolean default false, "
          + "upd_cnt integer default 0)");
      execute("insert into sd_rerun values (1, true, 0)");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Soft Delete", "SOFT_DELETE", "sd_rerun",
              "num1", "(none)", null, null, null, "rem_flg", null, null, null, null}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from sd_rerun where upd_cnt = 0")).isEqualTo(1);
    }

    @Test
    @DisplayName("also updates the configured timestamp and user-id columns")
    void updatesTimestampAndUserIdColumns() throws Exception {
      execute("create table sd_audit (num1 integer primary key, rem_flg boolean default false, "
          + "upd_at " + timestampColumnType() + ", upd_by varchar(20))");
      execute("insert into sd_audit values (1, false, null, null)");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Soft Delete", "SOFT_DELETE", "sd_audit",
              "num1", "(none)", null, null, null, "rem_flg", "upd_at", "upd_by", "quotes(')",
              "SYSTEM"}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows(
          "select count(*) from sd_audit where rem_flg = true and upd_at is not null "
              + "and upd_by = 'SYSTEM'")).isEqualTo(1);
    }
  }

  // -------------------------------------------------------------------------
  // expiration-based filtering
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("expiration-based filtering")
  class ExpirationFiltering {

    @Test
    @DisplayName("only deletes rows whose timestamp column is older than deleteTargetInDays")
    void onlyDeletesExpiredRows() throws Exception {
      execute("create table exp_basic (num1 integer primary key, last_updated "
          + timestampColumnType() + ")");
      execute("insert into exp_basic values (1, " + timestampDaysAgoExpr(100) + ")");
      execute("insert into exp_basic values (2, " + timestampDaysAgoExpr(1) + ")");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "exp_basic",
              "num1", "(none)", "last_updated", "OffsetDateTime", "30", null, null, null, null,
              null}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from exp_basic where num1 = 1")).isZero();
      assertThat(countRows("select count(*) from exp_basic where num1 = 2")).isEqualTo(1);
    }
  }

  // -------------------------------------------------------------------------
  // related table settings
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("related table settings")
  class RelatedTableSettings {

    @Test
    @DisplayName("'Delete' pattern deletes the related-table rows before the target row")
    void deletePatternCascades() throws Exception {
      execute("create table rt_parent (num1 integer primary key, child_code varchar(20))");
      execute("create table rt_child (code varchar(20) primary key)");
      execute("insert into rt_parent values (1, 'c1')");
      execute("insert into rt_child values ('c1')");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "rt_parent",
              "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "HARD_DELETE", "Delete", "DELETE", "child_code",
              "rt_child", "code", "quotes(')", null, null, null, null, null}),
          List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from rt_parent")).isZero();
      assertThat(countRows("select count(*) from rt_child")).isZero();
    }

    @Test
    @DisplayName("'Check and Skip Delete' pattern leaves the target row untouched "
        + "when a related row exists")
    void checkAndSkipDeletePatternSkipsWhenRelatedRowExists() throws Exception {
      execute("create table rt_parent2 (num1 integer primary key, child_code varchar(20))");
      execute("create table rt_child2 (code varchar(20) primary key)");
      execute("insert into rt_parent2 values (1, 'c1')");
      execute("insert into rt_child2 values ('c1')");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "rt_parent2",
              "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "HARD_DELETE", "Check and Skip Delete",
              "CHECK_AND_SKIP_DELETE", "child_code", "rt_child2", "code", "quotes(')", null, null,
              null, null, null}),
          List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from rt_parent2")).isEqualTo(1);
    }

    @Test
    @DisplayName("'Check and Skip Delete' pattern deletes the target row "
        + "when no related row exists")
    void checkAndSkipDeletePatternDeletesWhenNoRelatedRow() throws Exception {
      execute("create table rt_parent3 (num1 integer primary key, child_code varchar(20))");
      execute("create table rt_child3 (code varchar(20) primary key)");
      execute("insert into rt_parent3 values (1, 'c1')");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "rt_parent3",
              "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "HARD_DELETE", "Check and Skip Delete",
              "CHECK_AND_SKIP_DELETE", "child_code", "rt_child3", "code", "quotes(')", null, null,
              null, null, null}),
          List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from rt_parent3")).isZero();
    }
  }

  // -------------------------------------------------------------------------
  // search condition settings
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("search condition settings")
  class SearchConditionSettings {

    @Test
    @DisplayName("adds an extra AND condition, narrowing the deleted rows")
    void narrowsDeletionWithExtraCondition() throws Exception {
      execute("create table sc_basic (num1 integer primary key, status varchar(20))");
      execute("insert into sc_basic values (1, 'COMPLETED'), (2, 'RUNNING')");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "sc_basic",
              "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.of(),
          List.<String[]>of(new String[] {"task-1", "status", "quotes(')", "COMPLETED"}));

      runTasklet(excel);

      assertThat(countRows("select count(*) from sc_basic where num1 = 1")).isZero();
      assertThat(countRows("select count(*) from sc_basic where num1 = 2")).isEqualTo(1);
    }
  }

  // -------------------------------------------------------------------------
  // multiple tasks
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("multiple tasks")
  class MultipleTasks {

    @Test
    @DisplayName("each row in Housekeep DB Settings runs as an independent task")
    void eachTaskRunsIndependently() throws Exception {
      execute("create table mt_a (num1 integer primary key)");
      execute("create table mt_b (num1 integer primary key, rem_flg boolean default false)");
      execute("insert into mt_a values (1)");
      execute("insert into mt_b values (1, false)");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.of(
              new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "mt_a", "num1",
                  "(none)", null, null, null, null, null, null, null, null},
              new String[] {"task-2", "conn1", "Soft Delete", "SOFT_DELETE", "mt_b", "num1",
                  "(none)", null, null, null, "rem_flg", null, null, null, null}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from mt_a")).isZero();
      assertThat(countRows("select count(*) from mt_b where rem_flg = true")).isEqualTo(1);
    }
  }

  // -------------------------------------------------------------------------
  // empty settings
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("empty settings")
  class EmptySettings {

    @Test
    @DisplayName("with no rows in Housekeep DB Settings, finishes without error")
    void finishesWithoutErrorWhenNoTasksConfigured() throws Exception {
      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")), List.of(),
          List.of(), List.of());

      runTasklet(excel);
    }
  }
}
