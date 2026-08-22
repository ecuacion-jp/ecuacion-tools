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
package jp.ecuacion.tool.housekeepdb.bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ColumnInfoBean}. */
@DisplayName("ColumnInfoBean")
class ColumnInfoBeanTest {

  // -------------------------------------------------------------------------
  // constructors
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("constructor(String, boolean)")
  class BooleanConstructor {

    @Test
    @DisplayName("stores column name and quotation-mark flag as-is")
    void storesValues() {
      ColumnInfoBean bean = new ColumnInfoBean("col1", true);

      assertThat(bean.getColumn()).isEqualTo("col1");
      assertThat(bean.isNeedsQuotationMark()).isTrue();
    }
  }

  @Nested
  @DisplayName("constructor(String, String) - excel literal-symbol string")
  class ExcelStringConstructor {

    @Test
    @DisplayName("'(none)' means no quotation mark")
    void noneMeansFalse() {
      ColumnInfoBean bean = new ColumnInfoBean("col1", "(none)");

      assertThat(bean.isNeedsQuotationMark()).isFalse();
    }

    @Test
    @DisplayName("\"quotes(')\" means quotation mark required")
    void quotesMeansTrue() {
      ColumnInfoBean bean = new ColumnInfoBean("col1", "quotes(')");

      assertThat(bean.isNeedsQuotationMark()).isTrue();
    }

    @Test
    @DisplayName("any other string throws RuntimeException")
    void otherThrows() {
      assertThatThrownBy(() -> new ColumnInfoBean("col1", "unexpected"))
          .isInstanceOf(RuntimeException.class);
    }
  }

  // -------------------------------------------------------------------------
  // setters
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("setters")
  class Setters {

    @Test
    @DisplayName("setColumn / setNeedsQuotationMark update the held state")
    void updateState() {
      ColumnInfoBean bean = new ColumnInfoBean("col1", false);

      bean.setColumn("col2");
      bean.setNeedsQuotationMark(true);

      assertThat(bean.getColumn()).isEqualTo("col2");
      assertThat(bean.isNeedsQuotationMark()).isTrue();
    }
  }

  // -------------------------------------------------------------------------
  // getColumnAndValueInfo
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getColumnAndValueInfo(Object)")
  class GetColumnAndValueInfo {

    @Test
    @DisplayName("builds a ColumnAndValueInfoBean carrying this bean's column/quotation settings")
    void buildsBean() {
      ColumnInfoBean bean = new ColumnInfoBean("id", true);

      ColumnAndValueInfoBean result = bean.getColumnAndValueInfo(123);

      assertThat(result.getColumn()).isEqualTo("id");
      assertThat(result.isNeedsQuotationMark()).isTrue();
      assertThat(result.getValue()).isEqualTo("123");
    }
  }

  // -------------------------------------------------------------------------
  // getTimestampColumnNowInfo
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getTimestampColumnNowInfo(String)")
  class GetTimestampColumnNowInfo {

    @Test
    @DisplayName("builds a quoted ColumnAndValueInfoBean holding the current timestamp")
    void buildsBeanWithNow() {
      ColumnInfoBean bean = new ColumnInfoBean("updated_at", false);

      ColumnAndValueInfoBean result = bean.getTimestampColumnNowInfo("postgresql");

      assertThat(result.getColumn()).isEqualTo("updated_at");
      assertThat(result.isNeedsQuotationMark()).isTrue();
      OffsetDateTime parsed =
          OffsetDateTime.parse((String) result.getValue(), DateTimeFormatter.ISO_DATE_TIME);
      assertThat(parsed).isCloseTo(OffsetDateTime.now(ZoneId.systemDefault()),
          new TemporalUnitWithinOffset(10, ChronoUnit.SECONDS));
    }
  }
}
