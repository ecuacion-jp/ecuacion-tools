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
import java.util.function.Function;
import jp.ecuacion.lib.core.util.EmbeddedVariableUtil;
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

  // No @Size here (unlike the other string fields above): a password/passphrase's length is
  // dictated by the remote server/key, not by this tool, and any length cap here would surface
  // the offending value itself in the validation error message (see getPassword()'s javadoc for
  // why that matters) the moment someone used a longer one.
  private String password;

  @Size(min = 1, max = 300)
  @Pattern(regexp = "^[^\\x00-\\x1F\"*<>?|]*$")
  private String keyPath;

  // Fields not in the Excel sheet.

  private @Nullable String envVarExpandedPassword;

  private Function<String, String> envVarValueGetter;

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

  /**
   * Returns the password/passphrase, with any {@code ${VAR}} reference it contains already
   * expanded via {@link #setEnvVarValueGetter}, if that was called; otherwise returns the raw
   * Excel value unchanged (e.g. when this record was built directly in a test without going
   * through {@link jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf#execute}).
   *
   * <p>This lets an operator keep the actual secret out of the settings Excel file entirely by
   * writing e.g. {@code ${SFTP_PASSWORD}} and defining {@code SFTP_PASSWORD} as an OS environment
   * variable / JVM system property / {@code application.properties} entry instead - the same
   * mechanism already used for srcPath/destPath.</p>
   */
  public String getPassword() {
    String expanded = envVarExpandedPassword;
    if (expanded != null) {
      return expanded;
    }

    return password;
  }

  public String getKeyPath() {
    return keyPath;
  }

  /**
   * Sets the ${VAR} value resolver and eagerly expands any {@code ${VAR}} reference in
   * {@code password}, throwing (via EmbeddedVariableUtil.VariableNotFoundException, wrapped in
   * RuntimeException) if a referenced variable cannot be resolved.
   */
  public void setEnvVarValueGetter(Function<String, String> envVarValueGetter) {
    this.envVarValueGetter = envVarValueGetter == null ? key -> null : envVarValueGetter;
    envVarExpandedPassword = password == null ? null : substituteEnvVars(password);
  }

  private String substituteEnvVars(String value) {
    try {
      return EmbeddedVariableUtil.getVariableReplacedString(value, "${", "}", envVarValueGetter);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void afterReading() {

  }
}
