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
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.tool.housekeepfiles.enums.AuthTypeEnum;
import jp.ecuacion.util.poi.excel.table.bean.StringExcelTableBean;

public class HousekeepFilesAuthRecord extends StringExcelTableBean {

  @NotEmpty
  @Size(min = 1, max = 40)
  @Pattern(regexp = "^[^!\"#\\$%&'\\(\\)=\\^~\\\\\\|`\\[\\{;\\+:\\\\*\\]\\},<>/\\?]*$")
  private String remoteServer;

  @NotEmpty
  private String protocol;

  @NotEmpty
  @DecimalMin(value = "0")
  @DecimalMax(value = "99999")
  private String port;

  @NotEmpty
  private String authType;

  @Size(min = 1, max = 40)
  @Pattern(regexp = "^[^!\"#\\$%&'\\(\\)=\\^~\\\\\\|`\\[\\{;\\+:\\\\*\\]\\},<>/\\?]*$")
  private String userName;

  @Size(min = 1, max = 30)
  private String password;

  @Size(min = 1, max = 300)
  private String keyPath;

  @Override
  protected @Nonnull String[] getFieldNameArray() {
    return new String[] {"remoteServer", "protocol", "port", "authType", "userName", "password",
        "keyPath"};
  }

  public HousekeepFilesAuthRecord(List<String> colList) {
    super(colList);
  }

  /** テスト用。 */
  public HousekeepFilesAuthRecord(String remoteServer, String protocol, String port,
      String authType, String userName, String password, String keyPath) {

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
  public void afterReading() throws AppException {

  }
}
