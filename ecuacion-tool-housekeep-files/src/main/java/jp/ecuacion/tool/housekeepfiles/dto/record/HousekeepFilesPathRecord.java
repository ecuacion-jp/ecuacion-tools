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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.util.poi.excel.table.bean.StringExcelTableBean;

/**
 * Stores path info.
 */
public class HousekeepFilesPathRecord extends StringExcelTableBean {

  @NotEmpty
  @Size(min = 1, max = 50)
  @Pattern(regexp = "^[a-zA-Z0-9 -/:-@\\[-\\`\\{-\\~]*$")
  @Pattern(regexp = "^[A-Z0-9_]*$")
  @Pattern(regexp = "^[^!\"#\\$%&'\\(\\)=\\^~\\\\\\|`\\[\\{;\\+:\\\\*\\]\\},<>/\\?]*$")
  private String key;

  @NotEmpty
  @Size(min = 1, max = 300)
  private String value;

  // accessor:key
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  // accessor:value
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  protected @Nonnull String[] getFieldNameArray() {
    return new String[] {"key", "value"};
  }

  /**
   * Constructs a new instance.
   * 
   * @param colList colList
   */
  public HousekeepFilesPathRecord(List<String> colList) {
    super(colList);
  }

  @Override
  public void afterReading() throws AppException {

  }
}
