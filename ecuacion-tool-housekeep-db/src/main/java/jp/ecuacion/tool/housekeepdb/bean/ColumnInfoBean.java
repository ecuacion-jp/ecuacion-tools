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
import jp.ecuacion.tool.housekeepdb.util.SqlUtil;

/**
 * Stores database column information.
 */
public class ColumnInfoBean {
  private static final String NO_MARK = "(none)";
  private static final String QUOTES = "quotes(')";

  @NotEmpty
  private String column;

  private boolean needsQuotationMark;

  /**
   * Constructs a new instance.
   * 
   * @param column column
   * @param needsQuationMark needsQuationMark
   */
  public ColumnInfoBean(String column, boolean needsQuationMark) {
    this.column = column;
    this.needsQuotationMark = needsQuationMark;
  }

  /**
   * Constructs a new instance.
   * 
   * @param column column
   * @param needsQuotationMarkExcelString String that represents needsQuationMark
   */
  public ColumnInfoBean(String column, String needsQuotationMarkExcelString) {
    this.column = column;
    this.needsQuotationMark =
        getNeedsQuotationMarkBooleanFromExcelString(needsQuotationMarkExcelString);
  }

  private boolean getNeedsQuotationMarkBooleanFromExcelString(String value) {
    if (value.equals(NO_MARK)) {
      return false;

    } else if (value.equals(QUOTES)) {
      return true;

    } else {
      throw new RuntimeException(
          "The value must be either '" + NO_MARK + "' or '" + QUOTES + "'.");
    }
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public boolean isNeedsQuotationMark() {
    return needsQuotationMark;
  }

  public void setNeedsQuotationMark(boolean needsQuotationMark) {
    this.needsQuotationMark = needsQuotationMark;
  }

  /**
   * Receives value as argument and returns {@code ColumnAndValueInfoBean}.
   * 
   * @param value value
   * @return ColumnAndValueInfoBean
   */
  public ColumnAndValueInfoBean getColumnAndValueInfo(Object value) {
    return new ColumnAndValueInfoBean(column, isNeedsQuotationMark(), value);
  }

  /** 
   * Get {@code ColumnAndValueInfoBean} with current time is set as its value. 
   */
  public ColumnAndValueInfoBean getTimestampColumnNowInfo(String protocol) {
    String now = SqlUtil.getTimestampNow(protocol);
    return new ColumnAndValueInfoBean(column, true, now);
  }
}
