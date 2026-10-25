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
import jp.ecuacion.lib.validation.constraints.PatternWithDescription;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueStringBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import jp.ecuacion.tool.housekeepdb.util.LangExcelUtil;
import jp.ecuacion.util.excel.table.bean.StringExcelTableBean;
import org.jspecify.annotations.Nullable;

/**
 * Stores one "Update Column Settings" row: a column an
 * {@link HousekeepInfoBean#PROCESS_KIND_UPDATE} task rewrites, and the value to rewrite it with.
 */
// needsQuotationMark and value are only for a fixed value: they must be empty for the current
// timestamp / null, and set for a fixed value.
@EmptyWhen(propertyPath = {"needsQuotationMark", "value"},
    conditionPropertyPath = "valueTypeInternalValue",
    conditionValueString = {UpdateColumnInfoBean.VALUE_TYPE_CURRENT_TIMESTAMP,
        UpdateColumnInfoBean.VALUE_TYPE_NULL},
    notEmptyWhenConditionNotSatisfied = true)
@SuppressWarnings("NullAway.Init")
public class UpdateColumnInfoBean extends StringExcelTableBean {

  public static final String VALUE_TYPE_FIXED_VALUE = "FIXED_VALUE";
  public static final String VALUE_TYPE_CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";
  public static final String VALUE_TYPE_NULL = "NULL";

  @NotEmpty
  private String taskId;
  // updateColumn is embedded as-is (unquoted, unescaped) into generated SQL, so only unquoted SQL
  // identifier characters are allowed.
  @NotEmpty
  @PatternWithDescription(regexp = "^[A-Za-z_][A-Za-z0-9_]*$",
      description = "letters, digits and underscores only, and must not start with a digit")
  private String updateColumn;
  @NotEmpty
  @SuppressWarnings("UnusedVariable")
  private String valueType;
  @NotEmpty
  @Pattern(regexp = "^(" + VALUE_TYPE_FIXED_VALUE + "|" + VALUE_TYPE_CURRENT_TIMESTAMP + "|"
      + VALUE_TYPE_NULL + ")$")
  private String valueTypeInternalValue;
  @Pattern(regexp = "^(\\(none\\)|quotes\\(\\'\\)$)")
  private String needsQuotationMark;
  private String value;

  public static final String[] HEADER_LABEL_KEYS =
      LangExcelUtil.UpdateColumnSettings.HEADER_LABELS;

  @Override
  protected @Nullable String[] getFieldNameArray() {
    return new String[] {"taskId", "updateColumn", "valueType", "valueTypeInternalValue",
        "needsQuotationMark", "value"};
  }

  /**
   * Constructs a new instance.
   *
   * @param colList colList
   */
  @SuppressWarnings("null")
  public UpdateColumnInfoBean(List<String> colList) {
    super(colList);
  }

  public String getTaskId() {
    return taskId;
  }

  public String getUpdateColumn() {
    return updateColumn;
  }

  /**
   * Builds one item of the SET clause rewriting {@link #getUpdateColumn()}.
   *
   * <p>A fixed value is typed into the excel config, so it's embedded as SQL literal text (see
   *     {@link ColumnAndValueInfoBean}'s class Javadoc), while the current time is computed by
   *     this tool and is bound as a JDBC parameter, the same way the soft-delete "update
   *     timestamp" column is.</p>
   *
   * @param protocol database kind like 'postgresql', used to type the current time - see
   *     {@link jp.ecuacion.tool.housekeepdb.util.SqlUtil#getTimestampNowValue}
   * @return the SET clause item
   */
  public SqlConditionInterface getSetCondition(String protocol) {
    if (valueTypeInternalValue.equals(VALUE_TYPE_CURRENT_TIMESTAMP)) {
      return new ColumnInfoBean(updateColumn, false).getBoundTimestampNowCondition(protocol);

    } else if (valueTypeInternalValue.equals(VALUE_TYPE_NULL)) {
      return new ColumnAndValueStringBean(updateColumn + " = null");

    } else {
      return new ColumnAndValueInfoBean(updateColumn, needsQuotationMark, value);
    }
  }
}
