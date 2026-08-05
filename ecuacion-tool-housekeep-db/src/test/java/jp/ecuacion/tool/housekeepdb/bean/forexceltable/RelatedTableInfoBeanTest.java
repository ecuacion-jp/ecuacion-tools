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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean.RelatedTableProcessPatternEnum;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link RelatedTableInfoBean}. */
@DisplayName("RelatedTableInfoBean")
class RelatedTableInfoBeanTest {

  private static final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  // Column order: taskId, isSoftDeleteInternalValue, relatedTableProcessPattern,
  // relatedTableProcessPatternInternalValue, targetTableColumn, relatedTable,
  // relatedTableIdColumn, relatedTableIdColumnNeedsQuotationMark, softDeleteColumn,
  // softDeleteUpdateTimestampColumn, softDeleteUpdateUserIdColumn,
  // softDeleteUpdateUserIdColumnNeedsQuotationMark, softDeleteUpdateUserIdColumnValue
  private static final String[] HARD_BASE = {"task1", "HARD_DELETE", "Delete", "DELETE", "col1",
      "reltbl1", "relid1", "(none)", null, null, null, null, null};
  private static final String[] SOFT_BASE = {"task1", "SOFT_DELETE", "Delete", "DELETE", "col1",
      "reltbl1", "relid1", "(none)", "del_flg", null, null, null, null};

  private static RelatedTableInfoBean bean(String[] base, int index, @Nullable String value) {
    String[] copy = Arrays.copyOf(base, base.length);
    copy[index] = value;
    return new RelatedTableInfoBean(Arrays.asList(copy));
  }

  private static RelatedTableInfoBean bean(String[] base) {
    return new RelatedTableInfoBean(Arrays.asList(base));
  }

  // -------------------------------------------------------------------------
  // @NotEmpty required fields
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("required fields (@NotEmpty)")
  class RequiredFields {

    @Test
    @DisplayName("all-null input fails @NotEmpty on exactly the 8 required columns")
    void allNullFailsOnRequiredColumnsOnly() {
      List<String> allNull = Arrays.asList(new String[13]);

      Set<ConstraintViolation<RelatedTableInfoBean>> result =
          validator.validate(new RelatedTableInfoBean(allNull));

      assertThat(result).hasSize(8);
      assertThat(result).allSatisfy(cv -> assertThat(
          cv.getConstraintDescriptor().getAnnotation().annotationType().getCanonicalName())
              .isEqualTo("jakarta.validation.constraints.NotEmpty"));
      assertThat(result.stream().map(cv -> cv.getPropertyPath().toString()).toList())
          .containsExactlyInAnyOrder("taskId", "isSoftDeleteInternalValue",
              "relatedTableProcessPattern", "relatedTableProcessPatternInternalValue",
              "targetTableColumn", "relatedTable", "relatedTableIdColumn",
              "relatedTableIdColumnNeedsQuotationMark");
    }
  }

  // -------------------------------------------------------------------------
  // @Pattern fields
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("@Pattern fields")
  class PatternFields {

    @Test
    @DisplayName("a valid combination passes")
    void validPatternValues() {
      assertThat(validator.validate(bean(HARD_BASE))).isEmpty();
    }

    @Test
    @DisplayName("invalid isSoftDeleteInternalValue fails @Pattern")
    void invalidSoftDeleteInternalValue() {
      Set<ConstraintViolation<RelatedTableInfoBean>> result =
          validator.validate(bean(HARD_BASE, 1, "UNEXPECTED"));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getConstraintDescriptor().getAnnotation()
          .annotationType().getCanonicalName()).isEqualTo("jakarta.validation.constraints.Pattern");
    }

