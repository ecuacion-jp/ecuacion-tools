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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link BoundCondition}. */
@DisplayName("BoundCondition")
class BoundConditionTest {

  @Test
  @DisplayName("the (column, value) constructor builds an equality condition")
  void equalityCondition() {
    BoundCondition condition = new BoundCondition("id", 42);

    assertThat(condition.getSqlFragment()).isEqualTo("id = ?");
    assertThat(condition.getBindValue()).isEqualTo(42);
  }

  @Test
  @DisplayName("the (column, operator, value) constructor uses the given operator")
  void customOperatorCondition() {
    BoundCondition condition = new BoundCondition("id", ">", 42);

    assertThat(condition.getSqlFragment()).isEqualTo("id > ?");
    assertThat(condition.getBindValue()).isEqualTo(42);
  }

  @Test
  @DisplayName("the bind value is returned as-is, with no formatting or escaping - binding needs"
      + " none")
  void bindValueReturnedVerbatim() {
    Object value = "o'brien\\";
    BoundCondition condition = new BoundCondition("name", value);

    assertThat(condition.getBindValue()).isSameAs(value);
  }
}
