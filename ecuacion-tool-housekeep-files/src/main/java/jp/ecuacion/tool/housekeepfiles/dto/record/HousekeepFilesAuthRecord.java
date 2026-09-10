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
import jp.ecuacion.lib.validation.constraints.EnumElement;
import jp.ecuacion.lib.validation.constraints.IntegerString;
import jp.ecuacion.lib.validation.constraints.NotEmptyWhen;
import jp.ecuacion.lib.validation.constraints.enums.ConditionValue;
import jp.ecuacion.tool.housekeepfiles.enums.AuthTypeEnum;
import jp.ecuacion.tool.housekeepfiles.enums.FileManipulateProtocolEnum;
import jp.ecuacion.tool.housekeepfiles.util.LangExcelUtil;
import jp.ecuacion.util.excel.table.bean.StringExcelTableBean;
import org.jspecify.annotations.Nullable;

/**
 * Store Auth info.
 */
// keyPath is required only when authType is KEY, since AbstractTaskSftp#getConnection()
// unconditionally calls ssh.addIdentity(auth.getKeyPath()) in that case.
@NotEmptyWhen(propertyPath = "keyPath", conditionPropertyPath = "authType",
    conditionValue = ConditionValue.STRING, conditionValueString = "KEY")
// password is required only when authType is PASSWORD; for KEY it is an optional passphrase,
// and for KERBEROS it is unused.
@NotEmptyWhen(propertyPath = "password", conditionPropertyPath = "authType",
    conditionValue = ConditionValue.STRING, conditionValueString = "PASSWORD")
@SuppressWarnings("NullAway.Init")
public class HousekeepFilesAuthRecord extends StringExcelTableBean {

  public static final String[] HEADER_LABEL_KEYS = LangExcelUtil.ServerAuthSettings.HEADER_LABELS;

  @NotEmpty
  @Size(min = 1, max = 40)
  @Pattern(regexp = "^[^!\"#\\$%&'\\(\\)=\\^~\\\\\\|`\\[\\{;\\+:\\\\*\\]\\},<>/\\?]*$")
  private String remoteServer;

  @NotEmpty
  @EnumElement(enumClass = FileManipulateProtocolEnum.class)
  private String protocol;

  @NotEmpty
  @IntegerString
  @DecimalMin(value = "0")
  @DecimalMax(value = "99999")
  private String port;

  @NotEmpty
  @EnumElement(enumClass = AuthTypeEnum.class)
  private String authType;

  @NotEmpty
  @Size(min = 1, max = 40)
  @Pattern(regexp = "^[^!\"#\\$%&'\\(\\)=\\^~\\\\\\|`\\[\\{;\\+:\\\\*\\]\\},<>/\\?]*$")
  private String userName;

  @Size(min = 1, max = 30)
  private String password;

  @Size(min = 1, max = 300)
  @Pattern(regexp = "^[^\\x00-\\x1F\"*<>?|]*$")
  private String keyPath;

  @Override
  protected @Nullable String[] getFieldNameArray() {
    return new String[] {"remoteServer", "protocol", "port", "authType", "userName", "password",
        "keyPath"};
  }

  /**
   * Constructs a new instance.
   * 
   * @param colList colList
   */
  @SuppressWarnings("null")
  public HousekeepFilesAuthRecord(List<String> colList) {
    super(colList);
  }

  /**
   * only for unit test.
   */
  @SuppressWarnings("null")
  public HousekeepFilesAuthRecord(@Nullable String remoteServer, @Nullable String protocol,
      @Nullable String port, @Nullable String authType, @Nullable String userName,
      @Nullable String password, @Nullable String keyPath) {

    super(Arrays.asList(
        new String[] {remoteServer, protocol, port, authType, userName, password, keyPath}));
  }

  public String getRemoteServer() {
    return remoteServer;
  }

  public String getProtocol() {
    return protocol;
  }

  public int getPort() {
    return Integer.parseInt(port);
  }

  public AuthTypeEnum getAuthType() {
    return AuthTypeEnum.valueOf(authType);
  }

  public String getUserName() {
    return userName;
  }

  public String getPassword() {
    return password;
  }

  public String getKeyPath() {
    return keyPath;
  }

  @Override
  public void afterReading() {

  }
}
