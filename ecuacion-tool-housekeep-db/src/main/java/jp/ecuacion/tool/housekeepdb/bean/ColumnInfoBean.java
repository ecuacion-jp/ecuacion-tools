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
   * Receives value as argument and returns a literal {@code ColumnAndValueInfoBean}.
   *
   * <p>Only appropriate for a value typed into the excel config (this bean's own
   *     {@code needsQuotationMark} setting came from there too, and is meaningless for anything
   *     else) - see {@link ColumnAndValueInfoBean}'s class Javadoc. A value read back from the DB
   *     or computed by this tool should use {@link #getBoundCondition(Object)} instead.</p>
   *
   * @param value value
   * @return ColumnAndValueInfoBean
   */
  public ColumnAndValueInfoBean getColumnAndValueInfo(Object value) {
    return new ColumnAndValueInfoBean(column, isNeedsQuotationMark(), value);
  }

  /**
   * Builds an equality condition ({@code column = ?}) binding {@code value} as a JDBC parameter,
   * for a value read back from the DB (e.g. an id from a {@code ResultSet}) or otherwise not
   * typed into the excel config - see {@link BoundCondition}'s class Javadoc.
   *
   * @param value the value to bind
   * @return BoundCondition
   */
  public BoundCondition getBoundCondition(Object value) {
    return new BoundCondition(column, value);
  }

  /**
   * Builds a greater-than condition ({@code column > ?}) binding {@code value} as a JDBC
   * parameter - see {@link #getBoundCondition(Object)}.
   *
   * @param value the value to bind
   * @return BoundCondition
   */
  public BoundCondition getBoundGreaterThanCondition(Object value) {
    return new BoundCondition(column, ">", value);
  }

  /**
   * Builds an equality condition ({@code column = ?}) binding the current time as a JDBC
   * parameter, typed to suit {@code protocol} the same way {@link SqlUtil#getTimestampNow}
   * formats it for literal embedding - see {@link SqlUtil#getTimestampNowValue}.
   *
   * @param protocol database kind like 'postgresql'
   * @return BoundCondition
   */
  public BoundCondition getBoundTimestampNowCondition(String protocol) {
    return new BoundCondition(column, SqlUtil.getTimestampNowValue(protocol));
  }
}
