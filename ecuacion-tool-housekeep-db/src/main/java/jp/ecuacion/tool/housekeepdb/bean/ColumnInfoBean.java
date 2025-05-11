package jp.ecuacion.tool.housekeepdb.bean;

import jakarta.validation.constraints.NotEmpty;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.unchecked.EclibRuntimeException;
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
  public ColumnInfoBean(String column, String needsQuotationMarkExcelString)
      throws BizLogicAppException {
    this.column = column;
    this.needsQuotationMark =
        getNeedsQuotationMarkBooleanFromExcelString(needsQuotationMarkExcelString);
  }

  private Boolean getNeedsQuotationMarkBooleanFromExcelString(String value)
      throws BizLogicAppException {
    if (value == null) { 
      return null;
      
    } else if (value.equals(NO_MARK)) {
      return false;

    } else if (value.equals(QUOTES)) {
      return true;

    } else {
      throw new EclibRuntimeException(
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
