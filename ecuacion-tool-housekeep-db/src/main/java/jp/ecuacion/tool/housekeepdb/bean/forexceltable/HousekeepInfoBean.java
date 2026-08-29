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
package jp.ecuacion.tool.housekeepdb.bean.forexceltable;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import jp.ecuacion.lib.validation.constraints.EmptyWhen;
import jp.ecuacion.lib.validation.constraints.NotEmptyWhen;
import jp.ecuacion.lib.validation.constraints.PatternWithDescription;
import jp.ecuacion.lib.validation.constraints.enums.ConditionValue;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnInfoBean;
import jp.ecuacion.tool.housekeepdb.enums.TimestampKindEnum;
import jp.ecuacion.tool.housekeepdb.util.LangExcelUtil;
import jp.ecuacion.util.excel.table.bean.StringExcelTableBean;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * Stores housekeeping settings.
 */
// softDeleteColumn required for soft delete
@NotEmptyWhen(propertyPath = "softDeleteColumn",
    conditionPropertyPath = "isSoftDeleteInternalValue", conditionValue = ConditionValue.STRING,
    conditionValueString = HousekeepInfoBean.DELETE_KIND_SOFT)
// timestampColumn, timestampColumnKind and deleteTargetInDays must be all empty or all not empty
@EmptyWhen(propertyPath = {"timestampColumnKind", "deleteTargetInDays"},
    conditionPropertyPath = "timestampColumn", conditionValue = ConditionValue.EMPTY,
    notEmptyWhenConditionNotSatisfied = true)
// fields related to soft delete must be null when isSoftDelete is hard
// ("softDeleteUpdateUserIdColumnNeedsQuotationMark", "softDeleteUpdateUserIdColumnValue" are
// covered with the next @ConditionalEmpty)
@EmptyWhen(propertyPath = {"softDeleteUpdateTimestampColumn", "softDeleteUpdateUserIdColumn"},
    conditionPropertyPath = "isSoftDeleteInternalValue", conditionValue = ConditionValue.STRING,
    conditionValueString = HousekeepInfoBean.DELETE_KIND_HARD)
// softDeleteUpdateUserIdColumn, softDeleteUpdateUserIdColumnNeedsQuotationMark and
// softDeleteUpdateUserIdColumnAndValue must be all empty or all not empty
@EmptyWhen(
    propertyPath = {"softDeleteUpdateUserIdColumnNeedsQuotationMark",
        "softDeleteUpdateUserIdColumnValue"},
    conditionPropertyPath = "softDeleteUpdateUserIdColumn", conditionValue = ConditionValue.EMPTY,
    notEmptyWhenConditionNotSatisfied = true)
@SuppressWarnings("NullAway.Init")
public class HousekeepInfoBean extends StringExcelTableBean implements DeleteTargetInfo {

  public static final String DELETE_KIND_SOFT = "SOFT_DELETE";
  public static final String DELETE_KIND_HARD = "HARD_DELETE";

  // Columns below are embedded as-is (unquoted, unescaped) into generated SQL by
  // HousekeepDbTasklet / ColumnInfoBean, so only unquoted SQL identifier characters are allowed.
  private static final String COLUMN_NAME_REGEXP = "^[A-Za-z_][A-Za-z0-9_]*$";
  private static final String COLUMN_NAME_DESCRIPTION =
      "letters, digits and underscores only, and must not start with a digit";

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
  @PatternWithDescription(regexp = COLUMN_NAME_REGEXP, description = COLUMN_NAME_DESCRIPTION)
  private String table;
  @NotEmpty
  @PatternWithDescription(regexp = COLUMN_NAME_REGEXP, description = COLUMN_NAME_DESCRIPTION)
  private String idColumn;
  @NotEmpty
  @Pattern(regexp = "^(\\(none\\)|quotes\\(\\'\\)$)")
  private String idColumnNeedsQuotationMark;
  @PatternWithDescription(regexp = COLUMN_NAME_REGEXP, description = COLUMN_NAME_DESCRIPTION)
  private String timestampColumn;
  @PatternWithDescription(regexp = "(?i)^(localDateTime|offsetDateTime)$",
      description = "\"localDateTime\" or \"offsetDateTime\" (case-insensitive)")
  private String timestampColumnKind;
  @PatternWithDescription(regexp = "^[0-9]+$", description = "digits only")
  private String deleteTargetInDays;
  @PatternWithDescription(regexp = COLUMN_NAME_REGEXP, description = COLUMN_NAME_DESCRIPTION)
  private String softDeleteColumn;
  @PatternWithDescription(regexp = COLUMN_NAME_REGEXP, description = COLUMN_NAME_DESCRIPTION)
  private String softDeleteUpdateTimestampColumn;
  @PatternWithDescription(regexp = COLUMN_NAME_REGEXP, description = COLUMN_NAME_DESCRIPTION)
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

  public static final String[] HEADER_LABEL_KEYS = LangExcelUtil.HousekeepDbSettings.HEADER_LABELS;

  @Override
  protected @Nullable String[] getFieldNameArray() {
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
   */
  @SuppressWarnings("null")
  public HousekeepInfoBean(List<String> colList) {
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

  /**
   * Returns {@link #DELETE_KIND_SOFT} or {@link #DELETE_KIND_HARD}, for copying onto linked
   * {@link RelatedTableInfoBean} rows after merging - see that class's Javadoc.
   *
   * @return {@link #DELETE_KIND_SOFT} or {@link #DELETE_KIND_HARD}
   */
  public String getIsSoftDeleteInternalValue() {
    return isSoftDeleteInternalValue;
  }

  public String getDbConnectionInfoId() {
    return dbConnectionInfoId;
  }

  public String getTable() {
    return table;
  }

  @Override
  public String getTargetTable() {
    return getTable();
  }

  @Override
  public String getSoftDeleteColumn() {
    return softDeleteColumn;
  }

  @Override
  public String getSoftDeleteUpdateTimestampColumn() {
    return softDeleteUpdateTimestampColumn;
  }

  @Override
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

  @Override
  public ColumnInfoBean getDeleteKeyColumnInfo() {
    return getIdColumnInfo();
  }

  @Override
  public ColumnInfoBean getSoftDeleteColumnInfo() {
    return softDeleteColumnInfo;
  }

  @Override
  public ColumnInfoBean getSoftDeleteUpdateTimestampColumnInfo() {
    return softDeleteUpdateTimestampColumnInfo;
  }

  @Override
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
  public void afterReading() {
    constructColumnInfo();
  }

  private void constructColumnInfo() {

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