    @Test
    @DisplayName("invalid relatedTableIdColumnNeedsQuotationMark fails @Pattern")
    void invalidLiteralSymbol() {
      Set<ConstraintViolation<RelatedTableInfoBean>> result =
          validator.validate(bean(HARD_BASE, 7, "UNEXPECTED"));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getConstraintDescriptor().getAnnotation()
          .annotationType().getCanonicalName()).isEqualTo("jakarta.validation.constraints.Pattern");
    }
  }

  // -------------------------------------------------------------------------
  // @NotEmptyWhen: softDeleteColumn required for soft delete
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("@NotEmptyWhen: softDeleteColumn required for soft delete")
  class SoftDeleteColumnRequired {

    @Test
    @DisplayName("soft delete without softDeleteColumn fails with NotEmptyWhen")
    void softDeleteWithoutColumnFails() {
      Set<ConstraintViolation<RelatedTableInfoBean>> result =
          validator.validate(bean(SOFT_BASE, 8, null));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getConstraintDescriptor().getAnnotation()
          .annotationType().getCanonicalName())
              .isEqualTo("jp.ecuacion.lib.validation.constraints.NotEmptyWhen");
    }

    @Test
    @DisplayName("soft delete with softDeleteColumn set passes")
    void softDeleteWithColumnPasses() {
      assertThat(validator.validate(bean(SOFT_BASE))).isEmpty();
    }

    @Test
    @DisplayName("hard delete without softDeleteColumn passes (condition not satisfied)")
    void hardDeleteWithoutColumnPasses() {
      assertThat(validator.validate(bean(HARD_BASE))).isEmpty();
    }
  }

  // -------------------------------------------------------------------------
  // @EmptyWhen: soft-delete update columns must be empty on hard delete
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("@EmptyWhen: soft-delete update columns must be empty on hard delete")
  class SoftDeleteUpdateColumnsEmptyOnHardDelete {

    @Test
    @DisplayName("hard delete with update-timestamp column set fails")
    void hardDeleteWithUpdateTimestampColumnFails() {
      Set<ConstraintViolation<RelatedTableInfoBean>> result =
          validator.validate(bean(HARD_BASE, 9, "upd_at"));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getConstraintDescriptor().getAnnotation()
          .annotationType().getCanonicalName())
              .isEqualTo("jp.ecuacion.lib.validation.constraints.EmptyWhen");
    }

    @Test
    @DisplayName("hard delete with both update columns empty passes")
    void hardDeleteWithColumnsEmptyPasses() {
      assertThat(validator.validate(bean(HARD_BASE))).isEmpty();
    }

    @Test
    @DisplayName("soft delete with update-timestamp column set passes (condition not satisfied)")
    void softDeleteWithColumnsSetPasses() {
      assertThat(validator.validate(bean(SOFT_BASE, 9, "upd_at"))).isEmpty();
    }
  }

  // -------------------------------------------------------------------------
  // @EmptyWhen: soft-delete update-user-id trio must be all-empty-or-all-set
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("@EmptyWhen: soft-delete update-user-id trio must be all-empty-or-all-set")
  class UpdateUserIdTrioAllOrNothing {

    @Test
    @DisplayName("all three empty passes")
    void allEmptyPasses() {
      assertThat(validator.validate(bean(SOFT_BASE))).isEmpty();
    }

    @Test
    @DisplayName("all three set passes")
    void allSetPasses() {
      List<String> list = Arrays.asList("task1", "SOFT_DELETE", "Delete", "DELETE", "col1",
          "reltbl1", "relid1", "(none)", "del_flg", null, "upd_by", "quotes(')", "SYSTEM");

      assertThat(validator.validate(new RelatedTableInfoBean(list))).isEmpty();
    }

    @Test
    @DisplayName("only softDeleteUpdateUserIdColumn set (symbol/value left empty) fails")
    void onlyUserIdColumnSetFails() {
      Set<ConstraintViolation<RelatedTableInfoBean>> result =
          validator.validate(bean(SOFT_BASE, 10, "upd_by"));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getConstraintDescriptor().getAnnotation()
          .annotationType().getCanonicalName())
              .isEqualTo("jp.ecuacion.lib.validation.constraints.EmptyWhen");
    }
  }

  // -------------------------------------------------------------------------
  // getRelatedTableProcessPattern() / getRelatedTableProcessPatternStringKey()
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getRelatedTableProcessPattern() / getRelatedTableProcessPatternStringKey()")
  class RelatedTableProcessPattern {

    @Test
    @DisplayName("'DELETE' resolves to deleteRelatedTableRecord")
    void deleteResolvesToDeleteRelatedTableRecord() {
      RelatedTableInfoBean b = bean(HARD_BASE, 3, "DELETE");

      assertThat(b.getRelatedTableProcessPattern())
          .isEqualTo(RelatedTableProcessPatternEnum.deleteRelatedTableRecord);
      assertThat(b.getRelatedTableProcessPatternStringKey())
          .isEqualTo("EXCEL_VALUE_RELATED_TABLE_PROCESS_PATTERN_DELETE");
    }

    @Test
    @DisplayName("any non-'DELETE' value resolves to skipTargetTableRecordDeletion")
    void otherResolvesToSkip() {
      RelatedTableInfoBean b = bean(HARD_BASE, 3, "CHECK_AND_SKIP_DELETE");

      assertThat(b.getRelatedTableProcessPattern())
          .isEqualTo(RelatedTableProcessPatternEnum.skipTargetTableRecordDeletion);
      assertThat(b.getRelatedTableProcessPatternStringKey())
          .isEqualTo("EXCEL_VALUE_RELATED_TABLE_PROCESS_PATTERN_CHECK_AND_SKIP_DELETE");
    }
  }

  // -------------------------------------------------------------------------
  // afterReading() / constructColumnInfo()
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("afterReading()")
  class AfterReading {

    @Test
    @DisplayName("relatedTableIdColumnInfo is always constructed")
    void relatedTableIdColumnInfoAlwaysConstructed() {
      RelatedTableInfoBean b = bean(HARD_BASE);
      b.afterReading();

      assertThat(b.getRelatedTableIdColumnInfo().getColumn()).isEqualTo("relid1");
      assertThat(b.getRelatedTableIdColumnInfo().isNeedsQuotationMark()).isFalse();
    }

    @Test
    @DisplayName("softDeleteColumnInfo stays null when softDeleteColumn is empty")
    void softDeleteColumnInfoNullWhenEmpty() {
      RelatedTableInfoBean b = bean(HARD_BASE);
      b.afterReading();

      assertThat(b.getSoftDeleteColumnInfo()).isNull();
    }

    @Test
    @DisplayName("softDeleteColumnInfo is constructed when softDeleteColumn is set")
    void softDeleteColumnInfoConstructedWhenSet() {
      RelatedTableInfoBean b = bean(SOFT_BASE);
      b.afterReading();

      assertThat(b.getSoftDeleteColumnInfo().getColumn()).isEqualTo("del_flg");
    }

    @Test
    @DisplayName("softDeleteUpdateUserIdColumnAndValueInfo is constructed when its column is set")
    void updateUserIdColumnAndValueInfoConstructedWhenSet() {
      List<String> list = Arrays.asList("task1", "SOFT_DELETE", "Delete", "DELETE", "col1",
          "reltbl1", "relid1", "(none)", "del_flg", null, "upd_by", "quotes(')", "SYSTEM");
      RelatedTableInfoBean b = new RelatedTableInfoBean(list);
      b.afterReading();

      assertThat(b.getSoftDeleteUpdateUserIdColumnAndValueInfo().getCondition())
          .isEqualTo("upd_by = 'SYSTEM'");
    }
  }
}
