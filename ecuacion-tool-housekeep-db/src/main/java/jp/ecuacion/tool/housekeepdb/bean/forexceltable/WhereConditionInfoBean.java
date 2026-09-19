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
import jp.ecuacion.lib.validation.constraints.PatternWithDescription;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.util.LangExcelUtil;
import jp.ecuacion.util.excel.table.bean.StringExcelTableBean;
import org.jspecify.annotations.Nullable;

/**
 * Stores where clause settings.
 */
@SuppressWarnings("NullAway.Init")
public class WhereConditionInfoBean extends StringExcelTableBean {
  @NotEmpty
  private String taskId;
  // conditionColumn is embedded as-is (unquoted, unescaped) into generated SQL by
  // ColumnAndValueInfoBean.getSqlFragment(), so only unquoted SQL identifier characters are
  // allowed.
  @NotEmpty
  @PatternWithDescription(regexp = "^[A-Za-z_][A-Za-z0-9_]*$",
      description = "letters, digits and underscores only, and must not start with a digit")
  private String conditionColumn;
  @NotEmpty
  @Pattern(regexp = "^(\\(none\\)|quotes\\(\\'\\)$)")
  private String conditionColumnNeedsQuotationMark;
  @NotEmpty
  private String conditionColumnValue;
  private ColumnAndValueInfoBean conditionColumnInfo;

  public static final String[] HEADER_LABEL_KEYS =
      LangExcelUtil.SearchConditionSettings.HEADER_LABELS;

  @Override
  protected @Nullable String[] getFieldNameArray() {
    return new String[] {"taskId", "conditionColumn", "conditionColumnNeedsQuotationMark",
        "conditionColumnValue"};
  }

  /**
   * Constructs a new instance.
   *
   * @param colList colList
   */
  @SuppressWarnings("null")
  public WhereConditionInfoBean(List<String> colList) {
    super(colList);
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
  public void afterReading() {
    conditionColumnInfo = new ColumnAndValueInfoBean(conditionColumn,
        conditionColumnNeedsQuotationMark, conditionColumnValue);
  }
}
