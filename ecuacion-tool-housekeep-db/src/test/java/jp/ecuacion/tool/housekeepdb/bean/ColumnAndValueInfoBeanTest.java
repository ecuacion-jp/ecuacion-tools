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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ColumnAndValueInfoBean}. */
@DisplayName("ColumnAndValueInfoBean")
class ColumnAndValueInfoBeanTest {

  private static final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  // -------------------------------------------------------------------------
  // constructors
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("constructors")
  class Constructors {

    @Test
    @DisplayName("String value is stored as-is")
    void stringValue() {
      ColumnAndValueInfoBean bean = new ColumnAndValueInfoBean("col1", true, "abc");

      assertThat(bean.getValue()).isEqualTo("abc");
    }

    @Test
    @DisplayName("non-String value is converted with Object#toString")
    void nonStringValue() {
      ColumnAndValueInfoBean bean = new ColumnAndValueInfoBean("col1", false, 123);

      assertThat(bean.getValue()).isEqualTo("123");
    }

    @Test
    @DisplayName("the (String needsQuotationMark) overload parses the excel literal-symbol string")
    void excelStringOverload() {
      ColumnAndValueInfoBean bean = new ColumnAndValueInfoBean("col1", "quotes(')", "abc");

      assertThat(bean.isNeedsQuotationMark()).isTrue();
    }
  }

  // -------------------------------------------------------------------------
  // surroundWithQuotationMarks / getCondition
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("surroundWithQuotationMarks / getCondition")
  class SurroundWithQuotationMarks {

    @Test
    @DisplayName("needsQuotationMark=false returns the raw value")
    void noQuotationMark() {
      ColumnAndValueInfoBean bean = new ColumnAndValueInfoBean("num1", false, 123);

      assertThat(bean.surroundWithQuotationMarks()).isEqualTo("123");
      assertThat(bean.getCondition()).isEqualTo("num1 = 123");
    }

    @Test
    @DisplayName("needsQuotationMark=true wraps the value in single quotes")
    void withQuotationMark() {
      ColumnAndValueInfoBean bean = new ColumnAndValueInfoBean("char1", true, "abc");

      assertThat(bean.surroundWithQuotationMarks()).isEqualTo("'abc'");
      assertThat(bean.getCondition()).isEqualTo("char1 = 'abc'");
    }

    @Test
    @DisplayName("an embedded single quote is escaped by doubling it (SQL injection guard)")
    void embeddedQuoteIsEscaped() {
      ColumnAndValueInfoBean bean = new ColumnAndValueInfoBean("char1", true, "o'brien");

      assertThat(bean.surroundWithQuotationMarks()).isEqualTo("'o''brien'");
      assertThat(bean.getCondition()).isEqualTo("char1 = 'o''brien'");
    }
  }

  // -------------------------------------------------------------------------
  // bean validation
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("bean validation")
  class BeanValidation {

    @Test
    @DisplayName("non-empty value passes validation")
    void nonEmptyPasses() {
      assertThat(validator.validate(new ColumnAndValueInfoBean("col1", false, "abc"))).isEmpty();
    }

    @Test
    @DisplayName("empty string value fails @NotEmpty")
    void emptyValueFails() {
      Set<ConstraintViolation<ColumnAndValueInfoBean>> result =
          validator.validate(new ColumnAndValueInfoBean("col1", false, ""));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getPropertyPath().toString()).isEqualTo("value");
    }
  }
}
