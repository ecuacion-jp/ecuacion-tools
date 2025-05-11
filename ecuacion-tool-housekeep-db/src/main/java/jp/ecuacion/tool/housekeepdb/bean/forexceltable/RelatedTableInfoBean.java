package jp.ecuacion.tool.housekeepdb.bean.forexceltable;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.jakartavalidation.validator.ConditionalEmpty;
import jp.ecuacion.lib.core.jakartavalidation.validator.ConditionalNotEmpty;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnInfoBean;
import jp.ecuacion.tool.housekeepdb.lang.LangExcel;
import jp.ecuacion.util.poi.excel.table.bean.StringExcelTableBean;
import org.apache.commons.lang3.StringUtils;

/**
 * Stores related tables settings.
 */
//softDeleteColumn required for soft delete
@ConditionalNotEmpty(field = "softDeleteColumn", conditionField = "isSoftDeleteInternalValue",
    conditionValue = HousekeepInfoBean.DELETE_KIND_SOFT)
// softDeleteUpdateUserIdColumn, softDeleteUpdateUserIdColumnNeedsQuotationMark and
// softDeleteUpdateUserIdColumnAndValue must be all empty or all not empty.
@ConditionalEmpty(
    field = {"softDeleteUpdateUserIdColumnNeedsQuotationMark", "softDeleteUpdateUserIdColumnValue"},
    conditionField = "softDeleteUpdateUserIdColumn", conditionValueIsEmpty = true,
    notEmptyForOtherValues = true)
// fields related to soft delete must be null when isSoftDelete is hard
// ("softDeleteUpdateUserIdColumnNeedsQuotationMark", "softDeleteUpdateUserIdColumnValue" are
// covered with the next @ConditionalEmpty)
@ConditionalEmpty(field = {"softDeleteUpdateTimestampColumn", "softDeleteUpdateUserIdColumn"},
    conditionField = "isSoftDeleteInternalValue",
    conditionValue = HousekeepInfoBean.DELETE_KIND_HARD)
// softDeleteUpdateUserIdColumn, softDeleteUpdateUserIdColumnNeedsQuotationMark and
// softDeleteUpdateUserIdColumnAndValue must be all empty or all not empty
@ConditionalEmpty(
    field = {"softDeleteUpdateUserIdColumnNeedsQuotationMark", "softDeleteUpdateUserIdColumnValue"},
    conditionField = "softDeleteUpdateUserIdColumn", conditionValueIsEmpty = true,
    notEmptyForOtherValues = true)
public class RelatedTableInfoBean extends StringExcelTableBean {

  public static final String RELATED_TABLE_PROCESS_PATTERN_DELETE = "DELETE";
  public static final String RELATED_TABLE_PROCESS_PATTERN_CHECK_AND_SKIP_DELETE =
      "CHECK_AND_SKIP_DELETE";

  public static final String EMPTY = "";

  @NotEmpty
  private String taskId;
  @NotEmpty
  @Pattern(regexp = "^" + HousekeepInfoBean.DELETE_KIND_HARD + "|"
      + HousekeepInfoBean.DELETE_KIND_SOFT + "$")
  private String isSoftDeleteInternalValue;
  @NotEmpty
  private String relatedTableProcessPattern;
  @NotEmpty
  @Pattern(regexp = "^" + RELATED_TABLE_PROCESS_PATTERN_DELETE + "|"
      + RELATED_TABLE_PROCESS_PATTERN_CHECK_AND_SKIP_DELETE + "$")
  private String relatedTableProcessPatternInternalValue;
  @NotEmpty
  private String targetTableColumn;
  @NotEmpty
  private String relatedTable;
  @NotEmpty
  private String relatedTableIdColumn;
  @NotEmpty
  @Pattern(regexp = "^(\\(none\\)|quotes\\(\\'\\)$)")
  private String relatedTableIdColumnNeedsQuotationMark;
  private String softDeleteColumn;
  private String softDeleteUpdateTimestampColumn;
  private String softDeleteUpdateUserIdColumn;
  @Pattern(regexp = "^(\\(none\\)|quotes\\(\\'\\)$)")
  private String softDeleteUpdateUserIdColumnNeedsQuotationMark;
  private String softDeleteUpdateUserIdColumnValue;

  private ColumnInfoBean relatedTableIdColumnInfo;
  private ColumnInfoBean softDeleteColumnInfo;
  private ColumnInfoBean softDeleteUpdateTimestampColumnInfo;
  private ColumnAndValueInfoBean softDeleteUpdateUserIdColumnAndValueInfo;

  public static final String[] HEADER_LABEL_KEYS = LangExcel.RelatedTableSettings.HEADER_LABELS;

