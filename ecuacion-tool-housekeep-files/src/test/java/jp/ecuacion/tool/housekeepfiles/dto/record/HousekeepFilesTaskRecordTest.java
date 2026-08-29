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
package jp.ecuacion.tool.housekeepfiles.dto.record;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.validation.constraints.BooleanString;
import jp.ecuacion.lib.validation.constraints.EnumElement;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the bean validation constraints on {@link HousekeepFilesTaskRecord}. */
@DisplayName("HousekeepFilesTaskRecord")
class HousekeepFilesTaskRecordTest {

  @SuppressWarnings("null")
  private static List<ConstraintViolation<?>> validate(HousekeepFilesTaskRecord rec) {
    return new Violations().validate(rec).getConstraintViolations();
  }

  private static void assertSingleViolation(List<ConstraintViolation<?>> violations,
      Class<?> constraintAnnotation, String propertyPath) {
    assertThat(violations).singleElement().satisfies(cv -> {
      assertThat(cv.getConstraintDescriptor().getAnnotation().annotationType())
          .isEqualTo(constraintAnnotation);
      assertThat(cv.getPropertyPath().toString()).isEqualTo(propertyPath);
    });
  }

  @Nested
  @DisplayName("record as a whole")
  class RecordAsAWhole {

    @Test
    @DisplayName("valid: only required items filled, all others null")
    void allNullExceptRequired() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "CREATE_DIR", null, null, null, null, null, null, null, null, null, null, null);

      assertThat(validate(rec)).isEmpty();
    }

    @Test
    @DisplayName("valid: all items filled with normal values")
    void allItemsNormalStringInput() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "SFTP_MOVE_FROM_SERVER", "aHost", "aPath", "TRUE", "DAY", "7", "IGNORE", "aPath", "TRUE",
          "FALSE", "IGNORE", "key1=value2,key2");

      assertThat(validate(rec)).isEmpty();
    }
  }

  @Nested
  @DisplayName("taskId")
  class TaskId {

    private HousekeepFilesTaskRecord recordWithTaskId(@Nullable String taskId) {
      return new HousekeepFilesTaskRecord(taskId, "aTaskName", "CREATE_DIR", null, null, null,
          null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("null violates @NotEmpty")
    void nullValue() {
      assertSingleViolation(validate(recordWithTaskId(null)), NotEmpty.class, "taskId");
    }

    @Test
    @DisplayName("empty string violates @NotEmpty and @Size")
    void emptyString() {
      List<ConstraintViolation<?>> violations = validate(recordWithTaskId(""));

      assertThat(violations).hasSize(2)
          .allSatisfy(cv -> assertThat(cv.getPropertyPath().toString()).isEqualTo("taskId"))
          .extracting(cv -> (Object) cv.getConstraintDescriptor().getAnnotation().annotationType())
          .containsExactlyInAnyOrder(NotEmpty.class, Size.class);
    }

    @Test
    @DisplayName("11 characters violates @Size (max 10)")
    void tooLong() {
      assertSingleViolation(validate(recordWithTaskId("12345678901")), Size.class, "taskId");
    }

    @Test
    @DisplayName("illegal character '#' violates @Pattern")
    void illegalCharacter() {
      assertSingleViolation(validate(recordWithTaskId("task#")), Pattern.class, "taskId");
    }
  }

  @Nested
  @DisplayName("taskName")
  class TaskName {

    private HousekeepFilesTaskRecord recordWithTaskName(@Nullable String taskName) {
      return new HousekeepFilesTaskRecord("aTaskId", taskName, "CREATE_DIR", null, null, null,
          null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("null violates @NotEmpty")
    void nullValue() {
      assertSingleViolation(validate(recordWithTaskName(null)), NotEmpty.class, "taskName");
    }

    @Test
    @DisplayName("empty string violates @NotEmpty and @Size")
    void emptyString() {
      List<ConstraintViolation<?>> violations = validate(recordWithTaskName(""));

      assertThat(violations).hasSize(2)
          .allSatisfy(cv -> assertThat(cv.getPropertyPath().toString()).isEqualTo("taskName"))
          .extracting(cv -> (Object) cv.getConstraintDescriptor().getAnnotation().annotationType())
          .containsExactlyInAnyOrder(NotEmpty.class, Size.class);
    }

    @Test
    @DisplayName("41 characters violates @Size (max 40)")
    void tooLong() {
      assertSingleViolation(
          validate(recordWithTaskName("12345678901234567890123456789012345678901")), Size.class,
          "taskName");
    }

    @Test
    @DisplayName("illegal character '#' violates @Pattern")
    void illegalCharacter() {
      assertSingleViolation(validate(recordWithTaskName("aTaskName#")), Pattern.class, "taskName");
    }
  }

  @Nested
  @DisplayName("taskPtnEnumName")
  class TaskPtnEnumName {

    private HousekeepFilesTaskRecord recordWithTaskPtn(@Nullable String taskPtnEnumName) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", taskPtnEnumName, null, null,
          null, null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("null violates @NotEmpty")
    void nullValue() {
      assertSingleViolation(validate(recordWithTaskPtn(null)), NotEmpty.class, "taskPtnEnumName");
    }

    @Test
    @DisplayName("string not defined in TaskPtnEnum violates @EnumElement")
    void unexpectedString() {
      assertSingleViolation(validate(recordWithTaskPtn("AAA")), EnumElement.class,
          "taskPtnEnumName");
    }
  }

  @Nested
  @DisplayName("isSrcPathDirEnumName")
  class IsSrcPathDirEnumName {

    @Test
    @DisplayName("non-boolean string violates @BooleanString")
    void unsupportedString() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE",
          null, "aPath", "はい", "DAY", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE", null);

      assertSingleViolation(validate(rec), BooleanString.class, "isSrcPathDirEnumName");
    }
  }

  @Nested
  @DisplayName("actionForNoSrcPathEnumName")
  class ActionForNoSrcPathEnumName {

    @Test
    @DisplayName("string not defined in IncidentTreatedAsEnum violates @EnumElement")
    void unexpectedString() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE",
          null, "aPath", "TRUE", "DAY", "7", "無視", "aPath", "TRUE", "FALSE", "IGNORE", null);

      assertSingleViolation(validate(rec), EnumElement.class, "actionForNoSrcPathEnumName");
    }
  }
}
