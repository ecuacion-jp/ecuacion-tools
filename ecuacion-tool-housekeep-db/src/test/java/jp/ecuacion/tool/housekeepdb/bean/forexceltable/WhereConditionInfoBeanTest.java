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
package jp.ecuacion.tool.housekeepdb.bean.forexceltable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WhereConditionInfoBean}. */
@DisplayName("WhereConditionInfoBean")
class WhereConditionInfoBeanTest {

  private static final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  // Column order: taskId, conditionColumn, conditionColumnNeedsQuotationMark,
  // conditionColumnValue
  private static WhereConditionInfoBean bean(String taskId, String column, String literalSymbol,
      String value) {
    return new WhereConditionInfoBean(Arrays.asList(taskId, column, literalSymbol, value));
  }

  // -------------------------------------------------------------------------
  // conditionColumnInfo is built in afterReading(), like the other beans' derived fields
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("conditionColumnInfo (built in afterReading(), after bean validation passes)")
  class ConditionColumnInfo {

    @Test
    @DisplayName("\"quotes(')\" produces a quoted condition")
    void quotesProducesQuotedCondition() {
      WhereConditionInfoBean b = bean("task1", "col1", "quotes(')", "abc");
      b.afterReading();

      assertThat(b.getConditionColumnInfo().getCondition()).isEqualTo("col1 = 'abc'");
    }

    @Test
    @DisplayName("'(none)' produces an unquoted condition")
    void noneProducesUnquotedCondition() {
      WhereConditionInfoBean b = bean("task1", "col1", "(none)", "123");
      b.afterReading();

      assertThat(b.getConditionColumnInfo().getCondition()).isEqualTo("col1 = 123");
    }

    @Test
    @DisplayName("an unrecognized literal-symbol string throws RuntimeException in afterReading()")
    void unrecognizedLiteralSymbolThrows() {
      WhereConditionInfoBean b = bean("task1", "col1", "unexpected", "abc");

      assertThatThrownBy(b::afterReading).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("a null conditionColumnValue throws NullPointerException in afterReading() "
        + "-- afterReading() assumes bean validation (@NotEmpty) already ran and passed; the "
        + "real read pipeline (StringHeaderExcelTableToBeanReader) guarantees that ordering, "
        + "so this only bites callers who invoke afterReading() directly on an unvalidated bean")
    void nullValueThrowsNpe() {
      List<String> list = Arrays.asList("task1", "col1", "(none)", null);
      WhereConditionInfoBean b = new WhereConditionInfoBean(list);

      assertThatThrownBy(b::afterReading).isInstanceOf(NullPointerException.class);
    }
  }

  // -------------------------------------------------------------------------
  // bean validation (@NotEmpty)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("bean validation (@NotEmpty)")
  class BeanValidation {

    @Test
    @DisplayName("a fully-populated bean passes validation")
    void validBeanPasses() {
      assertThat(validator.validate(bean("task1", "col1", "(none)", "123"))).isEmpty();
    }

    @Test
    @DisplayName("empty taskId fails @NotEmpty on taskId alone")
    void emptyTaskIdFails() {
      Set<ConstraintViolation<WhereConditionInfoBean>> result =
          validator.validate(bean("", "col1", "(none)", "123"));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getPropertyPath().toString()).isEqualTo("taskId");
    }

    @Test
    @DisplayName("empty conditionColumn fails @NotEmpty on conditionColumn alone "
        + "(conditionColumnInfo is still null at validation time, so there is no cascade)")
    void emptyConditionColumnFails() {
      Set<ConstraintViolation<WhereConditionInfoBean>> result =
          validator.validate(bean("task1", "", "(none)", "123"));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getPropertyPath().toString())
          .isEqualTo("conditionColumn");
    }

    @Test
    @DisplayName("empty conditionColumnValue fails @NotEmpty on conditionColumnValue alone, "
        + "with a clean validation error instead of the NPE afterReading() would raise")
    void emptyConditionColumnValueFails() {
      Set<ConstraintViolation<WhereConditionInfoBean>> result =
          validator.validate(bean("task1", "col1", "(none)", ""));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getPropertyPath().toString())
          .isEqualTo("conditionColumnValue");
    }
  }
}
