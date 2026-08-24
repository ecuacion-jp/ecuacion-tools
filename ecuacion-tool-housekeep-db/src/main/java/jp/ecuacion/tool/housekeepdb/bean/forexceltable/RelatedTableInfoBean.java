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
import jp.ecuacion.lib.validation.constraints.enums.ConditionValue;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnInfoBean;
import jp.ecuacion.tool.housekeepdb.tasklet.HousekeepDbTasklet;
import jp.ecuacion.tool.housekeepdb.tasklet.HousekeepDbTasklet.AfterMergeValidation;
import jp.ecuacion.tool.housekeepdb.util.LangExcelUtil;
import jp.ecuacion.util.excel.table.bean.StringExcelTableBean;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * Stores related tables settings.
 *
 * <p>{@code isSoftDeleteInternalValue} is no longer an Excel column here: it used to be a
 *     VLOOKUP-hidden column duplicating the value from the Housekeep DB Settings sheet, but
 *     {@link HousekeepDbTasklet} already links each row to its {@link HousekeepInfoBean} by task
 *     ID, so it copies that value onto this field itself after linking (see
 *     {@code getHousekeepInfoList()}), before running {@link AfterMergeValidation}-grouped
 *     validation. The 2 constraints below that key off it are therefore in that group: they'd
 *     otherwise fail every time, since this field is never populated at Excel-read time (unlike
 *     the other 2 {@code @EmptyWhen} constraints, which key off
 *     {@code softDeleteUpdateUserIdColumn} - a field of this same bean available from the start -
 *     so they stay in the default group and run immediately, as before).</p>
 */
// softDeleteColumn required for soft delete
@NotEmptyWhen(propertyPath = "softDeleteColumn",
    conditionPropertyPath = "isSoftDeleteInternalValue", conditionValue = ConditionValue.STRING,
    conditionValueString = HousekeepInfoBean.DELETE_KIND_SOFT,
    groups = AfterMergeValidation.class)
// softDeleteUpdateUserIdColumn, softDeleteUpdateUserIdColumnNeedsQuotationMark and
// softDeleteUpdateUserIdColumnAndValue must be all empty or all not empty.
@EmptyWhen(
    propertyPath = {"softDeleteUpdateUserIdColumnNeedsQuotationMark",
        "softDeleteUpdateUserIdColumnValue"},
    conditionPropertyPath = "softDeleteUpdateUserIdColumn",
    conditionValue = ConditionValue.EMPTY,
    notEmptyWhenConditionNotSatisfied = true)
// fields related to soft delete must be null when isSoftDelete is hard
// ("softDeleteUpdateUserIdColumnNeedsQuotationMark", "softDeleteUpdateUserIdColumnValue" are
// covered with the next @ConditionalEmpty)
@EmptyWhen(
    propertyPath = {"softDeleteUpdateTimestampColumn", "softDeleteUpdateUserIdColumn"},
    conditionPropertyPath = "isSoftDeleteInternalValue",
    conditionValue = ConditionValue.STRING,
    conditionValueString = HousekeepInfoBean.DELETE_KIND_HARD,
    groups = AfterMergeValidation.class)
// softDeleteUpdateUserIdColumn, softDeleteUpdateUserIdColumnNeedsQuotationMark and
// softDeleteUpdateUserIdColumnAndValue must be all empty or all not empty
@EmptyWhen(
    propertyPath = {"softDeleteUpdateUserIdColumnNeedsQuotationMark",
        "softDeleteUpdateUserIdColumnValue"},
    conditionPropertyPath = "softDeleteUpdateUserIdColumn",
    conditionValue = ConditionValue.EMPTY,
    notEmptyWhenConditionNotSatisfied = true)
@SuppressWarnings("NullAway.Init")
public class RelatedTableInfoBean extends StringExcelTableBean {

  public static final String RELATED_TABLE_PROCESS_PATTERN_DELETE = "DELETE";
  public static final String RELATED_TABLE_PROCESS_PATTERN_CHECK_AND_SKIP_DELETE =
      "CHECK_AND_SKIP_DELETE";

  public static final String EMPTY = "";

  @NotEmpty
  private String taskId;
  @NotEmpty(groups = AfterMergeValidation.class)
  @Pattern(regexp = "^" + HousekeepInfoBean.DELETE_KIND_HARD + "|"
      + HousekeepInfoBean.DELETE_KIND_SOFT + "$", groups = AfterMergeValidation.class)
  @SuppressWarnings("UnusedVariable")
  private String isSoftDeleteInternalValue;
  @NotEmpty
  @SuppressWarnings("UnusedVariable")
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

  public static final String[] HEADER_LABEL_KEYS = LangExcelUtil.RelatedTableSettings.HEADER_LABELS;

  @Override
  protected @Nullable String[] getFieldNameArray() {
    return new String[] {"taskId", "relatedTableProcessPattern",
        "relatedTableProcessPatternInternalValue", "targetTableColumn", "relatedTable",
        "relatedTableIdColumn", "relatedTableIdColumnNeedsQuotationMark", "softDeleteColumn",
        "softDeleteUpdateTimestampColumn", "softDeleteUpdateUserIdColumn",
        "softDeleteUpdateUserIdColumnNeedsQuotationMark", "softDeleteUpdateUserIdColumnValue"};
  }

  /**
   * Constructs a new instance.
   *
   * @param colList colList
   */
  @SuppressWarnings("null")
  public RelatedTableInfoBean(List<String> colList) {
    super(colList);
  }

  public String getTaskId() {
    return taskId;
  }

  /**
   * Sets the soft/hard delete kind of the task this related-table row belongs to, copied from
   * the linked {@link HousekeepInfoBean} after merging - see the class Javadoc.
   *
   * @param isSoftDeleteInternalValue {@link HousekeepInfoBean#DELETE_KIND_SOFT} or
   *     {@link HousekeepInfoBean#DELETE_KIND_HARD}
   */
  public void setIsSoftDeleteInternalValue(String isSoftDeleteInternalValue) {
    this.isSoftDeleteInternalValue = isSoftDeleteInternalValue;
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
  public void afterReading() {
    constructColumnInfo();
  }

  private void constructColumnInfo() {
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
