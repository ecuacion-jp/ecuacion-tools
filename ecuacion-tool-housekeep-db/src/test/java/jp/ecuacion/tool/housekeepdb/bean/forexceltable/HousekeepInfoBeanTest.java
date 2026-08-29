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
import jp.ecuacion.tool.housekeepdb.enums.TimestampKindEnum;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link HousekeepInfoBean}. */
@DisplayName("HousekeepInfoBean")
class HousekeepInfoBeanTest {

  private static final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  // Column order: taskId, dbConnectionInfoId, isSoftDelete, isSoftDeleteInternalValue, table,
  // idColumn, idColumnNeedsQuotationMark, timestampColumn, timestampColumnKind,
  // deleteTargetInDays, softDeleteColumn, softDeleteUpdateTimestampColumn,
  // softDeleteUpdateUserIdColumn, softDeleteUpdateUserIdColumnNeedsQuotationMark,
  // softDeleteUpdateUserIdColumnValue
  private static final String[] HARD_BASE = {"task1", "conn1", "Hard Delete", "HARD_DELETE",
      "tbl1", "id1", "(none)", null, null, null, null, null, null, null, null};
  private static final String[] SOFT_BASE = {"task1", "conn1", "Soft Delete", "SOFT_DELETE",
      "tbl1", "id1", "(none)", null, null, null, "del_flg", null, null, null, null};

  private static HousekeepInfoBean bean(String[] base, int index, @Nullable String value) {
    String[] copy = Arrays.copyOf(base, base.length);
    copy[index] = value;
    return new HousekeepInfoBean(Arrays.asList(copy));
  }

  private static HousekeepInfoBean bean(String[] base) {
    return new HousekeepInfoBean(Arrays.asList(base));
  }

  // -------------------------------------------------------------------------
  // @NotEmpty required fields
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("required fields (@NotEmpty)")
  class RequiredFields {

    @Test
    @DisplayName("all-null input fails @NotEmpty on exactly the A-G columns")
    void allNullFailsOnRequiredColumnsOnly() {
      List<String> allNull = Arrays.asList(new String[15]);

      @SuppressWarnings("null")
      Set<ConstraintViolation<HousekeepInfoBean>> result =
          validator.validate(new HousekeepInfoBean(allNull));

      assertThat(result).hasSize(7);
      assertThat(result).allSatisfy(cv -> assertThat(
          cv.getConstraintDescriptor().getAnnotation().annotationType().getCanonicalName())
              .isEqualTo("jakarta.validation.constraints.NotEmpty"));
      assertThat(result.stream().map(cv -> cv.getPropertyPath().toString()).toList())
          .containsExactlyInAnyOrder("taskId", "dbConnectionInfoId", "isSoftDelete",
              "isSoftDeleteInternalValue", "table", "idColumn", "idColumnNeedsQuotationMark");
    }
  }

  // -------------------------------------------------------------------------
  // @Pattern fields
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("@Pattern fields")
  class PatternFields {

    @Test
    @DisplayName("isSoftDeleteInternalValue and idColumnNeedsQuotationMark both invalid -> 2 violations")
    void bothInvalidPatterns() {
      // Fill every required column with a non-empty placeholder so only the two @Pattern
      // constraints under test can fail.
      List<String> list = Arrays.asList("taskId", "dbConnectionInfoId", "isSoftDelete",
          "isSoftDeleteInternalValue", "table", "idColumn", "idColumnNeedsQuotationMark", null,
          null, null, null, null, null, null, null);

      @SuppressWarnings("null")
      Set<ConstraintViolation<HousekeepInfoBean>> result =
          validator.validate(new HousekeepInfoBean(list));

      assertThat(result).hasSize(2);
      assertThat(result).allSatisfy(cv -> assertThat(
          cv.getConstraintDescriptor().getAnnotation().annotationType().getCanonicalName())
              .isEqualTo("jakarta.validation.constraints.Pattern"));
    }

