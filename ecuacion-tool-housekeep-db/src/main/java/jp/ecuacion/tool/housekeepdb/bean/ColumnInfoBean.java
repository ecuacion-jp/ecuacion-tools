package jp.ecuacion.tool.housekeepdb.bean;

import jakarta.validation.constraints.NotEmpty;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil;

public class ColumnInfoBean {
  private static final String noMark = "(none)";
  // private static final String quotes = "quotes(')";

  @NotEmpty
  private String column;

  private boolean needsQuotationMark;

  public ColumnInfoBean(String column, boolean needsQuationMark) {
    this.column = column;
    this.needsQuotationMark = needsQuationMark;
  }

  public ColumnInfoBean(String column, String needsQuotationMarkExcelString)
      throws BizLogicAppException {
    this.column = column;
    this.needsQuotationMark =
        getNeedsQuotationMarkBooleanFromExcelString(needsQuotationMarkExcelString);
  }

  private boolean getNeedsQuotationMarkBooleanFromExcelString(String value)
      throws BizLogicAppException {
    if (value.equals(noMark)) {
      return false;

    } else {
      return true;
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

  public ColumnAndValueInfoBean getColumnAndValueInfo(Object value) {
    return new ColumnAndValueInfoBean(column, isNeedsQuotationMark(), value);
  }

  /** 現在時刻を値として埋めたColumnAndValueInfoBeanを返す。 */
  public ColumnAndValueInfoBean getTimestampColumnNowInfo(String protocol) {
    String now = new SqlUtil().getTimestampNow(protocol);
    return new ColumnAndValueInfoBean(column, true, now);
  }
}
