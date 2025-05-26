package jp.ecuacion.tool.housekeepdb.bean.forexceltable;

import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.lang.LangExcel;
import jp.ecuacion.util.poi.excel.table.bean.StringExcelTableBean;

/**
 * Stores where clause settings.
 */
public class WhereConditionInfoBean extends StringExcelTableBean {
  @NotEmpty
  private String taskId;
  @NotEmpty
  private String conditionColumn;
  @NotEmpty
  private String conditionColumnNeedsQuotationMark;
  @NotEmpty
  private String conditionColumnValue;
  @Valid
  private ColumnAndValueInfoBean conditionColumnInfo;

  public static final String[] HEADER_LABEL_KEYS = LangExcel.SearchConditionSettings.HEADER_LABELS;

  @Override
  protected @Nonnull String[] getFieldNameArray() {
    return new String[] {"taskId", "conditionColumn", "conditionColumnNeedsQuotationMark",
        "conditionColumnValue"};
  }

  /**
   * Constructs a new instance.
   * 
   * @param colList colList
   * @throws BizLogicAppException BizLogicAppException
   */
  public WhereConditionInfoBean(List<String> colList) throws BizLogicAppException {
    super(colList);

    conditionColumnInfo = new ColumnAndValueInfoBean(conditionColumn,
        conditionColumnNeedsQuotationMark, conditionColumnValue);
  }

  public String getTaskId() {
    return taskId;
  }

  public String getConditionColumn() {
    return conditionColumn;
  }

  public String getConditionColumnNeedsQuotationMark() {
    return conditionColumnNeedsQuotationMark;
  }

  public String getConditionColumnValue() {
    return conditionColumnValue;
  }

  public ColumnAndValueInfoBean getConditionColumnInfo() {
    return conditionColumnInfo;
  }

  @Override
  public void afterReading() throws AppException {

  }
}
