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

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.jakartavalidation.validator.BooleanString;
import jp.ecuacion.lib.core.jakartavalidation.validator.EnumElement;
import jp.ecuacion.lib.core.jakartavalidation.validator.IntegerString;
import jp.ecuacion.lib.core.util.EmbeddedParameterUtil;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTask;
import jp.ecuacion.tool.housekeepfiles.enums.IncidentTreatedAsEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.util.poi.excel.table.bean.StringExcelTableBean;
import org.apache.commons.lang3.StringUtils;

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

  @Pattern(regexp = "DAY")
  public String unitName;

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

  public String options;

  // 以下、excelにはない項目

  private String envVarExpandedSrcPath;

  private String envVarExpandedDestPath;

  private Map<String, String> envVarInfoMap;

  // taskオブジェクトを保持しておく
  public AbstractTask task;

  @Override
  protected @Nonnull String[] getFieldNameArray() {
    return new String[] {"taskId", "taskName", null, "taskPtnEnumName", "remoteServer", "srcPath",
        "isSrcPathDirEnumName", "unitName", "value", "actionForNoSrcPathEnumName", "destPath",
        "isDestPathDirEnumName", "doesOverwriteDestPathEnumName", "actionForDestFileExistsEnumName",
        "options"};
  }

  /** テスト用。 */
  public HousekeepFilesTaskRecord(String taskId, String taskName, String taskPtnEnumName,
      String remoteServer, String pathFrom, String isSrcPathDirEnumName, String unitName,
      String value, String actionForNoSrcPathEnumName, String pathTo, String isDestPathDirEnumName,
      String doesOverwriteDestPathEnumName, String actionForDestFileExistsEnumName,
      String options) {
    super(Arrays.asList(new String[] {taskId, taskName, null, taskPtnEnumName, remoteServer,
        pathFrom, isSrcPathDirEnumName, unitName, value, actionForNoSrcPathEnumName, pathTo,
        isDestPathDirEnumName, doesOverwriteDestPathEnumName, actionForDestFileExistsEnumName,
        options}));
  }

  public HousekeepFilesTaskRecord(List<String> colList) {
    super(colList);
  }

  public Integer getUnit() {
    int rtn = -1;
    if (unitName == null || unitName.equals("")) {
      return null;

    } else if (unitName.equals("YEAR")) {
      rtn = Calendar.YEAR;

    } else if (unitName.equals("MONTH")) {
      rtn = Calendar.MONTH;

    } else if (unitName.equals("DAY")) {
      rtn = Calendar.DAY_OF_MONTH;

    } else if (unitName.equals("HOUR")) {
      rtn = Calendar.HOUR;

    } else if (unitName.equals("MINUTE")) {
      rtn = Calendar.MINUTE;
    } else if (unitName.equals("SECOND")) {
      rtn = Calendar.SECOND;

    } else {
      throw new RuntimeException("Not exist unit value: " + unitName);
    }

    return rtn;
  }

  public void setUnit(String unit) {
    throw new RuntimeException("Unit cannot be set. set 'unitName'.");
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

  public Boolean getIsSrcPathDir() throws BizLogicAppException {
    return (StringUtils.isEmpty(isSrcPathDirEnumName)) ? null
        : Boolean.valueOf(isSrcPathDirEnumName.toLowerCase());
  }

  public String getSrcPath() {
    return srcPath;
  }

  public Boolean getIsDestPathDir() {
    return (StringUtils.isEmpty(isDestPathDirEnumName)) ? null
        : Boolean.valueOf(isDestPathDirEnumName.toLowerCase());
  }

  public String getDestPath() {
    return destPath;
  }

  public Integer getValue() {
    return value == null ? null : Integer.valueOf(value);
  }

  public IncidentTreatedAsEnum getActionForNoSrcPath() {
    return actionForNoSrcPathEnumName == null ? null
        : IncidentTreatedAsEnum.valueOf(actionForNoSrcPathEnumName);
  }

  public Boolean getDoesOverwriteDestPath() {
    return (StringUtils.isEmpty(doesOverwriteDestPathEnumName)) ? null
        : Boolean.valueOf(doesOverwriteDestPathEnumName.toLowerCase());
  }

  public IncidentTreatedAsEnum getActionForDestFileExists() {
    return (StringUtils.isEmpty(actionForDestFileExistsEnumName)) ? null
        : IncidentTreatedAsEnum.valueOf(actionForDestFileExistsEnumName);

  }

  public String getEnvVarExpandedSrcPath() {
    if (envVarInfoMap == null) {
      throw new RuntimeException("envVarInfoMap must be set before call the method.");
    }

    return envVarExpandedSrcPath;
  }

  public String getEnvVarExpandedDestPath() {
    if (envVarInfoMap == null) {
      throw new RuntimeException("envVarInfoMap must be set before call the method.");
    }

    return envVarExpandedDestPath;
  }

  /**
   * pathInfoMapを取得。 取得時にsrcPath、destPathの環境変数展開を併せて実施。
   * @throws MultipleAppException 
   */
  public void setEnvVarInfoMap(Map<String, String> envVarInfoMap) throws AppException {
    if (envVarInfoMap == null) {
      envVarInfoMap = new HashMap<>();
    }

    this.envVarInfoMap = envVarInfoMap;

    envVarExpandedSrcPath = srcPath == null ? null : substituteEnvVars(srcPath);
    envVarExpandedDestPath = destPath == null ? null : substituteEnvVars(destPath);
  }

  private String substituteEnvVars(String path) throws BizLogicAppException, MultipleAppException {
    String envVarExpandedPath =
        EmbeddedParameterUtil.getParameterReplacedString(path, "${", "}", envVarInfoMap);

    // "//"を取り除く
    while (envVarExpandedPath.contains("//")) {
      envVarExpandedPath = envVarExpandedPath.replace("//", "/");
    }

    return envVarExpandedPath;
  }

  @Override
  public void afterReading() throws AppException {

    // 元パス関連情報は、全て入力か全て未入力のいずれか
    boolean isAllEmpty = StringUtils.isEmpty(srcPath) && StringUtils.isEmpty(isSrcPathDirEnumName)
        && StringUtils.isEmpty(unitName) && StringUtils.isEmpty(value)
        && StringUtils.isEmpty(actionForNoSrcPathEnumName);
    boolean isAllNotEmpty = !StringUtils.isEmpty(srcPath)
        && !StringUtils.isEmpty(isSrcPathDirEnumName) && !StringUtils.isEmpty(unitName)
        && !StringUtils.isEmpty(value) && !StringUtils.isEmpty(actionForNoSrcPathEnumName);
    String[] lbls = new String[] {"srcPath", "isSrcPathDir", "unit", "value", "actionForNoSrcPath"};

    if (!isAllEmpty && !isAllNotEmpty) {
      throw new BizLogicAppException("MSG_ERR_FIELDS_ARE_EITHER_ALL_EMPTY_OR_ALL_NOT_EMPTY",
          getLabelNameCsv(lbls));
    }

    // 先パス関連情報は、全て入力か全て未入力のいずれか
    isAllEmpty = StringUtils.isEmpty(destPath) && StringUtils.isEmpty(isDestPathDirEnumName)
        && StringUtils.isEmpty(doesOverwriteDestPathEnumName)
        && StringUtils.isEmpty(actionForDestFileExistsEnumName);
    isAllNotEmpty = !StringUtils.isEmpty(destPath) && !StringUtils.isEmpty(isDestPathDirEnumName)
        && !StringUtils.isEmpty(doesOverwriteDestPathEnumName)
        && !StringUtils.isEmpty(actionForDestFileExistsEnumName);
    lbls = new String[] {"destPath", "isDestPathDir", "doesOverwriteDestPath",
        "actionForToFileExists"};

    if (!isAllEmpty && !isAllNotEmpty) {
      throw new BizLogicAppException("MSG_ERR_FIELDS_ARE_EITHER_ALL_EMPTY_OR_ALL_NOT_EMPTY",
          getLabelNameCsv(lbls));
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

      sb.append(PropertyFileUtil.getItemName("HousekeepFilesTask." + itemId));
    }

    return sb.toString();
  }


}
