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
package jp.ecuacion.tool.housekeepfiles.dto.record;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import jp.ecuacion.lib.core.util.EmbeddedVariableUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.validation.constraints.BooleanString;
import jp.ecuacion.lib.validation.constraints.EnumElement;
import jp.ecuacion.lib.validation.constraints.IntegerString;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTask;
import jp.ecuacion.tool.housekeepfiles.enums.IncidentTreatedAsEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.util.excel.table.bean.StringExcelTableBean;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * Stores task info.
 */
@SuppressWarnings("NullAway.Init")
public class HousekeepFilesTaskRecord extends StringExcelTableBean {

  @NotEmpty
  @Size(min = 1, max = 10)
  @Pattern(regexp = "^[a-zA-Z0-9 -/:-@\\[-\\`\\{-\\~]*$")
  @Pattern(regexp = "^[^!\"#\\$%&'\\(\\)=\\^~\\\\\\|`\\[\\{;\\+:\\\\*\\]\\},<>/\\?]*$")
  private String taskId;

  @NotEmpty
  @Size(min = 1, max = 40)
  @Pattern(regexp = "^[^!\"#\\$%&'\\(\\)=\\^~\\\\\\|`\\[\\{;\\+:\\\\*\\]\\},<>/\\?]*$")
  private String taskName;

  @NotEmpty
  @EnumElement(enumClass = TaskPtnEnum.class)
  public String taskPtnEnumName;

  @Size(min = 1, max = 40)
  @Pattern(regexp = "^[^!\"#\\$%&'\\(\\)=\\^~\\\\\\|`\\[\\{;\\+:\\\\*\\]\\},<>/\\?]*$")
  private String remoteServer;

  @BooleanString
  public String isSrcPathDirEnumName;

  @Size(min = 1, max = 300)
  private String srcPath;

  @IntegerString
  @DecimalMin(value = "0")
  @DecimalMax(value = "1000")
  private String value;

  @EnumElement(enumClass = IncidentTreatedAsEnum.class)
  public String actionForNoSrcPathEnumName;

  @Size(min = 1, max = 300)
  @Pattern(regexp = "^[^*?]*$")
  private String destPath;

  @BooleanString
  public String isDestPathDirEnumName;

  @BooleanString
  public String doesOverwriteDestPathEnumName;

  @EnumElement(enumClass = IncidentTreatedAsEnum.class)
  public String actionForDestFileExistsEnumName;

  // Fields not in the Excel sheet.

  private @Nullable String envVarExpandedSrcPath;

  private @Nullable String envVarExpandedDestPath;

  private Function<String, String> envVarValueGetter;

  // Holds the task object.
  public AbstractTask task;

  @Override
  protected @Nullable String[] getFieldNameArray() {
    return new String[] {"taskId", "taskName", null, "taskPtnEnumName", "remoteServer", "srcPath",
        "isSrcPathDirEnumName", "value", "actionForNoSrcPathEnumName", "destPath",
        "isDestPathDirEnumName", "doesOverwriteDestPathEnumName",
        "actionForDestFileExistsEnumName"};
  }

  /**
   * only for unit test.
   */
  @SuppressWarnings("null")
  public HousekeepFilesTaskRecord(@Nullable String taskId, @Nullable String taskName,
      @Nullable String taskPtnEnumName, @Nullable String remoteServer, @Nullable String pathFrom,
      @Nullable String isSrcPathDirEnumName, @Nullable String value,
      @Nullable String actionForNoSrcPathEnumName, @Nullable String pathTo,
      @Nullable String isDestPathDirEnumName, @Nullable String doesOverwriteDestPathEnumName,
      @Nullable String actionForDestFileExistsEnumName) {
    super(Arrays.asList(new String[] {taskId, taskName, null, taskPtnEnumName, remoteServer,
        pathFrom, isSrcPathDirEnumName, value, actionForNoSrcPathEnumName, pathTo,
        isDestPathDirEnumName, doesOverwriteDestPathEnumName,
        actionForDestFileExistsEnumName}));
  }

  /**
   * Constructs a new instance.
   * 
   * @param colList colList
   */
  @SuppressWarnings("null")
  public HousekeepFilesTaskRecord(List<String> colList) {
    super(colList);
  }

  public String getTaskId() {
    return taskId;
  }

  public String getTaskName() {
    return taskName;
  }

  public TaskPtnEnum getTaskPtn() {
    return TaskPtnEnum.valueOf(taskPtnEnumName);
  }

  public String getRemoteServer() {
    return remoteServer;
  }

  /**
   * Gets isSrcPathDir.
   */
  public @Nullable Boolean getIsSrcPathDir() {
    return StringUtils.isEmpty(isSrcPathDirEnumName) ? null
        : Boolean.valueOf(isSrcPathDirEnumName.toLowerCase(Locale.ROOT));
  }

  public String getSrcPath() {
    return srcPath;
  }

  public @Nullable Boolean getIsDestPathDir() {
    return StringUtils.isEmpty(isDestPathDirEnumName) ? null
        : Boolean.valueOf(isDestPathDirEnumName.toLowerCase(Locale.ROOT));
  }

  public String getDestPath() {
    return destPath;
  }

