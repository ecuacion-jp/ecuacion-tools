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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.WhereConditionInfoBean;
import jp.ecuacion.tool.housekeepdb.util.LangExcelUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

  /**
   * Column type for a timestamp column carrying no time-zone offset, matching the
   * "LocalDateTime" value of the Excel "Expiration Check: Timestamp Column Data Type" column.
   */
  protected abstract String localTimestampColumnType();

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

  protected static void runTasklet(Path excelFile) throws Exception {
    runTasklet(excelFile, 1000);
  }

  @SuppressWarnings("null")
  protected static void runTasklet(Path excelFile, int maxSelectLines) throws Exception {
    RepeatStatus status = new HousekeepDbTasklet(excelFile.toString(), maxSelectLines)
        .execute(mock(StepContribution.class), mock(ChunkContext.class));

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
  }

  protected Path buildExcelFile(List<String[]> dbConnectionRows, List<String[]> housekeepRows,
      List<String[]> relatedRows, List<String[]> searchRows) throws IOException {
    return buildExcelFile(Locale.of("en"), dbConnectionRows, housekeepRows, relatedRows,
        searchRows);
  }

  /**
   * Writes a settings excel file whose sheet names and header labels are localized to
   * {@code locale}, the same way {@link HousekeepDbTasklet} localizes them when reading it back
   * from the "locale" row of the "Info" sheet.
   */
  protected Path buildExcelFile(Locale locale, List<String[]> dbConnectionRows,
      List<String[]> housekeepRows, List<String[]> relatedRows, List<String[]> searchRows)
      throws IOException {
    LangExcelUtil lang = new LangExcelUtil(locale);

    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      writeSheet(wb, "Info", new String[] {"item", "value"},
          List.<String[]>of(new String[] {"locale", locale.getLanguage()},
              new String[] {"format-version", "1.3.0"},
              new String[] {"database", protocol()}));
      writeSheet(wb, lang.get(LangExcelUtil.DB_CONNECTION_SETTINGS),
          lang.getHeaderLabels(DbConnectionInfoBean.HEADER_LABEL_KEYS), dbConnectionRows);
      writeSheet(wb, lang.get(LangExcelUtil.HOUSEKEEP_DB_SETTINGS),
          lang.getHeaderLabels(HousekeepInfoBean.HEADER_LABEL_KEYS), housekeepRows);
      writeSheet(wb, lang.get(LangExcelUtil.RELATED_TABLE_SETTINGS),
          lang.getHeaderLabels(RelatedTableInfoBean.HEADER_LABEL_KEYS), relatedRows);
      writeSheet(wb, lang.get(LangExcelUtil.SEARCH_CONDITION_SETTINGS),
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

    @Test
    @DisplayName("works the same against an offset-less timestamp column (\"LocalDateTime\"), "
        + "which the generated SQL handles without consulting the configured column data type")
    void onlyDeletesExpiredRowsWithLocalDateTimeColumn() throws Exception {
      execute("create table exp_local (num1 integer primary key, last_updated "
          + localTimestampColumnType() + ")");
      execute("insert into exp_local values (1, " + timestampDaysAgoExpr(100) + ")");
      execute("insert into exp_local values (2, " + timestampDaysAgoExpr(1) + ")");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "exp_local", "num1", "(none)", "last_updated", "LocalDateTime", "30", null, null,
              null, null, null}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from exp_local where num1 = 1")).isZero();
      assertThat(countRows("select count(*) from exp_local where num1 = 2")).isEqualTo(1);
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
          List.<String[]>of(new String[] {"task-1", "Delete", "DELETE", "child_code",
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
          List.<String[]>of(new String[] {"task-1", "Check and Skip Delete",
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
          List.<String[]>of(new String[] {"task-1", "Check and Skip Delete",
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

    @Test
    @DisplayName("several rows for one task are combined with AND, so only rows matching every "
        + "condition are deleted")
    void combinesSeveralConditionsWithAnd() throws Exception {
      execute("create table sc_multi (num1 integer primary key, status varchar(20), "
          + "category varchar(20))");
      execute("insert into sc_multi values (1, 'COMPLETED', 'A'), (2, 'COMPLETED', 'B'), "
          + "(3, 'RUNNING', 'A')");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "sc_multi", "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.of(),
          List.of(new String[] {"task-1", "status", "quotes(')", "COMPLETED"},
              new String[] {"task-1", "category", "quotes(')", "A"}));

      runTasklet(excel);

      assertThat(countRows("select count(*) from sc_multi where num1 = 1")).isZero();
      assertThat(countRows("select count(*) from sc_multi where num1 = 2")).isEqualTo(1);
      assertThat(countRows("select count(*) from sc_multi where num1 = 3")).isEqualTo(1);
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

  // -------------------------------------------------------------------------
  // paging (maxSelectLines)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("paging (maxSelectLines)")
  class Paging {

    @Test
    @DisplayName("processes every row across multiple select-and-commit iterations "
        + "when there are more rows than maxSelectLines")
    void processesAllRowsAcrossMultipleBatches() throws Exception {
      execute("create table pg_basic (num1 integer primary key)");
      for (int i = 1; i <= 5; i++) {
        execute("insert into pg_basic values (" + i + ")");
      }

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "pg_basic", "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.of(), List.of());

      // maxSelectLines=2 forces the while(true) loop to iterate 3 times for 5 rows.
      runTasklet(excel, 2);

      assertThat(countRows("select count(*) from pg_basic")).isZero();
    }

    @Test
    @DisplayName("reaches records located after a full page of skip targets: skipped records are "
        + "not deleted, so paging must advance past them instead of re-selecting them forever")
    void reachesRecordsAfterAfullPageOfSkipTargets() throws Exception {
      execute("create table pg_skip_parent (num1 integer primary key, child_code varchar(20))");
      execute("create table pg_skip_child (code varchar(20) primary key)");
      execute("insert into pg_skip_parent values (1, 'c1'), (2, 'c2'), (3, 'c3')");
      // num1 = 1 and 2 are skip targets; num1 = 3 has no related record and is deletable.
      execute("insert into pg_skip_child values ('c1'), ('c2')");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "pg_skip_parent", "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "Check and Skip Delete",
              "CHECK_AND_SKIP_DELETE", "child_code", "pg_skip_child", "code", "quotes(')", null,
              null, null, null, null}),
          List.of());

      // maxSelectLines=2 makes the first batch consist solely of the two skip targets.
      runTasklet(excel, 2);

      assertThat(countRows("select count(*) from pg_skip_parent where num1 = 3")).isZero();
      assertThat(countRows("select count(*) from pg_skip_parent")).isEqualTo(2);
    }

    @Test
    @DisplayName("soft delete also processes every row across multiple batches")
    void softDeleteProcessesAllRowsAcrossMultipleBatches() throws Exception {
      execute("create table pg_soft (num1 integer primary key, rem_flg boolean default false)");
      for (int i = 1; i <= 5; i++) {
        execute("insert into pg_soft values (" + i + ", false)");
      }

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Soft Delete", "SOFT_DELETE",
              "pg_soft", "num1", "(none)", null, null, null, "rem_flg", null, null, null, null}),
          List.of(), List.of());

      runTasklet(excel, 2);

      assertThat(countRows("select count(*) from pg_soft where rem_flg = true")).isEqualTo(5);
    }
  }

  // -------------------------------------------------------------------------
  // configuration validation errors (raised while building the housekeep task list,
  // before any DB connection for the target tables is opened)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("configuration validation errors")
  class ConfigurationValidationErrors {

    @Test
    @DisplayName("duplicate task IDs in Housekeep DB Settings raise MSG_ERR_TASK_ID_DUPLICATED")
    void duplicateTaskIdFails() throws Exception {
      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.of(
              new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "cv_dup", "num1",
                  "(none)", null, null, null, null, null, null, null, null},
              new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "cv_dup", "num1",
                  "(none)", null, null, null, null, null, null, null, null}),
          List.of(), List.of());

      assertThatExceptionOfType(ViolationException.class).isThrownBy(() -> runTasklet(excel))
          .satisfies(ex -> assertThat(ex.getViolations().getBusinessViolations())
              .extracting(BusinessViolation::getMessageId)
              .containsExactly("MSG_ERR_TASK_ID_DUPLICATED"));
    }

    @Test
    @DisplayName("an unknown dbConnectionInfoId raises MSG_ERR_DB_CONN_ID_NOT_FOUND")
    void unknownDbConnectionIdFails() throws Exception {
      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "no-such-conn", "Hard Delete", "HARD_DELETE",
              "cv_unknown_conn", "num1", "(none)", null, null, null, null, null, null, null,
              null}),
          List.of(), List.of());

      assertThatExceptionOfType(ViolationException.class).isThrownBy(() -> runTasklet(excel))
          .satisfies(ex -> assertThat(ex.getViolations().getBusinessViolations())
              .extracting(BusinessViolation::getMessageId)
              .containsExactly("MSG_ERR_DB_CONN_ID_NOT_FOUND"));
    }

    @Test
    @DisplayName("a Related Table Settings row whose taskId matches no task "
        + "raises MSG_ERR_DATA_NOT_USED_REL")
    void orphanRelatedTableRowFails() throws Exception {
      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "cv_orphan_rel", "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.<String[]>of(new String[] {"task-unknown", "Delete", "DELETE",
              "child_code", "rt_orphan", "code", "quotes(')", null, null, null, null, null}),
          List.of());

      assertThatExceptionOfType(ViolationException.class).isThrownBy(() -> runTasklet(excel))
          .satisfies(ex -> assertThat(ex.getViolations().getBusinessViolations())
              .extracting(BusinessViolation::getMessageId)
              .containsExactly("MSG_ERR_DATA_NOT_USED_REL"));
    }

    @Test
    @DisplayName("a Search Condition Settings row whose taskId matches no task "
        + "raises MSG_ERR_DATA_NOT_USED_COND")
    void orphanSearchConditionRowFails() throws Exception {
      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "cv_orphan_cond", "num1", "(none)", null, null, null, null, null, null, null,
              null}),
          List.of(),
          List.<String[]>of(new String[] {"task-unknown", "status", "quotes(')", "COMPLETED"}));

      assertThatExceptionOfType(ViolationException.class).isThrownBy(() -> runTasklet(excel))
          .satisfies(ex -> assertThat(ex.getViolations().getBusinessViolations())
              .extracting(BusinessViolation::getMessageId)
              .containsExactly("MSG_ERR_DATA_NOT_USED_COND"));
    }

    @Test
    @DisplayName("an invalid DB Connection Settings row (missing required field) "
        + "fails bean validation")
    void invalidDbConnectionRowFails() throws Exception {
      String[] row = dbConnectionRow("conn1");
      row[1] = "";

      Path excel = buildExcelFile(List.<String[]>of(row), List.of(), List.of(), List.of());

      assertThatThrownBy(() -> runTasklet(excel)).isInstanceOf(ViolationException.class);
    }
  }

  // -------------------------------------------------------------------------
  // excel path validation
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("excel path validation")
  class ExcelPathValidation {

    @SuppressWarnings("null")
    @Test
    @DisplayName("a null excelPath fails @NotEmpty validation")
    void nullExcelPathFails() {
      assertThatThrownBy(() -> new HousekeepDbTasklet(null, 1000).execute(
          mock(StepContribution.class), mock(ChunkContext.class)))
              .isInstanceOf(ViolationException.class);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("a path pointing to a non-existent file fails @FileExists validation")
    void nonExistentFileFails() {
      assertThatThrownBy(() -> new HousekeepDbTasklet("/no/such/file.xlsx", 1000)
          .execute(mock(StepContribution.class), mock(ChunkContext.class)))
              .isInstanceOf(ViolationException.class);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("a non-.xlsx extension fails @FileExtension validation")
    void wrongExtensionFails(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("settings.txt");
      Files.writeString(file, "not an excel file");

      assertThatThrownBy(() -> new HousekeepDbTasklet(file.toString(), 1000)
          .execute(mock(StepContribution.class), mock(ChunkContext.class)))
              .isInstanceOf(ViolationException.class);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("a .xlsx file that isn't a real workbook raises MSG_ERR_EXCEL_PATH_CANNOT_OPEN")
    void unopenableFileFails(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("corrupt.xlsx");
      Files.writeString(file, "not actually an xlsx file");

      assertThatExceptionOfType(ViolationException.class)
          .isThrownBy(() -> new HousekeepDbTasklet(file.toString(), 1000)
              .execute(mock(StepContribution.class), mock(ChunkContext.class)))
          .satisfies(ex -> assertThat(ex.getViolations().getBusinessViolations())
              .extracting(BusinessViolation::getMessageId)
              .containsExactly("MSG_ERR_EXCEL_PATH_CANNOT_OPEN"));
    }
  }

  // -------------------------------------------------------------------------
  // related table settings - soft delete
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("related table settings - soft delete")
  class RelatedTableSoftDelete {

    @Test
    @DisplayName("'Delete' pattern soft-deletes the related-table row (sets the flag) "
        + "when the parent task is a soft delete")
    void deletePatternSoftDeletesRelatedRow() throws Exception {
      execute("create table rts_parent (num1 integer primary key, rem_flg boolean default "
          + "false, child_code varchar(20))");
      execute(
          "create table rts_child (code varchar(20) primary key, rem_flg boolean default false)");
      execute("insert into rts_parent values (1, false, 'c1')");
      execute("insert into rts_child values ('c1', false)");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Soft Delete", "SOFT_DELETE",
              "rts_parent", "num1", "(none)", null, null, null, "rem_flg", null, null, null,
              null}),
          List.<String[]>of(new String[] {"task-1", "Delete", "DELETE",
              "child_code", "rts_child", "code", "quotes(')", "rem_flg", null, null, null, null}),
          List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from rts_parent where rem_flg = true")).isEqualTo(1);
      assertThat(countRows("select count(*) from rts_child where rem_flg = true")).isEqualTo(1);
    }

    @Test
    @DisplayName("also updates the related row's configured timestamp and user-id columns")
    void updatesTimestampAndUserIdColumnsOnRelatedRow() throws Exception {
      execute("create table rts_parent2 (num1 integer primary key, rem_flg boolean default "
          + "false, child_code varchar(20))");
      execute("create table rts_child2 (code varchar(20) primary key, rem_flg boolean default "
          + "false, upd_at " + timestampColumnType() + ", upd_by varchar(20))");
      execute("insert into rts_parent2 values (1, false, 'c1')");
      execute("insert into rts_child2 values ('c1', false, null, null)");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Soft Delete", "SOFT_DELETE",
              "rts_parent2", "num1", "(none)", null, null, null, "rem_flg", null, null, null,
              null}),
          List.<String[]>of(new String[] {"task-1", "Delete", "DELETE",
              "child_code", "rts_child2", "code", "quotes(')", "rem_flg", "upd_at", "upd_by",
              "quotes(')", "SYSTEM"}),
          List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from rts_child2 where rem_flg = true "
          + "and upd_at is not null and upd_by = 'SYSTEM'")).isEqualTo(1);
    }
  }

  // -------------------------------------------------------------------------
  // related table settings - hard delete + related table's own soft-delete column
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("related table settings - hard delete + related table's soft-delete column")
  class RelatedTableHardDeleteWithSoftDeleteColumn {

    @Test
    @DisplayName("only purges related rows already flagged")
    void purgesFlaggedRelatedRows() throws Exception {
      execute("create table rtf_parent (num1 integer primary key, child_code varchar(20))");
      execute(
          "create table rtf_child (code varchar(20) primary key, rem_flg boolean default false)");
      execute("insert into rtf_parent values (1, 'c1')");
      execute("insert into rtf_child values ('c1', true)");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "rtf_parent", "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "Delete", "DELETE",
              "child_code", "rtf_child", "code", "quotes(')", "rem_flg", null, null, null, null}),
          List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from rtf_child")).isZero();
    }

    @Test
    @DisplayName("skips related rows not yet flagged")
    void skipsUnflaggedRelatedRows() throws Exception {
      execute("create table rtf_parent2 (num1 integer primary key, child_code varchar(20))");
      execute("create table rtf_child2 (code varchar(20) primary key, rem_flg boolean default "
          + "false)");
      execute("insert into rtf_parent2 values (1, 'c1')");
      execute("insert into rtf_child2 values ('c1', false)");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "rtf_parent2", "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "Delete", "DELETE",
              "child_code", "rtf_child2", "code", "quotes(')", "rem_flg", null, null, null, null}),
          List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from rtf_child2")).isEqualTo(1);
    }
  }

  // -------------------------------------------------------------------------
  // related table settings - multiple settings on one task
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("related table settings - multiple settings on one task")
  class MultipleRelatedTableSettings {

    @Test
    @DisplayName("when a 'Check and Skip Delete' related row exists, neither the target row "
        + "nor a co-configured 'Delete' pattern related row is touched")
    void skipBlocksBothTargetAndCoConfiguredDeletePattern() throws Exception {
      execute("create table mrt_parent (num1 integer primary key, child_code varchar(20), "
          + "other_code varchar(20))");
      execute("create table mrt_child_del (code varchar(20) primary key)");
      execute("create table mrt_child_skip (code varchar(20) primary key)");
      execute("insert into mrt_parent values (1, 'c1', 'o1')");
      execute("insert into mrt_child_del values ('c1')");
      execute("insert into mrt_child_skip values ('o1')");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "mrt_parent", "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.of(
              new String[] {"task-1", "Delete", "DELETE", "child_code",
                  "mrt_child_del", "code", "quotes(')", null, null, null, null, null},
              new String[] {"task-1", "Check and Skip Delete",
                  "CHECK_AND_SKIP_DELETE", "other_code", "mrt_child_skip", "code", "quotes(')",
                  null, null, null, null, null}),
          List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from mrt_parent")).isEqualTo(1);
      assertThat(countRows("select count(*) from mrt_child_del")).isEqualTo(1);
      assertThat(countRows("select count(*) from mrt_child_skip")).isEqualTo(1);
    }
  }

  // -------------------------------------------------------------------------
  // related table settings - target record removed as a side effect
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("related table settings - target record removed as a side effect")
  class TargetRecordRemovedAsSideEffect {

    @Test
    @DisplayName("finishes normally when deleting the related record already removed the target "
        + "record through a cascading foreign key, leaving the target delete affecting no rows")
    void finishesWhenCascadeAlreadyRemovedTheTargetRecord() throws Exception {
      execute("create table cas_child (code varchar(20) primary key)");
      execute("create table cas_parent (num1 integer primary key, child_code varchar(20) "
          + "references cas_child (code) on delete cascade)");
      execute("insert into cas_child values ('c1')");
      execute("insert into cas_parent values (1, 'c1')");

      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE",
              "cas_parent", "num1", "(none)", null, null, null, null, null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "Delete", "DELETE", "child_code", "cas_child",
              "code", "quotes(')", null, null, null, null, null}),
          List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from cas_child")).isZero();
      assertThat(countRows("select count(*) from cas_parent")).isZero();
    }
  }

  // -------------------------------------------------------------------------
  // multiple db connections
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("multiple db connections")
  class MultipleDbConnections {

    @Test
    @DisplayName("each task opens the connection its own DB Connection ID resolves to")
    void eachTaskUsesItsOwnConnection() throws Exception {
      execute("create table mc_a (num1 integer primary key)");
      execute("create table mc_b (num1 integer primary key)");
      execute("insert into mc_a values (1)");
      execute("insert into mc_b values (1)");

      Path excel = buildExcelFile(
          List.of(dbConnectionRow("conn1"), dbConnectionRow("conn2")),
          List.of(
              new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "mc_a", "num1",
                  "(none)", null, null, null, null, null, null, null, null},
              new String[] {"task-2", "conn2", "Hard Delete", "HARD_DELETE", "mc_b", "num1",
                  "(none)", null, null, null, null, null, null, null, null}),
          List.of(), List.of());

      runTasklet(excel);

      assertThat(countRows("select count(*) from mc_a")).isZero();
      assertThat(countRows("select count(*) from mc_b")).isZero();
    }
  }

  // -------------------------------------------------------------------------
  // localized settings file
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("localized settings file")
  class LocalizedSettingsFile {

    @Test
    @DisplayName("a Japanese settings file, whose sheet names and header labels differ entirely "
        + "from the English one, drives the same housekeeping")
    void japaneseSettingsFile() throws Exception {
      execute("create table loc_parent (num1 integer primary key, rem_flg boolean default false, "
          + "child_code varchar(20), status varchar(20))");
      execute("create table loc_child (code varchar(20) primary key, rem_flg boolean "
          + "default false)");
      execute("insert into loc_parent values (1, false, 'c1', 'COMPLETED')");
      execute("insert into loc_parent values (2, false, 'c2', 'RUNNING')");
      execute("insert into loc_child values ('c1', false)");

      // Every sheet is exercised so that a mistranslated label on any of them fails this test.
      Path excel = buildExcelFile(Locale.of("ja"), List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "論理廃止", "SOFT_DELETE", "loc_parent",
              "num1", "(none)", null, null, null, "rem_flg", null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "論理廃止／削除", "DELETE", "child_code",
              "loc_child", "code", "quotes(')", "rem_flg", null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "status", "quotes(')", "COMPLETED"}));

      runTasklet(excel);

      assertThat(countRows("select count(*) from loc_parent where num1 = 1 and rem_flg = true"))
          .isEqualTo(1);
      assertThat(countRows("select count(*) from loc_parent where num1 = 2 and rem_flg = false"))
          .isEqualTo(1);
      assertThat(countRows("select count(*) from loc_child where rem_flg = true")).isEqualTo(1);
    }
  }
}
