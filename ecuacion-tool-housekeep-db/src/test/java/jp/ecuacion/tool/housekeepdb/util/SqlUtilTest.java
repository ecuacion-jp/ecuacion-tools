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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import jp.ecuacion.tool.housekeepdb.bean.BoundCondition;
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
      assertThat(parsed).isCloseTo(OffsetDateTime.now(ZoneId.systemDefault()),
          new TemporalUnitWithinOffset(10, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("'mysql' protocol returns a parsable offset-less date-time close to now")
    void mysql() {
      String result = SqlUtil.getTimestampNow("mysql");

      LocalDateTime parsed =
          LocalDateTime.parse(result, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      assertThat(parsed).isCloseTo(LocalDateTime.now(ZoneId.systemDefault()),
          new TemporalUnitWithinOffset(10, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("unrecognized protocol throws RuntimeException")
    void unrecognizedProtocol() {
      assertThatThrownBy(() -> SqlUtil.getTimestampNow("oracle"))
          .isInstanceOf(RuntimeException.class).hasMessageContaining("oracle");
    }
  }

  // -------------------------------------------------------------------------
  // getTimestampNowValue
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getTimestampNowValue")
  class GetTimestampNowValue {

    @Test
    @DisplayName("'postgresql' protocol returns an OffsetDateTime close to now")
    void postgresql() {
      Object result = SqlUtil.getTimestampNowValue("postgresql");

      assertThat(result).isInstanceOf(OffsetDateTime.class);
      assertThat((OffsetDateTime) result).isCloseTo(OffsetDateTime.now(ZoneId.systemDefault()),
          new TemporalUnitWithinOffset(10, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("'mysql' protocol returns an offset-less LocalDateTime close to now")
    void mysql() {
      Object result = SqlUtil.getTimestampNowValue("mysql");

      assertThat(result).isInstanceOf(LocalDateTime.class);
      assertThat((LocalDateTime) result).isCloseTo(LocalDateTime.now(ZoneId.systemDefault()),
          new TemporalUnitWithinOffset(10, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("unrecognized protocol throws RuntimeException")
    void unrecognizedProtocol() {
      assertThatThrownBy(() -> SqlUtil.getTimestampNowValue("oracle"))
          .isInstanceOf(RuntimeException.class).hasMessageContaining("oracle");
    }
  }

  // -------------------------------------------------------------------------
  // getExpirationCondition
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getExpirationCondition")
  class GetExpirationCondition {

    @Test
    @DisplayName("'postgresql' protocol renders interval subtraction with a quoted 'n days'")
    void postgresql() {
      String result = SqlUtil.getExpirationCondition("postgresql", "updated_at", 30);

      assertThat(result).matches(
          "'.+' - updated_at > '30 days'");
    }

    @Test
    @DisplayName("'mysql' protocol renders interval subtraction with a quoted 'n day'")
    void mysql() {
      String result = SqlUtil.getExpirationCondition("mysql", "updated_at", 30);

      assertThat(result).matches(
          "timestamp '.+' - interval '30' day > updated_at");
    }

    @Test
    @DisplayName("unrecognized protocol throws RuntimeException")
    void unrecognizedProtocol() {
      assertThatThrownBy(() -> SqlUtil.getExpirationCondition("oracle", "updated_at", 30))
          .isInstanceOf(RuntimeException.class).hasMessageContaining("oracle");
    }
  }

  // -------------------------------------------------------------------------
  // getWhere
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getWhere")
  class GetWhere {

    @Test
    @DisplayName("empty list returns an empty string (no where clause) and no bind values")
    void emptyList() {
      SqlUtil.SqlFragment result = SqlUtil.getWhere(Collections.emptyList());

      assertThat(result.sql()).isEqualTo("");
      assertThat(result.bindValues()).isEmpty();
    }

    @Test
    @DisplayName("single literal condition is prefixed with ' where '")
    void singleCondition() {
      List<SqlConditionInterface> list =
          List.of(new ColumnAndValueStringBean("col1 = 1"));

      assertThat(SqlUtil.getWhere(list).sql()).isEqualTo(" where col1 = 1");
    }

    @Test
    @DisplayName("multiple conditions are joined with ' and '")
    void multipleConditions() {
      List<SqlConditionInterface> list = List.of(new ColumnAndValueStringBean("col1 = 1"),
          new ColumnAndValueStringBean("col2 = 2"));

      assertThat(SqlUtil.getWhere(list).sql()).isEqualTo(" where col1 = 1 and col2 = 2");
    }

    @Test
    @DisplayName("varargs overload behaves the same as the List overload")
    void varargsOverload() {
      assertThat(SqlUtil.getWhere(new ColumnAndValueStringBean("col1 = 1"),
          new ColumnAndValueStringBean("col2 = 2")))
              .isEqualTo(SqlUtil.getWhere(List.of(new ColumnAndValueStringBean("col1 = 1"),
                  new ColumnAndValueStringBean("col2 = 2"))));
    }

    @Test
    @DisplayName("condition built from ColumnAndValueInfoBean renders 'column = value' (literal,"
        + " no bind value)")
    void withColumnAndValueInfoBean() {
      List<SqlConditionInterface> list =
          List.of(new ColumnAndValueInfoBean("id", true, "abc"));

      SqlUtil.SqlFragment result = SqlUtil.getWhere(list);
      assertThat(result.sql()).isEqualTo(" where id = 'abc'");
      assertThat(result.bindValues()).isEmpty();
    }

    @Test
    @DisplayName("condition built from BoundCondition renders 'column = ?' and collects the bind"
        + " value")
    void withBoundCondition() {
      List<SqlConditionInterface> list = List.of(new BoundCondition("id", 42));

      SqlUtil.SqlFragment result = SqlUtil.getWhere(list);
      assertThat(result.sql()).isEqualTo(" where id = ?");
      assertThat(result.bindValues()).containsExactly(42);
    }

    @Test
    @DisplayName("bind values are collected in the same left-to-right order as their '?'"
        + " placeholders, skipping literal (non-bound) conditions in between")
    void bindValuesPreserveOrderAmongMixedConditions() {
      List<SqlConditionInterface> list = List.of(new BoundCondition("a", 1),
          new ColumnAndValueStringBean("b = 2"), new BoundCondition("c", 3));

      SqlUtil.SqlFragment result = SqlUtil.getWhere(list);
      assertThat(result.sql()).isEqualTo(" where a = ? and b = 2 and c = ?");
      assertThat(result.bindValues()).containsExactly(1, 3);
    }
  }

  // -------------------------------------------------------------------------
  // getUpdateSet
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getUpdateSet")
  class GetUpdateSet {

    @Test
    @DisplayName("empty list still renders the 'set ' prefix, with no bind values")
    void emptyList() {
      SqlUtil.SqlFragment result = SqlUtil.getUpdateSet(Collections.emptyList());

      assertThat(result.sql()).isEqualTo(" set ");
      assertThat(result.bindValues()).isEmpty();
    }

    @Test
    @DisplayName("single literal assignment")
    void singleAssignment() {
      List<SqlConditionInterface> list =
          List.of(new ColumnAndValueInfoBean("deleted", false, "true"));

      assertThat(SqlUtil.getUpdateSet(list).sql()).isEqualTo(" set deleted = true");
    }

    @Test
    @DisplayName("multiple assignments are joined with ', '")
    void multipleAssignments() {
      List<SqlConditionInterface> list = List.of(new ColumnAndValueInfoBean("deleted", false, "true"),
          new ColumnAndValueInfoBean("updated_by", true, "user1"));

      assertThat(SqlUtil.getUpdateSet(list).sql())
          .isEqualTo(" set deleted = true, updated_by = 'user1'");
    }

    @Test
    @DisplayName("varargs overload behaves the same as the List overload")
    void varargsOverload() {
      assertThat(SqlUtil.getUpdateSet(new ColumnAndValueInfoBean("deleted", false, "true")))
          .isEqualTo(SqlUtil.getUpdateSet(List.of(new ColumnAndValueInfoBean("deleted", false, "true"))));
    }

    @Test
    @DisplayName("a bound assignment renders 'column = ?' and collects the bind value")
    void withBoundCondition() {
      List<SqlConditionInterface> list =
          List.of(new BoundCondition("deleted", Boolean.TRUE));

      SqlUtil.SqlFragment result = SqlUtil.getUpdateSet(list);
      assertThat(result.sql()).isEqualTo(" set deleted = ?");
      assertThat(result.bindValues()).containsExactly(Boolean.TRUE);
    }
  }
}
