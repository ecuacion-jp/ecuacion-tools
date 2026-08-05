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
package jp.ecuacion.tool.housekeepdb.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueStringBean;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link SqlUtil}. */
@DisplayName("SqlUtil")
class SqlUtilTest {

  // -------------------------------------------------------------------------
  // getTimestampNow
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getTimestampNow")
  class GetTimestampNow {

    @Test
    @DisplayName("'postgresql' protocol returns a parsable ISO offset date-time close to now")
    void postgresql() {
      String result = SqlUtil.getTimestampNow("postgresql");

      OffsetDateTime parsed = OffsetDateTime.parse(result, DateTimeFormatter.ISO_DATE_TIME);
      assertThat(parsed).isCloseTo(OffsetDateTime.now(),
          new TemporalUnitWithinOffset(10, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("unrecognized protocol throws RuntimeException")
    void unrecognizedProtocol() {
      assertThatThrownBy(() -> SqlUtil.getTimestampNow("mysql"))
          .isInstanceOf(RuntimeException.class).hasMessageContaining("mysql");
    }
  }

  // -------------------------------------------------------------------------
  // getWhere
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getWhere")
  class GetWhere {

    @Test
    @DisplayName("empty list returns an empty string (no where clause)")
    void emptyList() {
      assertThat(SqlUtil.getWhere(Collections.emptyList())).isEqualTo("");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("single condition is prefixed with '\\nwhere '")
    void singleCondition() {
      List<SqlConditionInterface> list =
          List.of(new ColumnAndValueStringBean("col1 = 1"));

      assertThat(SqlUtil.getWhere(list)).isEqualTo("\nwhere col1 = 1");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("multiple conditions are joined with ' and '")
    void multipleConditions() {
      List<SqlConditionInterface> list = List.of(new ColumnAndValueStringBean("col1 = 1"),
          new ColumnAndValueStringBean("col2 = 2"));

      assertThat(SqlUtil.getWhere(list)).isEqualTo("\nwhere col1 = 1 and col2 = 2");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("varargs overload behaves the same as the List overload")
    void varargsOverload() {
      assertThat(SqlUtil.getWhere(new ColumnAndValueStringBean("col1 = 1"),
          new ColumnAndValueStringBean("col2 = 2")))
              .isEqualTo(SqlUtil.getWhere(List.of(new ColumnAndValueStringBean("col1 = 1"),
                  new ColumnAndValueStringBean("col2 = 2"))));
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("condition built from ColumnAndValueInfoBean renders 'column = value'")
    void withColumnAndValueInfoBean() {
      List<SqlConditionInterface> list =
          List.of(new ColumnAndValueInfoBean("id", true, "abc"));

      assertThat(SqlUtil.getWhere(list)).isEqualTo("\nwhere id = 'abc'");
    }
  }

  // -------------------------------------------------------------------------
  // getUpdateSet
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getUpdateSet")
  class GetUpdateSet {

    @Test
    @DisplayName("empty list still renders the 'set ' prefix")
    void emptyList() {
      assertThat(SqlUtil.getUpdateSet(Collections.emptyList())).isEqualTo("\nset ");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("single assignment")
    void singleAssignment() {
      List<SqlConditionInterface> list =
          List.of(new ColumnAndValueInfoBean("deleted", false, "true"));

      assertThat(SqlUtil.getUpdateSet(list)).isEqualTo("\nset deleted = true");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("multiple assignments are joined with ', '")
    void multipleAssignments() {
      List<SqlConditionInterface> list = List.of(new ColumnAndValueInfoBean("deleted", false, "true"),
          new ColumnAndValueInfoBean("updated_by", true, "user1"));

      assertThat(SqlUtil.getUpdateSet(list)).isEqualTo("\nset deleted = true, updated_by = 'user1'");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("varargs overload behaves the same as the List overload")
    void varargsOverload() {
      assertThat(SqlUtil.getUpdateSet(new ColumnAndValueInfoBean("deleted", false, "true")))
          .isEqualTo(SqlUtil.getUpdateSet(List.of(new ColumnAndValueInfoBean("deleted", false, "true"))));
    }
  }
}