    @Test
    @DisplayName("HARD_DELETE / quotes(') is a valid combination")
    void validPatternValues() {
      assertThat(validator.validate(bean(HARD_BASE))).isEmpty();
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
      @SuppressWarnings("null")
      Set<ConstraintViolation<HousekeepInfoBean>> result =
          validator.validate(bean(SOFT_BASE, 10, null));

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
  // @EmptyWhen: expiration-check columns are all-empty-or-all-set together
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("@EmptyWhen: expiration-check columns must be all-empty-or-all-set")
  class ExpirationColumnsAllOrNothing {

    @Test
    @DisplayName("all three empty passes")
    void allEmptyPasses() {
      assertThat(validator.validate(bean(HARD_BASE))).isEmpty();
    }

    @Test
    @DisplayName("all three set passes")
    void allSetPasses() {
      List<String> list = Arrays.asList("task1", "conn1", "Hard Delete", "HARD_DELETE", "tbl1",
          "id1", "(none)", "last_updated", "OffsetDateTime", "30", null, null, null, null, null);

      assertThat(validator.validate(new HousekeepInfoBean(list))).isEmpty();
    }

    @Test
    @DisplayName("only timestampColumn set (kind/days left empty) fails")
    void onlyTimestampColumnSetFails() {
      @SuppressWarnings("null")
      Set<ConstraintViolation<HousekeepInfoBean>> result =
          validator.validate(bean(HARD_BASE, 7, "last_updated"));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getConstraintDescriptor().getAnnotation()
          .annotationType().getCanonicalName())
              .isEqualTo("jp.ecuacion.lib.validation.constraints.EmptyWhen");
    }

    @Test
    @DisplayName("timestampColumn empty but kind set fails")
    void timestampColumnEmptyButKindSetFails() {
      @SuppressWarnings("null")
      Set<ConstraintViolation<HousekeepInfoBean>> result =
          validator.validate(bean(HARD_BASE, 8, "OffsetDateTime"));

      assertThat(result).hasSize(1);
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
      @SuppressWarnings("null")
      Set<ConstraintViolation<HousekeepInfoBean>> result =
          validator.validate(bean(HARD_BASE, 11, "upd_at"));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getConstraintDescriptor().getAnnotation()
          .annotationType().getCanonicalName())
              .isEqualTo("jp.ecuacion.lib.validation.constraints.EmptyWhen");
    }

    @Test
    @DisplayName("hard delete with update-user-id column set fails")
    void hardDeleteWithUpdateUserIdColumnFails() {
      // softDeleteUpdateUserIdColumn alone being non-empty also triggers the
      // "all-or-nothing" rule for its own quotation-mark/value pair, but since those stay
      // empty (condition not satisfied there, notEmptyWhenConditionNotSatisfied requires
      // them to be non-empty), this scenario is expected to raise 2 violations.
      @SuppressWarnings("null")
      Set<ConstraintViolation<HousekeepInfoBean>> result =
          validator.validate(bean(HARD_BASE, 12, "upd_by"));

      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("hard delete with both update columns empty passes")
    void hardDeleteWithColumnsEmptyPasses() {
      assertThat(validator.validate(bean(HARD_BASE))).isEmpty();
    }

    @Test
    @DisplayName("soft delete with update columns set passes (condition not satisfied)")
    void softDeleteWithColumnsSetPasses() {
      List<String> list = Arrays.asList("task1", "conn1", "Soft Delete", "SOFT_DELETE", "tbl1",
          "id1", "(none)", null, null, null, "del_flg", "upd_at", null, null, null);

      assertThat(validator.validate(new HousekeepInfoBean(list))).isEmpty();
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
      List<String> list = Arrays.asList("task1", "conn1", "Soft Delete", "SOFT_DELETE", "tbl1",
          "id1", "(none)", null, null, null, "del_flg", null, "upd_by", "quotes(')", "SYSTEM");

      assertThat(validator.validate(new HousekeepInfoBean(list))).isEmpty();
    }

    @Test
    @DisplayName("only softDeleteUpdateUserIdColumn set (symbol/value left empty) fails")
    void onlyUserIdColumnSetFails() {
      @SuppressWarnings("null")
      Set<ConstraintViolation<HousekeepInfoBean>> result =
          validator.validate(bean(SOFT_BASE, 12, "upd_by"));

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getConstraintDescriptor().getAnnotation()
          .annotationType().getCanonicalName())
              .isEqualTo("jp.ecuacion.lib.validation.constraints.EmptyWhen");
    }
  }

  // -------------------------------------------------------------------------
  // isSoftDelete()
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("isSoftDelete()")
  class IsSoftDelete {

    @Test
    @DisplayName("HARD_DELETE returns false")
    void hardDeleteReturnsFalse() {
      assertThat(bean(HARD_BASE).isSoftDelete()).isFalse();
    }

    @Test
    @DisplayName("SOFT_DELETE returns true")
    void softDeleteReturnsTrue() {
      assertThat(bean(SOFT_BASE).isSoftDelete()).isTrue();
    }