  public @Nullable Integer getValue() {
    return value == null ? null : Integer.valueOf(value);
  }

  public @Nullable IncidentTreatedAsEnum getActionForNoSrcPath() {
    return actionForNoSrcPathEnumName == null ? null
        : IncidentTreatedAsEnum.valueOf(actionForNoSrcPathEnumName);
  }

  public @Nullable Boolean getDoesOverwriteDestPath() {
    return StringUtils.isEmpty(doesOverwriteDestPathEnumName) ? null
        : Boolean.valueOf(doesOverwriteDestPathEnumName.toLowerCase(Locale.ROOT));
  }

  public @Nullable IncidentTreatedAsEnum getActionForDestFileExists() {
    return StringUtils.isEmpty(actionForDestFileExistsEnumName) ? null
        : IncidentTreatedAsEnum.valueOf(actionForDestFileExistsEnumName);
  }

  /**
   * Gets EnvVarExpandedSrcPath.
   */
  @SuppressWarnings("unused")
  public @Nullable String getEnvVarExpandedSrcPath() {
    if (envVarValueGetter == null) {
      throw new RuntimeException("envVarValueGetter must be set before call the method.");
    }

    return envVarExpandedSrcPath;
  }

  /**
   * Gets EnvVarExpandedDestPath.
   */
  @SuppressWarnings("unused")
  public @Nullable String getEnvVarExpandedDestPath() {
    if (envVarValueGetter == null) {
      throw new RuntimeException("envVarValueGetter must be set before call the method.");
    }

    return envVarExpandedDestPath;
  }

  /**
   * Sets the ${VAR} value resolver, and eagerly expands srcPath/destPath using it.
   */
  @SuppressWarnings("unused")
  public void setEnvVarValueGetter(Function<String, String> envVarValueGetter) {
    this.envVarValueGetter = envVarValueGetter == null ? (key -> null) : envVarValueGetter;

    // Retrieve pathInfoMap. Also expand environment variables in srcPath and destPath
    // during retrieval.
    envVarExpandedSrcPath = srcPath == null ? null : substituteEnvVars(srcPath);
    envVarExpandedDestPath = destPath == null ? null : substituteEnvVars(destPath);
  }

  private String substituteEnvVars(String path) {
    String envVarExpandedPath;
    try {
      envVarExpandedPath =
          EmbeddedVariableUtil.getVariableReplacedString(path, "${", "}", envVarValueGetter);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Remove "//".
    while (envVarExpandedPath.contains("//")) {
      envVarExpandedPath = envVarExpandedPath.replace("//", "/");
    }

    return envVarExpandedPath;
  }

  @Override
  public void afterReading() {

    // Source path related fields must all be filled or all empty.
    boolean isAllEmpty = StringUtils.isEmpty(srcPath) && StringUtils.isEmpty(isSrcPathDirEnumName)
        && StringUtils.isEmpty(value) && StringUtils.isEmpty(actionForNoSrcPathEnumName);
    boolean isAllNotEmpty = !StringUtils.isEmpty(srcPath)
        && !StringUtils.isEmpty(isSrcPathDirEnumName) && !StringUtils.isEmpty(value)
        && !StringUtils.isEmpty(actionForNoSrcPathEnumName);
    String[] lbls = new String[] {"srcPath", "isSrcPathDir", "value", "actionForNoSrcPath"};

    if (!isAllEmpty && !isAllNotEmpty) {
      new Violations().add(new BusinessViolation(
          "MSG_ERR_FIELDS_ARE_EITHER_ALL_EMPTY_OR_ALL_NOT_EMPTY",
          getLabelNameCsv(lbls))).throwIfAny();
    }

    // Destination path related fields must all be filled or all empty.
    isAllEmpty = StringUtils.isEmpty(destPath) && StringUtils.isEmpty(isDestPathDirEnumName)
        && StringUtils.isEmpty(doesOverwriteDestPathEnumName)
        && StringUtils.isEmpty(actionForDestFileExistsEnumName);
    isAllNotEmpty = !StringUtils.isEmpty(destPath) && !StringUtils.isEmpty(isDestPathDirEnumName)
        && !StringUtils.isEmpty(doesOverwriteDestPathEnumName)
        && !StringUtils.isEmpty(actionForDestFileExistsEnumName);
    lbls = new String[] {"destPath", "isDestPathDir", "doesOverwriteDestPath",
        "actionForToFileExists"};

    if (!isAllEmpty && !isAllNotEmpty) {
      new Violations().add(new BusinessViolation(
          "MSG_ERR_FIELDS_ARE_EITHER_ALL_EMPTY_OR_ALL_NOT_EMPTY",
          getLabelNameCsv(lbls))).throwIfAny();
    }
  }

  private String getLabelNameCsv(String[] itemIds) {
    StringBuilder sb = new StringBuilder();
    boolean is1st = true;
    for (String itemId : itemIds) {
      if (is1st) {
        is1st = false;
      } else {
        sb.append(", ");
      }

      sb.append(PropertiesFileUtil.getItemName(Locale.getDefault(),
          "HousekeepFilesTask." + itemId));
    }

    return sb.toString();
  }


}
