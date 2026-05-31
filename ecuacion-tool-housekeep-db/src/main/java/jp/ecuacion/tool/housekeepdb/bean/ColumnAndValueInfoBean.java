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

/**
 * Stores database column and its value information to create condition clause.
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
   * @return String
   */
  public String surroundWithQuotationMarks() {
    String mark = isNeedsQuotationMark() ? "'" : "";

    return mark + value.toString() + mark;
  }

  @Override
  public String getCondition() {
    return getColumn() + " = " + surroundWithQuotationMarks();
  }
}
