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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import jp.ecuacion.lib.core.exception.ViolationException;
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

/**
 * Tests for {@link HousekeepConfigLoader}.
 *
 * <p>Builds its own minimal excel fixtures rather than reusing
 *     {@code AbstractHousekeepDbTaskletTest}'s helper - that class lives in a different package
 *     (and is focused on end-to-end tasklet behavior against a real DB), whereas the tests here
 *     exercise {@link HousekeepConfigLoader#load} directly, with no DB connection ever opened.</p>
 */
@DisplayName("HousekeepConfigLoader")
class HousekeepConfigLoaderTest {

  private Path buildExcelFile(List<String[]> dbConnectionRows, List<String[]> housekeepRows,
      List<String[]> relatedRows, List<String[]> searchRows) throws IOException {
    LangExcelUtil lang = new LangExcelUtil(Locale.of("en"));

    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      writeSheet(wb, "Info", new String[] {"item", "value"},
          List.<String[]>of(new String[] {"locale", "en"}, new String[] {"format-version", "1.3.0"},
              new String[] {"database", "postgresql"}));
      writeSheet(wb, lang.get(LangExcelUtil.DB_CONNECTION_SETTINGS),
          lang.getHeaderLabels(DbConnectionInfoBean.HEADER_LABEL_KEYS), dbConnectionRows);
      writeSheet(wb, lang.get(LangExcelUtil.HOUSEKEEP_DB_SETTINGS),
          lang.getHeaderLabels(HousekeepInfoBean.HEADER_LABEL_KEYS), housekeepRows);
      writeSheet(wb, lang.get(LangExcelUtil.RELATED_TABLE_SETTINGS),
          lang.getHeaderLabels(RelatedTableInfoBean.HEADER_LABEL_KEYS), relatedRows);
      writeSheet(wb, lang.get(LangExcelUtil.SEARCH_CONDITION_SETTINGS),
          lang.getHeaderLabels(WhereConditionInfoBean.HEADER_LABEL_KEYS), searchRows);

      Path path = Files.createTempFile("housekeep-config-loader-test-", ".xlsx");
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

  private static String[] dbConnectionRow(String id) {
    return new String[] {id, "org.postgresql.Driver", "postgresql", "localhost", "5432",
        "postgres", "", "postgres", "postgres"};
  }

  // -------------------------------------------------------------------------
  // getInfoMap()
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getInfoMap()")
  class GetInfoMap {

    @Test
    @DisplayName("load() populates the info map from the \"Info\" sheet")
    void loadPopulatesInfoMap() throws Exception {
      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")), List.of(),
          List.of(), List.of());

      HousekeepConfigLoader loader = new HousekeepConfigLoader();
      loader.load(excel.toString());

      assertThat(loader.getInfoMap()).containsEntry("locale", "en")
          .containsEntry("format-version", "1.3.0").containsEntry("database", "postgresql");
    }
  }

  // -------------------------------------------------------------------------
  // getHousekeepInfoList() - linking related-table / where-condition rows by task ID
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getHousekeepInfoList()")
  class GetHousekeepInfoList {

    @Test
    @DisplayName("each task is linked only to its own related-table and where-condition rows, "
        + "not another task's")
    void linksOnlyOwnRelatedAndWhereConditionRows() throws Exception {
      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.of(
              new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "tbl_a", "id1",
                  "(none)", null, null, null, null, null, null, null, null},
              new String[] {"task-2", "conn1", "Hard Delete", "HARD_DELETE", "tbl_b", "id1",
                  "(none)", null, null, null, null, null, null, null, null}),
          List.of(
              new String[] {"task-1", "Delete", "DELETE", "child_code", "rel_a", "code",
                  "quotes(')", null, null, null, null, null},
              new String[] {"task-2", "Delete", "DELETE", "child_code", "rel_b", "code",
                  "quotes(')", null, null, null, null, null}),
          List.of(new String[] {"task-1", "status", "quotes(')", "COMPLETED"},
              new String[] {"task-2", "status", "quotes(')", "RUNNING"}));

      HousekeepConfigLoader loader = new HousekeepConfigLoader();
      loader.load(excel.toString());

      List<HousekeepInfoBean> list = loader.getHousekeepInfoList();
      HousekeepInfoBean task1 =
          list.stream().filter(b -> b.getTaskId().equals("task-1")).findFirst().orElseThrow();
      HousekeepInfoBean task2 =
          list.stream().filter(b -> b.getTaskId().equals("task-2")).findFirst().orElseThrow();

      assertThat(task1.getRelatedRecordTableInfoList()).hasSize(1)
          .allSatisfy(rel -> assertThat(rel.getRelatedTable()).isEqualTo("rel_a"));
      assertThat(task1.getWhereConditionInfoList()).hasSize(1)
          .allSatisfy(cond -> assertThat(cond.getConditionColumnValue()).isEqualTo("COMPLETED"));

      assertThat(task2.getRelatedRecordTableInfoList()).hasSize(1)
          .allSatisfy(rel -> assertThat(rel.getRelatedTable()).isEqualTo("rel_b"));
      assertThat(task2.getWhereConditionInfoList()).hasSize(1)
          .allSatisfy(cond -> assertThat(cond.getConditionColumnValue()).isEqualTo("RUNNING"));
    }
  }

  // -------------------------------------------------------------------------
  // invalid relatedTableProcessPatternInternalValue
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("invalid relatedTableProcessPatternInternalValue")
  class InvalidRelatedTableProcessPatternInternalValue {

    @Test
    @DisplayName("a value that is neither DELETE nor CHECK_AND_SKIP_DELETE fails validation "
        + "on load()")
    void neitherDeleteNorCheckAndSkipDeleteFailsValidation() throws Exception {
      Path excel = buildExcelFile(List.<String[]>of(dbConnectionRow("conn1")),
          List.<String[]>of(new String[] {"task-1", "conn1", "Hard Delete", "HARD_DELETE", "tbl_a",
              "id1", "(none)", null, null, null, null, null, null, null, null}),
          List.<String[]>of(new String[] {"task-1", "Delete", "NOT_A_VALID_PATTERN", "child_code",
              "rel_a", "code", "quotes(')", null, null, null, null, null}),
          List.of());

      HousekeepConfigLoader loader = new HousekeepConfigLoader();

      assertThatExceptionOfType(ViolationException.class).isThrownBy(() -> loader.load(excel.toString()));
    }
  }
}
