package jp.ecuacion.tool.housekeepdb.bean;

import jakarta.validation.constraints.NotEmpty;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;

/**
 * Stores database column and its value information to create condition clause.
 */
public class ColumnAndValueInfoBean extends ColumnInfoBean
    implements SqlConditionInterface {

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
  public ColumnAndValueInfoBean(String column, String needsQuationMark, Object value)
      throws BizLogicAppException {
    super(column, needsQuationMark);
    this.value = getStringFromObject(value);
  }

  private String getStringFromObject(Object value) {
    if (value instanceof String) {
      return (String) value;

    } else {
      // 数字やその他諸々を含め、一旦雑にこう書いてみる。問題起きたら適宜対処で。
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

  public String getCondition() {
    return getColumn() + " = " + surroundWithQuotationMarks();
  }
}
