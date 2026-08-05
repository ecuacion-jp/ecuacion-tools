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
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests for {@link DbConnectionInfoBean}. */
@DisplayName("DbConnectionInfoBean")
class DbConnectionInfoBeanTest {

  private static final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  private static DbConnectionInfoBean valid() {
    return new DbConnectionInfoBean("conn1", "org.postgresql.Driver", "postgresql", "localhost",
        "5432", "mydb", "public", "user1", "pass1");
  }

  // -------------------------------------------------------------------------
  // getters
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("constructor / getters")
  class Getters {

    @Test
    @DisplayName("all fields are stored in the declared column order")
    void allFieldsStored() {
      DbConnectionInfoBean bean = valid();

      assertThat(bean.getId()).isEqualTo("conn1");
      assertThat(bean.getDriverName()).isEqualTo("org.postgresql.Driver");
      assertThat(bean.getProtocol()).isEqualTo("postgresql");
      assertThat(bean.getServer()).isEqualTo("localhost");
      assertThat(bean.getPort()).isEqualTo("5432");
      assertThat(bean.getDatabase()).isEqualTo("mydb");
      assertThat(bean.getSchema()).isEqualTo("public");
      assertThat(bean.getUsername()).isEqualTo("user1");
      assertThat(bean.getPassword()).isEqualTo("pass1");
    }
  }

  // -------------------------------------------------------------------------
  // bean validation
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("bean validation")
  class BeanValidation {

    @Test
    @DisplayName("a fully-populated bean passes validation")
    void validBeanPasses() {
      assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    @DisplayName("schema is optional: empty schema still passes")
    void emptySchemaPasses() {
      DbConnectionInfoBean bean = new DbConnectionInfoBean("conn1", "org.postgresql.Driver",
          "postgresql", "localhost", "5432", "mydb", "", "user1", "pass1");

      assertThat(validator.validate(bean)).isEmpty();
    }

    @ParameterizedTest(name = "{0} empty -> @NotEmpty violation on that field")
    @MethodSource("jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBeanTest#requiredFieldMutators")
    @DisplayName("each required field, when empty, fails @NotEmpty on that field alone")
    void requiredFieldEmptyFails(String propertyName, DbConnectionInfoBean bean) {
      Set<ConstraintViolation<DbConnectionInfoBean>> result = validator.validate(bean);

      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getPropertyPath().toString()).isEqualTo(propertyName);
    }
  }

  /** Provides (property name, bean-with-that-property-emptied) pairs for required fields. */
  static Stream<Arguments> requiredFieldMutators() {
    return Stream.of(
        Arguments.of("id", new DbConnectionInfoBean("", "org.postgresql.Driver", "postgresql",
            "localhost", "5432", "mydb", "public", "user1", "pass1")),
        Arguments.of("driverName", new DbConnectionInfoBean("conn1", "", "postgresql", "localhost",
            "5432", "mydb", "public", "user1", "pass1")),
        Arguments.of("protocol", new DbConnectionInfoBean("conn1", "org.postgresql.Driver", "",
            "localhost", "5432", "mydb", "public", "user1", "pass1")),
        Arguments.of("server", new DbConnectionInfoBean("conn1", "org.postgresql.Driver",
            "postgresql", "", "5432", "mydb", "public", "user1", "pass1")),
        Arguments.of("port", new DbConnectionInfoBean("conn1", "org.postgresql.Driver",
            "postgresql", "localhost", "", "mydb", "public", "user1", "pass1")),
        Arguments.of("database", new DbConnectionInfoBean("conn1", "org.postgresql.Driver",
            "postgresql", "localhost", "5432", "", "public", "user1", "pass1")),
        Arguments.of("username", new DbConnectionInfoBean("conn1", "org.postgresql.Driver",
            "postgresql", "localhost", "5432", "mydb", "public", "", "pass1")),
        Arguments.of("password", new DbConnectionInfoBean("conn1", "org.postgresql.Driver",
            "postgresql", "localhost", "5432", "mydb", "public", "user1", "")));
  }
}
