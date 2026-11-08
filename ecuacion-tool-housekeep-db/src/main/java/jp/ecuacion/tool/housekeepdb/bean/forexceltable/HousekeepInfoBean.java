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
import jp.ecuacion.lib.validation.constraints.enums.ConditionValueState;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnInfoBean;
import jp.ecuacion.tool.housekeepdb.util.LangExcelUtil;
import jp.ecuacion.util.excel.table.bean.StringExcelTableBean;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * Stores housekeeping settings.
 */
// softDeleteColumn required for soft delete
@NotEmptyWhen(propertyPath = "softDeleteColumn",
    conditionPropertyPath = "processKindInternalValue",
    conditionValueString = HousekeepInfoBean.DELETE_KIND_SOFT)
// timestampColumn and deleteTargetInDays must be both empty or both not empty
@EmptyWhen(propertyPath = "deleteTargetInDays",
    conditionPropertyPath = "timestampColumn", conditionValueState = ConditionValueState.EMPTY,
    notEmptyWhenConditionNotSatisfied = true)
// fields related to soft delete must be null when processKind is hard delete, abnormal data check
// or update ("softDeleteUpdateUserIdColumnNeedsQuotationMark", "softDeleteUpdateUserIdColumnValue"
// are covered with the next @ConditionalEmpty)
@EmptyWhen(propertyPath = {"softDeleteUpdateTimestampColumn", "softDeleteUpdateUserIdColumn"},
    conditionPropertyPath = "processKindInternalValue",
    conditionValueString = {HousekeepInfoBean.DELETE_KIND_HARD,
        HousekeepInfoBean.PROCESS_KIND_ABNORMAL_DATA_CHECK, HousekeepInfoBean.PROCESS_KIND_UPDATE})
// an update task rewrites only the columns in "Update Column Settings", so the soft-delete column
// is not used either (to leave soft-deleted rows out, write it in "Search Condition Settings")
@EmptyWhen(propertyPath = "softDeleteColumn", conditionPropertyPath = "processKindInternalValue",
    conditionValueString = HousekeepInfoBean.PROCESS_KIND_UPDATE)
// softDeleteUpdateUserIdColumn, softDeleteUpdateUserIdColumnNeedsQuotationMark and
// softDeleteUpdateUserIdColumnAndValue must be all empty or all not empty
@EmptyWhen(
    propertyPath = {"softDeleteUpdateUserIdColumnNeedsQuotationMark",
        "softDeleteUpdateUserIdColumnValue"},
    conditionPropertyPath = "softDeleteUpdateUserIdColumn",
    conditionValueState = ConditionValueState.EMPTY, notEmptyWhenConditionNotSatisfied = true)
@SuppressWarnings("NullAway.Init")
public class HousekeepInfoBean extends StringExcelTableBean implements DeleteTargetInfo {

  public static final String DELETE_KIND_SOFT = "SOFT_DELETE";
  public static final String DELETE_KIND_HARD = "HARD_DELETE";
  /**
   * Doesn't delete anything, but reports the records matching the task's conditions as a
   * warning (log and email) since their existence means something has gone wrong.
   */
  public static final String PROCESS_KIND_ABNORMAL_DATA_CHECK = "ABNORMAL_DATA_CHECK";
  /**
   * Rewrites the columns listed in "Update Column Settings" of the records matching the task's
   * conditions, leaving the records themselves in place. Related-table records are hard-deleted
   * (never soft-deleted) - see {@link #isRelatedRecordSoftDelete()}.
   */
  public static final String PROCESS_KIND_UPDATE = "UPDATE";

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
  private String processKind;
  @NotEmpty
  @Pattern(regexp = "^(" + DELETE_KIND_HARD + "|" + DELETE_KIND_SOFT + "|"
      + PROCESS_KIND_ABNORMAL_DATA_CHECK + "|" + PROCESS_KIND_UPDATE + ")$")
  private String processKindInternalValue;
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

  private List<UpdateColumnInfoBean> updateColumnInfoList;

  public static final String[] HEADER_LABEL_KEYS = LangExcelUtil.HousekeepDbSettings.HEADER_LABELS;

  @Override
  protected @Nullable String[] getFieldNameArray() {
    return new String[] {"taskId", "dbConnectionInfoId", "processKind",
        "processKindInternalValue", "table", "idColumn", "idColumnNeedsQuotationMark",
        "timestampColumn", "deleteTargetInDays", "softDeleteColumn",
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
   * <p>Only meaningful for delete tasks - an abnormal data check task (see
   *     {@link #isAbnormalDataCheck()}) deletes nothing and an update task (see
   *     {@link #isUpdate()}) rewrites its records instead of deleting them, so it's an error to
   *     ask.</p>
   * 
   * @return boolean, true if soft delete.
   */
  public boolean isSoftDelete() {
    if (processKindInternalValue.equals(DELETE_KIND_HARD)) {
      return false;

    } else if (processKindInternalValue.equals(DELETE_KIND_SOFT)) {
      return true;

    } else {
      throw new RuntimeException("Not an assumed value: " + processKind);
    }
  }

  /**
   * Returns if the housekeeping task is an abnormal data check, which only reports the records
   * matching its conditions instead of deleting them.
   *
   * @return boolean, true if abnormal data check.
   */
  public boolean isAbnormalDataCheck() {
    return PROCESS_KIND_ABNORMAL_DATA_CHECK.equals(processKindInternalValue);
  }

  /**
   * Returns if the housekeeping task is an update, which rewrites the columns listed in "Update
   * Column Settings" instead of deleting the records.
   *
   * @return boolean, true if update.
   */
  public boolean isUpdate() {
    return PROCESS_KIND_UPDATE.equals(processKindInternalValue);
  }

  /**
   * Returns if the related-table records of this task are soft-deleted (as opposed to
   * hard-deleted): the same as the task itself for a delete task, and always hard-deleted for an
   * update task.
   *
   * @return boolean, true if soft delete.
   */
  public boolean isRelatedRecordSoftDelete() {
    return !isUpdate() && isSoftDelete();
  }

  /**
   * Returns the internal value of the process type.
   *
   * @return {@link #DELETE_KIND_SOFT}, {@link #DELETE_KIND_HARD},
   *     {@link #PROCESS_KIND_ABNORMAL_DATA_CHECK} or {@link #PROCESS_KIND_UPDATE}
   */
  public String getProcessKindInternalValue() {
    return processKindInternalValue;
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

  public List<UpdateColumnInfoBean> getUpdateColumnInfoList() {
    return updateColumnInfoList;
  }

  public void setUpdateColumnInfoList(List<UpdateColumnInfoBean> updateColumnInfoList) {
    this.updateColumnInfoList = updateColumnInfoList;
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