  @Override
  protected @Nonnull String[] getFieldNameArray() {
    return new String[] {"taskId", "isSoftDeleteInternalValue", "relatedTableProcessPattern",
        "relatedTableProcessPatternInternalValue", "targetTableColumn", "relatedTable",
        "relatedTableIdColumn", "relatedTableIdColumnNeedsQuotationMark", "softDeleteColumn",
        "softDeleteUpdateTimestampColumn", "softDeleteUpdateUserIdColumn",
        "softDeleteUpdateUserIdColumnNeedsQuotationMark", "softDeleteUpdateUserIdColumnValue"};
  }

  /**
   * Constructs a new instance.
   * 
   * @param colList colList
   * @throws BizLogicAppException BizLogicAppException
   */
  public RelatedTableInfoBean(List<String> colList) throws BizLogicAppException {
    super(colList);
  }

  public String getTaskId() {
    return taskId;
  }

  public RelatedTableProcessPatternEnum getRelatedTableProcessPattern() {
    return relatedTableProcessPatternInternalValue.equals(RELATED_TABLE_PROCESS_PATTERN_DELETE)
        ? RelatedTableProcessPatternEnum.deleteRelatedTableRecord
        : RelatedTableProcessPatternEnum.skipTargetTableRecordDeletion;
  }

  public String getRelatedTableProcessPatternStringKey() {
    return relatedTableProcessPatternInternalValue.equals(RELATED_TABLE_PROCESS_PATTERN_DELETE)
        ? "EXCEL_VALUE_RELATED_TABLE_PROCESS_PATTERN_DELETE"
        : "EXCEL_VALUE_RELATED_TABLE_PROCESS_PATTERN_CHECK_AND_SKIP_DELETE";
  }

  public String getTargetTableColumn() {
    return targetTableColumn;
  }

  public void setTargetTableColumn(String targetTableColumn) {
    this.targetTableColumn = targetTableColumn;
  }

  public String getRelatedTable() {
    return relatedTable;
  }

  public String getSoftDeleteColumn() {
    return softDeleteColumn;
  }

  public String getSoftDeleteUpdateTimestampColumn() {
    return softDeleteUpdateTimestampColumn;
  }

  public String getSoftDeleteUpdateUserIdColumn() {
    return softDeleteUpdateUserIdColumn;
  }

  public String getSoftDeleteUpdateUserIdColumnNeedsQuotationMark() {
    return softDeleteUpdateUserIdColumnNeedsQuotationMark;
  }

  public String getSoftDeleteUpdateUserIdColumnValue() {
    return softDeleteUpdateUserIdColumnValue;
  }

  public ColumnInfoBean getRelatedTableIdColumnInfo() {
    return relatedTableIdColumnInfo;
  }

  public ColumnInfoBean getSoftDeleteColumnInfo() {
    return softDeleteColumnInfo;
  }

  public ColumnInfoBean getSoftDeleteUpdateTimestampColumnInfo() {
    return softDeleteUpdateTimestampColumnInfo;
  }

  public ColumnAndValueInfoBean getSoftDeleteUpdateUserIdColumnAndValueInfo() {
    return softDeleteUpdateUserIdColumnAndValueInfo;
  }

  public void setRelatedTableIdColumnInfo(ColumnInfoBean relatedTableIdColumnInfo) {
    this.relatedTableIdColumnInfo = relatedTableIdColumnInfo;
  }

  @Override
  public void afterReading() throws AppException {
    constructColumnInfo();
  }

  private void constructColumnInfo() throws BizLogicAppException {
    relatedTableIdColumnInfo =
        new ColumnInfoBean(relatedTableIdColumn, relatedTableIdColumnNeedsQuotationMark);

    if (StringUtils.isNotEmpty(softDeleteColumn)) {
      softDeleteColumnInfo = new ColumnInfoBean(softDeleteColumn, false);
    }

    if (StringUtils.isNotEmpty(softDeleteUpdateTimestampColumn)) {
      softDeleteUpdateTimestampColumnInfo =
          new ColumnInfoBean(softDeleteUpdateTimestampColumn, false);
    }

    if (StringUtils.isNotEmpty(softDeleteUpdateUserIdColumn)) {
      softDeleteUpdateUserIdColumnAndValueInfo =
          new ColumnAndValueInfoBean(softDeleteUpdateUserIdColumn,
              softDeleteUpdateUserIdColumnNeedsQuotationMark, softDeleteUpdateUserIdColumnValue);
    }
  }

  /**
   * Stores related table process pattern.
   */
  public static enum RelatedTableProcessPatternEnum {
    deleteRelatedTableRecord, skipTargetTableRecordDeletion;
  }
}
