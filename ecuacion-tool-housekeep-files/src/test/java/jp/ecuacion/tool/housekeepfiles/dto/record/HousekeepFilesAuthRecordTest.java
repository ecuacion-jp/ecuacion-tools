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
import java.util.List;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.validation.constraints.EnumElement;
import jp.ecuacion.lib.validation.constraints.IntegerString;
import jp.ecuacion.lib.validation.constraints.NotEmptyWhen;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the bean validation constraints on {@link HousekeepFilesAuthRecord}. */
@DisplayName("HousekeepFilesAuthRecord")
class HousekeepFilesAuthRecordTest {

  @SuppressWarnings("null")
  private static List<ConstraintViolation<?>> validate(HousekeepFilesAuthRecord rec) {
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
    @DisplayName("valid: authType PASSWORD with password filled, keyPath empty")
    void validPasswordAuth() {
      HousekeepFilesAuthRecord rec = new HousekeepFilesAuthRecord("aHost", "SFTP", "22",
          "PASSWORD", "aUser", "aPassword", null);

      assertThat(validate(rec)).isEmpty();
    }

    @Test
    @DisplayName("valid: authType KEY with keyPath filled, password empty")
    void validKeyAuth() {
      HousekeepFilesAuthRecord rec =
          new HousekeepFilesAuthRecord("aHost", "SFTP", "22", "KEY", "aUser", null, "/a/key/path");

      assertThat(validate(rec)).isEmpty();
    }

    @Test
    @DisplayName("valid: authType KERBEROS with password and keyPath both empty")
    void validKerberosAuth() {
      HousekeepFilesAuthRecord rec =
          new HousekeepFilesAuthRecord("aHost", "FTP", "21", "KERBEROS", "aUser", null, null);

      assertThat(validate(rec)).isEmpty();
    }
  }

  @Nested
  @DisplayName("protocol")
  class Protocol {

    private HousekeepFilesAuthRecord recordWithProtocol(@Nullable String protocol) {
      return new HousekeepFilesAuthRecord("aHost", protocol, "22", "PASSWORD", "aUser",
          "aPassword", null);
    }

    @Test
    @DisplayName("null violates @NotEmpty")
    void nullValue() {
      assertSingleViolation(validate(recordWithProtocol(null)), NotEmpty.class, "protocol");
    }

    @Test
    @DisplayName("string not defined in FileManipulateProtocolEnum violates @EnumElement")
    void unexpectedString() {
      assertSingleViolation(validate(recordWithProtocol("sftp")), EnumElement.class, "protocol");
    }
  }

  @Nested
  @DisplayName("port")
  class Port {

    private HousekeepFilesAuthRecord recordWithPort(String port) {
      return new HousekeepFilesAuthRecord("aHost", "SFTP", port, "PASSWORD", "aUser", "aPassword",
          null);
    }

    @Test
    @DisplayName("non-numeric string violates @IntegerString (in addition to @DecimalMin/@DecimalMax,"
        + " which also fire since Hibernate Validator does not short-circuit)")
    void nonNumeric() {
      assertThat(validate(recordWithPort("abc")))
          .extracting(cv -> (Object) cv.getConstraintDescriptor().getAnnotation().annotationType())
          .contains(IntegerString.class);
    }
  }

  @Nested
  @DisplayName("authType")
  class AuthType {

    private HousekeepFilesAuthRecord recordWithAuthType(@Nullable String authType) {
      return new HousekeepFilesAuthRecord("aHost", "SFTP", "22", authType, "aUser", "aPassword",
          null);
    }

    @Test
    @DisplayName("null violates @NotEmpty")
    void nullValue() {
      assertSingleViolation(validate(recordWithAuthType(null)), NotEmpty.class, "authType");
    }

    @Test
    @DisplayName("string not defined in AuthTypeEnum violates @EnumElement")
    void unexpectedString() {
      assertSingleViolation(validate(recordWithAuthType("AAA")), EnumElement.class, "authType");
    }
  }

  @Nested
  @DisplayName("userName")
  class UserName {

    @Test
    @DisplayName("null violates @NotEmpty")
    void nullValue() {
      HousekeepFilesAuthRecord rec =
          new HousekeepFilesAuthRecord("aHost", "SFTP", "22", "PASSWORD", null, "aPassword", null);

      assertSingleViolation(validate(rec), NotEmpty.class, "userName");
    }
  }

  @Nested
  @DisplayName("password required only when authType is PASSWORD")
  class PasswordRequiredWhenPassword {

    @Test
    @DisplayName("empty password with authType PASSWORD violates @NotEmptyWhen")
    void missingForPasswordAuth() {
      HousekeepFilesAuthRecord rec =
          new HousekeepFilesAuthRecord("aHost", "SFTP", "22", "PASSWORD", "aUser", null, null);

      assertThat(validate(rec)).singleElement()
          .satisfies(cv -> assertThat(cv.getConstraintDescriptor().getAnnotation().annotationType())
              .isEqualTo(NotEmptyWhen.class));
    }
  }

  @Nested
  @DisplayName("keyPath required only when authType is KEY")
  class KeyPathRequiredWhenKey {

    @Test
    @DisplayName("empty keyPath with authType KEY violates @NotEmptyWhen")
    void missingForKeyAuth() {
      HousekeepFilesAuthRecord rec =
          new HousekeepFilesAuthRecord("aHost", "SFTP", "22", "KEY", "aUser", null, null);

      assertThat(validate(rec)).singleElement()
          .satisfies(cv -> assertThat(cv.getConstraintDescriptor().getAnnotation().annotationType())
              .isEqualTo(NotEmptyWhen.class));
    }
  }
}
