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
import jp.ecuacion.tool.housekeepdb.enums.TimestampKindEnum;
import jp.ecuacion.tool.housekeepdb.lang.LangExcel;
import jp.ecuacion.util.poi.excel.table.bean.StringExcelTableBean;
import org.apache.commons.lang3.StringUtils;

/**
 * Stores housekeeping settings.
 */
// softDeleteColumn required for soft delete
@ConditionalNotEmpty(field = "softDeleteColumn", conditionField = "isSoftDeleteInternalValue",
    conditionValue = HousekeepInfoBean.DELETE_KIND_SOFT)
// timestampColumn, timestampColumnKind and deleteTargetInDays must be all empty or all not empty
@ConditionalEmpty(field = {"timestampColumnKind", "deleteTargetInDays"},
    conditionField = "timestampColumn", conditionValueIsEmpty = true, notEmptyForOtherValues = true)
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
public class HousekeepInfoBean extends StringExcelTableBean {

  public static final String DELETE_KIND_SOFT = "SOFT_DELETE";
  public static final String DELETE_KIND_HARD = "HARD_DELETE";

  @NotEmpty
  private String taskId;
  @NotEmpty
  private String dbConnectionInfoId;
  @NotEmpty
  private String isSoftDelete;
  @NotEmpty
  @Pattern(regexp = "^" + DELETE_KIND_HARD + "|" + DELETE_KIND_SOFT + "$")
  private String isSoftDeleteInternalValue;
  @NotEmpty
  private String table;
  @NotEmpty
  private String idColumn;
  @NotEmpty
  @Pattern(regexp = "^(\\(none\\)|quotes\\(\\'\\)$)")
  private String idColumnNeedsQuotationMark;
  private String timestampColumn;
  private String timestampColumnKind;
  private String deleteTargetInDays;
  private String softDeleteColumn;
  private String softDeleteUpdateTimestampColumn;
  private String softDeleteUpdateUserIdColumn;
  @Pattern(regexp = "^(\\(none\\)|quotes\\(\\'\\)$)")
  private String softDeleteUpdateUserIdColumnNeedsQuotationMark;
  private String softDeleteUpdateUserIdColumnValue;

  private ColumnInfoBean idColumnInfo;
  private ColumnInfoBean softDeleteColumnInfo;
  private ColumnInfoBean softDeleteUpdateTimestampColumnInfo;
  private ColumnAndValueInfoBean softDeleteUpdateUserIdColumnAndValueInfo;

  private DbConnectionInfoBean dbConnectionInfo;
  private List<WhereConditionInfoBean> whereConditionInfoList;

  private List<RelatedTableInfoBean> relatedRecordTableInfoList;

  public static final String[] HEADER_LABEL_KEYS = LangExcel.HousekeepDbSettings.HEADER_LABELS;

  @Override
  protected @Nonnull String[] getFieldNameArray() {
    return new String[] {"taskId", "dbConnectionInfoId", "isSoftDelete",
        "isSoftDeleteInternalValue", "table", "idColumn", "idColumnNeedsQuotationMark",
        "timestampColumn", "timestampColumnKind", "deleteTargetInDays", "softDeleteColumn",
        "softDeleteUpdateTimestampColumn", "softDeleteUpdateUserIdColumn",
        "softDeleteUpdateUserIdColumnNeedsQuotationMark", "softDeleteUpdateUserIdColumnValue"};
  }

  /**
   * Constructs a new instance.
   * 
   * @param colList colList
   * @throws BizLogicAppException BizLogicAppException
   */
  public HousekeepInfoBean(List<String> colList) throws BizLogicAppException {
    super(colList);
  }

  public String getTaskId() {
    return taskId;
  }

  /**
   * Returns if the housekeeping task is soft delete or hard delete.
   * 
   * @return boolean, true if soft delete.
   */
  public boolean isSoftDelete() {
    if (isSoftDeleteInternalValue.equals(DELETE_KIND_HARD)) {
      return false;

    } else if (isSoftDeleteInternalValue.equals(DELETE_KIND_SOFT)) {
      return true;

    } else {
      throw new RuntimeException("Not an assumed value: " + isSoftDelete);
    }
  }

  public String getDbConnectionInfoId() {
    return dbConnectionInfoId;
  }

  public String getTable() {
    return table;
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

  /**
   * Returns the datatype of timestamp column.
   * 
   * @return TimestampKindEnum
   */
  public TimestampKindEnum getTimestampColumnKind() {
    if (TimestampKindEnum.localDateTime.toString().equalsIgnoreCase(timestampColumnKind)) {
      return TimestampKindEnum.localDateTime;

    } else if (TimestampKindEnum.offsetDateTime.toString().equalsIgnoreCase(timestampColumnKind)) {
      return TimestampKindEnum.offsetDateTime;

    } else {
      throw new RuntimeException(
          "timestampColumnKindString is not an assumed value: " + timestampColumnKind);
    }
  }

  public String getTimestampColumn() {
    return timestampColumn;
  }

  public int getDeleteTargetInDays() {
    return Integer.parseInt(deleteTargetInDays);
  }

  /**
   * Returns {@code true} when a timestamp column is set.
   */
  public boolean timestampColumnDefines() {
    return !StringUtils.isEmpty(timestampColumn);
  }

  public ColumnInfoBean getIdColumnInfo() {
    return idColumnInfo;
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

  public DbConnectionInfoBean getDbConnectionInfo() {
    return dbConnectionInfo;
  }

  public void setDbConnectionInfo(DbConnectionInfoBean dbConnectionInfo) {
    this.dbConnectionInfo = dbConnectionInfo;
  }

  public List<WhereConditionInfoBean> getWhereConditionInfoList() {
    return whereConditionInfoList;
  }

  public void setWhereConditionInfoList(List<WhereConditionInfoBean> columnValueConditionInfoList) {
    this.whereConditionInfoList = columnValueConditionInfoList;
  }

  public List<RelatedTableInfoBean> getRelatedRecordTableInfoList() {
    return relatedRecordTableInfoList;
  }

  public void setRelatedRecordTableInfoList(List<RelatedTableInfoBean> relatedRecordTableInfoList) {
    this.relatedRecordTableInfoList = relatedRecordTableInfoList;
  }

  @Override
  public void afterReading() throws AppException {
    constructColumnInfo();
  }

  private void constructColumnInfo() throws BizLogicAppException {

    idColumnInfo = new ColumnInfoBean(idColumn, idColumnNeedsQuotationMark);

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
}
