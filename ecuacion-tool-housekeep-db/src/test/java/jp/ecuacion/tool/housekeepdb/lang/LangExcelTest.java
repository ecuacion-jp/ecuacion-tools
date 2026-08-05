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
package jp.ecuacion.tool.housekeepdb.lang;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link LangExcel}. */
@DisplayName("LangExcel")
class LangExcelTest {

  // -------------------------------------------------------------------------
  // get(String)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("get(String)")
  class Get {

    @Test
    @DisplayName("English locale resolves EXCEL_SHEET_DB_CONNECTION_SETTINGS to its en message")
    void english() {
      LangExcel lang = new LangExcel(Locale.of("en"));

      assertThat(lang.get(LangExcel.DB_CONNECTION_SETTINGS)).isEqualTo("DB Connection Settings");
    }

    @Test
    @DisplayName("Japanese locale resolves EXCEL_SHEET_DB_CONNECTION_SETTINGS to its ja message")
    void japanese() {
      LangExcel lang = new LangExcel(Locale.of("ja"));

      assertThat(lang.get(LangExcel.DB_CONNECTION_SETTINGS)).isEqualTo("DB接続設定");
    }
  }

  // -------------------------------------------------------------------------
  // getHeaderLabels(String[])
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getHeaderLabels(String[])")
  class GetHeaderLabels {

    @Test
    @DisplayName("resolves each key in order, preserving array length and order")
    void resolvesInOrder() {
      LangExcel lang = new LangExcel(Locale.of("en"));

      String[] result = lang.getHeaderLabels(LangExcel.SearchConditionSettings.HEADER_LABELS);

      assertThat(result).containsExactly("Task ID", "Search Condition Column Name",
          "Search Condtion Column Literal Symbol", "Search Condition Column Value");
    }

    @Test
    @DisplayName("Japanese locale resolves the same keys to Japanese labels")
    void resolvesInOrderJapanese() {
      LangExcel lang = new LangExcel(Locale.of("ja"));

      String[] result = lang.getHeaderLabels(LangExcel.SearchConditionSettings.HEADER_LABELS);

      assertThat(result).containsExactly("処理ID", "条件カラム名", "条件カラム型リテラル記号", "条件カラム値");
    }
  }
}
