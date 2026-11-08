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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link UpdateColumnInfoBean}. */
@DisplayName("UpdateColumnInfoBean")
class UpdateColumnInfoBeanTest {

  private static final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  // Column order: taskId, updateColumn, valueType, valueTypeInternalValue, needsQuotationMark,
  // value
  @SuppressWarnings("null")
  private static UpdateColumnInfoBean bean(String column, String valueTypeInternalValue,
      @Nullable String literalSymbol, @Nullable String value) {
    return new UpdateColumnInfoBean(Arrays.asList("task1", column, "(display value)",
        valueTypeInternalValue, literalSymbol, value));
  }

  @SuppressWarnings("null")
  private static Set<ConstraintViolation<UpdateColumnInfoBean>> validate(UpdateColumnInfoBean b) {
    return validator.validate(b);
  }

  // -------------------------------------------------------------------------
  // bean validation
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("bean validation")
  class BeanValidation {

    @Test
    @DisplayName("a fixed value with a literal symbol and a value passes")
    void fixedValuePasses() {
      assertThat(validate(bean("col1", "FIXED_VALUE", "quotes(')", "abc"))).isEmpty();
    }

    @Test
    @DisplayName("the current timestamp / null with an empty literal symbol and value passes")
    void currentTimestampAndNullPass() {
      assertThat(validate(bean("col1", "CURRENT_TIMESTAMP", null, null))).isEmpty();
      assertThat(validate(bean("col1", "NULL", null, null))).isEmpty();
    }

    @Test
    @DisplayName("a fixed value without a literal symbol or a value fails")
    void fixedValueWithoutValueFails() {
      assertThat(validate(bean("col1", "FIXED_VALUE", null, "abc"))).hasSize(1);
      assertThat(validate(bean("col1", "FIXED_VALUE", "quotes(')", null))).hasSize(1);
    }

    @Test
    @DisplayName("the current timestamp / null with a literal symbol or a value fails")
    void currentTimestampAndNullWithValueFail() {
      assertThat(validate(bean("col1", "CURRENT_TIMESTAMP", "(none)", null))).hasSize(1);
      assertThat(validate(bean("col1", "NULL", null, "abc"))).hasSize(1);
    }

    @Test
    @DisplayName("an unrecognized value type fails @Pattern on valueTypeInternalValue")
    void unrecognizedValueTypeFails() {
      Set<ConstraintViolation<UpdateColumnInfoBean>> result =
          validate(bean("col1", "UNEXPECTED", "(none)", "1"));

      assertThat(result).extracting(cv -> cv.getPropertyPath().toString())
          .contains("valueTypeInternalValue");
    }

    @Test
    @DisplayName("a column name that is not an unquoted SQL identifier fails")
    void invalidColumnNameFails() {
      Set<ConstraintViolation<UpdateColumnInfoBean>> result =
          validate(bean("col1; drop table t", "NULL", null, null));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getPropertyPath().toString())
          .isEqualTo("updateColumn");
    }
  }

  // -------------------------------------------------------------------------
  // getSetCondition
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getSetCondition(String)")
  class GetSetCondition {

    @Test
    @DisplayName("a fixed value renders a literal, quoted according to the literal symbol")
    void fixedValue() {
      assertThat(bean("char1", "FIXED_VALUE", "quotes(')", "o'brien")
          .getSetCondition("postgresql").getSqlFragment()).isEqualTo("char1 = 'o''brien'");
      assertThat(bean("num1", "FIXED_VALUE", "(none)", "123").getSetCondition("postgresql")
          .getSqlFragment()).isEqualTo("num1 = 123");
    }

    @Test
    @DisplayName("null renders 'column = null' with nothing to bind")
    void nullValue() {
      SqlConditionInterface cond = bean("col1", "NULL", null, null).getSetCondition("postgresql");

      assertThat(cond.getSqlFragment()).isEqualTo("col1 = null");
      assertThat(cond.getBindValue()).isNull();
    }

    @Test
    @DisplayName("the current timestamp is bound as a JDBC parameter")
    void currentTimestamp() {
      SqlConditionInterface cond =
          bean("upd_at", "CURRENT_TIMESTAMP", null, null).getSetCondition("postgresql");

      assertThat(cond.getSqlFragment()).isEqualTo("upd_at = ?");
      assertThat(cond.getBindValue()).isInstanceOf(OffsetDateTime.class);
    }
  }
}
