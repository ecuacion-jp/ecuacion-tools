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
package jp.ecuacion.tool.housekeepfiles.enums;

/** 
 * Enumerates task patterns.
 */
public enum TaskPtnEnum {
  //@formatter:off
  CREATE_DIR("101"), CREATE_FILE("102"), MOVE("111"), COPY("121"), DELETE("131"),
  ZIP_DELETE_ORIG("141"), ZIP_REMAIN_ORIG("142"), UNZIP_DELETE_ORIG("151"), 
  UNZIP_REMAIN_ORIG("152"),
  SFTP_CREATE_DIR("201"), SFTP_CREATE_FILE("202"), SFTP_MOVE_FROM_SERVER("211"), 
  SFTP_MOVE_TO_SERVER("212"), SFTP_COPY_FROM_SERVER("221"), SFTP_COPY_TO_SERVER("222"), 
  SFTP_DELETE_FROM_SERVER("231");
  //@formatter:on

  private String code;

  private TaskPtnEnum(String code) {
    this.code = code;
  }

  /**
   * Returns code.
   */
  public String getCode() {
    return code;
  }
}
