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

/**
 * A condition bound via a JDBC {@code ?} placeholder rather than embedded as SQL literal text.
 *
 * <p>Use this - not {@link ColumnAndValueInfoBean} - whenever the value did not come from the
 *     excel config (e.g. an id or foreign-key value read back via {@code ResultSet}, or a
 *     Java-side computed value like "now" or a soft-delete flag): binding sidesteps SQL-literal
 *     escaping entirely, so there is no quoting/escaping bug class to get wrong for such values.
 *     {@link ColumnAndValueInfoBean} remains for excel-authored values, which still need literal
 *     embedding since their actual DB column type isn't tracked by the config.</p>
 */
public class BoundCondition implements SqlConditionInterface {

  private final String column;
  private final String operator;
  private final Object value;

  /**
   * Constructs an equality condition ({@code column = ?}).
   *
   * @param column column
   * @param value the value to bind
   */
  public BoundCondition(String column, Object value) {
    this(column, "=", value);
  }

  /**
   * Constructs a condition with the given comparison operator (e.g. {@code "="}, {@code ">"}).
   *
   * @param column column
   * @param operator SQL comparison operator
   * @param value the value to bind
   */
  public BoundCondition(String column, String operator, Object value) {
    this.column = column;
    this.operator = operator;
    this.value = value;
  }

  @Override
  public String getSqlFragment() {
    return column + " " + operator + " ?";
  }

  @Override
  public Object getBindValue() {
    return value;
  }
}
