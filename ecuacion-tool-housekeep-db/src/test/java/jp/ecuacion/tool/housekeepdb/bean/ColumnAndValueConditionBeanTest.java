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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ColumnAndValueConditionBean}. */
@DisplayName("ColumnAndValueConditionBean")
class ColumnAndValueConditionBeanTest {

  // -------------------------------------------------------------------------
  // constructor
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("an unrecognized operator string throws RuntimeException")
    void unrecognizedOperatorThrows() {
      assertThatThrownBy(() -> new ColumnAndValueConditionBean("col1", "=", "(none)", "123"))
          .isInstanceOf(RuntimeException.class);
    }
  }

  // -------------------------------------------------------------------------
  // getSqlFragment / getBindValue
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getSqlFragment / getBindValue")
  class GetSqlFragment {

    @Test
    @DisplayName("\"equal\" with a value renders 'column = value'")
    void equalWithValue() {
      assertThat(new ColumnAndValueConditionBean("num1", "equal", "(none)", "123")
          .getSqlFragment()).isEqualTo("num1 = 123");
      assertThat(new ColumnAndValueConditionBean("char1", "equal", "quotes(')", "abc")
          .getSqlFragment()).isEqualTo("char1 = 'abc'");
    }

    @Test
    @DisplayName("\"not equal\" with a value also matches null, since SQL's NULL <> value is not "
        + "true")
    void notEqualWithValue() {
      assertThat(new ColumnAndValueConditionBean("num1", "not equal", "(none)", "123")
          .getSqlFragment()).isEqualTo("(num1 <> 123 or num1 is null)");
      assertThat(new ColumnAndValueConditionBean("char1", "not equal", "quotes(')", "abc")
          .getSqlFragment()).isEqualTo("(char1 <> 'abc' or char1 is null)");
    }

    @Test
    @DisplayName("\"not equal\" escapes an embedded single quote the same way \"equal\" does")
    void notEqualEscapesQuote() {
      assertThat(new ColumnAndValueConditionBean("char1", "not equal", "quotes(')", "o'brien")
          .getSqlFragment()).isEqualTo("(char1 <> 'o''brien' or char1 is null)");
    }

    @Test
    @DisplayName("\"equal\" with (null) renders 'column is null'")
    void equalWithNull() {
      assertThat(new ColumnAndValueConditionBean("col1", "equal", "(none)", "(null)")
          .getSqlFragment()).isEqualTo("col1 is null");
    }

    @Test
    @DisplayName("\"not equal\" with (null) renders 'column is not null'")
    void notEqualWithNull() {
      assertThat(new ColumnAndValueConditionBean("col1", "not equal", "(none)", "(null)")
          .getSqlFragment()).isEqualTo("col1 is not null");
    }

    @Test
    @DisplayName("(null) means null even when the value is set to be quoted")
    void nullIgnoresQuotationMark() {
      assertThat(new ColumnAndValueConditionBean("col1", "equal", "quotes(')", "(null)")
          .getSqlFragment()).isEqualTo("col1 is null");
    }

    @Test
    @DisplayName("getBindValue() is always null - every fragment is literal")
    void bindValueIsAlwaysNull() {
      assertThat(new ColumnAndValueConditionBean("col1", "not equal", "(none)", "123")
          .getBindValue()).isNull();
    }
  }
}