    @Test
    @DisplayName("an unrecognized internal value throws RuntimeException")
    void unrecognizedValueThrows() {
      HousekeepInfoBean b = bean(HARD_BASE, 3, "UNEXPECTED");

      assertThatThrownBy(b::isSoftDelete).isInstanceOf(RuntimeException.class);
    }
  }

  // -------------------------------------------------------------------------
  // getTimestampColumnKind()
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getTimestampColumnKind()")
  class GetTimestampColumnKind {

    @Test
    @DisplayName("'LocalDateTime' (any case) resolves to TimestampKindEnum.localDateTime")
    void localDateTime() {
      HousekeepInfoBean b = bean(HARD_BASE, 8, "localdatetime");

      assertThat(b.getTimestampColumnKind()).isEqualTo(TimestampKindEnum.localDateTime);
    }

    @Test
    @DisplayName("'OffsetDateTime' (any case) resolves to TimestampKindEnum.offsetDateTime")
    void offsetDateTime() {
      HousekeepInfoBean b = bean(HARD_BASE, 8, "OFFSETDATETIME");

      assertThat(b.getTimestampColumnKind()).isEqualTo(TimestampKindEnum.offsetDateTime);
    }

    @Test
    @DisplayName("an unrecognized value throws RuntimeException")
    void unrecognizedValueThrows() {
      HousekeepInfoBean b = bean(HARD_BASE, 8, "unexpected");

      assertThatThrownBy(b::getTimestampColumnKind).isInstanceOf(RuntimeException.class);
    }
  }

  // -------------------------------------------------------------------------
  // getDeleteTargetInDays() / timestampColumnDefines()
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getDeleteTargetInDays() / timestampColumnDefines()")
  class DeleteTargetInDaysAndTimestampColumnDefines {

    @Test
    @DisplayName("getDeleteTargetInDays parses the column as an int")
    void parsesInt() {
      assertThat(bean(HARD_BASE, 9, "30").getDeleteTargetInDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("timestampColumnDefines is false when the column is empty")
    void definesFalseWhenEmpty() {
      assertThat(bean(HARD_BASE).timestampColumnDefines()).isFalse();
    }

    @Test
    @DisplayName("timestampColumnDefines is true when the column is set")
    void definesTrueWhenSet() {
      assertThat(bean(HARD_BASE, 7, "last_updated").timestampColumnDefines()).isTrue();
    }
  }

  // -------------------------------------------------------------------------
  // afterReading() / constructColumnInfo()
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("afterReading()")
  class AfterReading {

    @Test
    @DisplayName("idColumnInfo is always constructed from idColumn/idColumnNeedsQuotationMark")
    void idColumnInfoAlwaysConstructed() {
      HousekeepInfoBean b = bean(HARD_BASE);
      b.afterReading();

      assertThat(b.getIdColumnInfo().getColumn()).isEqualTo("id1");
      assertThat(b.getIdColumnInfo().isNeedsQuotationMark()).isFalse();
    }

    @Test
    @DisplayName("softDeleteColumnInfo stays null when softDeleteColumn is empty")
    void softDeleteColumnInfoNullWhenEmpty() {
      HousekeepInfoBean b = bean(HARD_BASE);
      b.afterReading();

      assertThat(b.getSoftDeleteColumnInfo()).isNull();
    }

    @Test
    @DisplayName("softDeleteColumnInfo is constructed when softDeleteColumn is set")
    void softDeleteColumnInfoConstructedWhenSet() {
      HousekeepInfoBean b = bean(SOFT_BASE);
      b.afterReading();

      assertThat(b.getSoftDeleteColumnInfo().getColumn()).isEqualTo("del_flg");
    }

    @Test
    @DisplayName("softDeleteUpdateUserIdColumnAndValueInfo is constructed when its column is set")
    void updateUserIdColumnAndValueInfoConstructedWhenSet() {
      List<String> list = Arrays.asList("task1", "conn1", "Soft Delete", "SOFT_DELETE", "tbl1",
          "id1", "(none)", null, null, null, "del_flg", null, "upd_by", "quotes(')", "SYSTEM");
      HousekeepInfoBean b = new HousekeepInfoBean(list);
      b.afterReading();

      assertThat(b.getSoftDeleteUpdateUserIdColumnAndValueInfo().getSqlFragment())
          .isEqualTo("upd_by = 'SYSTEM'");
    }
  }
}
