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
 * A literal WHERE condition with a comparison operator, built from a "Search Condition Settings"
 * row.
 *
 * <p>Unlike {@link ColumnAndValueInfoBean}, which always renders {@code column = value} (it is
 *     also used for SET clauses), this class renders the condition according to the operator and
 *     the value:</p>
 *
 * <ul>
 * <li>{@value #OPERATOR_EQUAL} and a value: {@code column = value}</li>
 * <li>{@value #OPERATOR_NOT_EQUAL} and a value: {@code (column <> value or column is null)}.
 *     The {@code is null} part is added because SQL's {@code NULL <> value} is not true, which
 *     would silently leave out rows whose column is null - not what "not equal" means to someone
 *     filling in the excel config.</li>
 * <li>{@value #OPERATOR_EQUAL} and {@value #NULL_VALUE}: {@code column is null}</li>
 * <li>{@value #OPERATOR_NOT_EQUAL} and {@value #NULL_VALUE}: {@code column is not null}</li>
 * </ul>
 *
 * <p>{@value #NULL_VALUE} means null whether or not the value is set to be quoted.</p>
 */
public class ColumnAndValueConditionBean extends ColumnAndValueInfoBean {
  public static final String OPERATOR_EQUAL = "equal";
  public static final String OPERATOR_NOT_EQUAL = "not equal";
  public static final String NULL_VALUE = "(null)";

  private final boolean isNotEqual;

  /**
   * Constructs a new instance.
   *
   * @param column column
   * @param operator {@value #OPERATOR_EQUAL} or {@value #OPERATOR_NOT_EQUAL}
   * @param needsQuationMark String that represents needsQuationMark
   * @param value value, or {@value #NULL_VALUE}
   */
  public ColumnAndValueConditionBean(String column, String operator, String needsQuationMark,
      Object value) {
    super(column, needsQuationMark, value);
    this.isNotEqual = getIsNotEqualFromExcelString(operator);
  }

  private boolean getIsNotEqualFromExcelString(String operator) {
    if (operator.equals(OPERATOR_EQUAL)) {
      return false;

    } else if (operator.equals(OPERATOR_NOT_EQUAL)) {
      return true;

    } else {
      throw new RuntimeException("The operator must be either '" + OPERATOR_EQUAL + "' or '"
          + OPERATOR_NOT_EQUAL + "'.");
    }
  }

  @Override
  public String getSqlFragment() {
    if (NULL_VALUE.equals(getValue())) {
      return getColumn() + (isNotEqual ? " is not null" : " is null");
    }

    if (!isNotEqual) {
      return super.getSqlFragment();
    }

    return "(" + getColumn() + " <> " + surroundWithQuotationMarks() + " or " + getColumn()
        + " is null)";
  }
}
