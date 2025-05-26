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
package jp.ecuacion.tool.housekeepfiles.reader;

import java.io.IOException;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.util.ValidationUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.apache.poi.EncryptedDocumentException;
import org.junit.jupiter.api.Test;

public class Test11_10_excelデータの値検証_taskList_単体項目チェック_共通 extends TestTool {

  
  
  @Test
  public void test01_共通_正常系_必須項目以外全てnull()
      throws EncryptedDocumentException, IOException, AppException {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_DIR", null, null, null, null, null, null, null, null, null, null, null);

    ValidationUtil.validateThenThrow(rec);
  }

  @Test
  public void test02_共通_正常系_全項目null以外の通常文字列入力()
      throws EncryptedDocumentException, IOException, AppException {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_MOVE_FROM_SERVER", "aHost", "aPath", "TRUE", "DAY", "7", "IGNORE", "aPath", "TRUE",
        "FALSE", "IGNORE", "key1=value2,key2");

    ValidationUtil.validateThenThrow(rec);
  }
}
