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

import jakarta.validation.constraints.NotEmpty;
import org.jspecify.annotations.Nullable;

/**
 * Stores database column and its value information to create a literal (not parameter-bound)
 * condition clause.
 *
 * <p>Only used for values typed into the excel config (e.g. {@code WhereConditionInfoBean}'s
 *     search-condition value, or a soft-delete-update "updated by" user id), since the excel
 *     config never records the actual DB column type - only whether to quote it - so those values
 *     cannot be handed to a type-aware JDBC bind. Such values are also author-controlled, not
 *     read back from the DB, so unlike an id or foreign-key value they were never at risk of
 *     carrying attacker-influenced content. Anything read back from the DB (an id, a foreign-key
 *     value) or computed by this tool itself (a soft-delete flag, "now") should use
 *     {@link BoundCondition} instead, which sidesteps escaping entirely.</p>
 */
public class ColumnAndValueInfoBean extends ColumnInfoBean implements SqlConditionInterface {

  @NotEmpty
  private String value;

  /**
   * Construct a new instance.
   *
   * @param column column
   * @param needsQuationMark needsQuationMark
   * @param value value
   */
  public ColumnAndValueInfoBean(String column, boolean needsQuationMark, Object value) {
    super(column, needsQuationMark);
    this.value = getStringFromObject(value);
  }

  /**
   * Construct a new instance.
   *
   * @param column column
   * @param needsQuationMark needsQuationMark
   * @param value value
   */
  public ColumnAndValueInfoBean(String column, String needsQuationMark, Object value) {
    super(column, needsQuationMark);
    this.value = getStringFromObject(value);
  }

  private String getStringFromObject(Object value) {
    if (value instanceof String s) {
      return s;

    } else {
      // Roughly covers numbers and various other cases. Address any issues as they arise.
      return value.toString();
    }
  }

  public Object getValue() {
    return value;
  }

  /**
   * Adds quotation mark at the both side of the string if isNeedsQuotationMark() == true.
   *
   * <p>When quoted, a single quote in the value is escaped by doubling it (the standard SQL
   *     string-literal escape, valid for PostgreSQL under the default
   *     {@code standard_conforming_strings=on}). This class is only ever used for excel-authored
   *     values (see the class Javadoc), which are trusted input, so this is defense-in-depth
   *     rather than a guard against attacker-controlled content.</p>
   *
   * @return String
   */
  public String surroundWithQuotationMarks() {
    if (!isNeedsQuotationMark()) {
      return value.toString();
    }

    return "'" + value.toString().replace("'", "''") + "'";
  }

  @Override
  public String getSqlFragment() {
    return getColumn() + " = " + surroundWithQuotationMarks();
  }

  @Override
  public @Nullable Object getBindValue() {
    return null;
  }
}
