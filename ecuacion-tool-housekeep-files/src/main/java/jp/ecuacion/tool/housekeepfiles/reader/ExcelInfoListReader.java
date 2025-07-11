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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.util.poi.excel.table.reader.concrete.StringOneLineHeaderExcelTableReader;
import org.apache.poi.EncryptedDocumentException;

/**
 * Reads info sheet of the settings excel.
 */
public class ExcelInfoListReader extends StringOneLineHeaderExcelTableReader {

  private static final String[] headerLabels = new String[] {"項目", "値"};

  /**
   * Constructs a new instance.
   */
  public ExcelInfoListReader() {
    super("基礎情報設定", headerLabels, null, 1, null);
  }

  /**
   * Returns excel data as map format.
   */
  public Map<String, String> readToMap(String excelPath)
      throws EncryptedDocumentException, IOException, AppException {
    // 表の情報をlistの形で取得
    List<List<String>> rowList = read(excelPath);

    Map<String, String> rtnMap = new HashMap<>();
    for (List<String> colList : rowList) {
      rtnMap.put(colList.get(0), colList.get(1));
    }

    return rtnMap;
  }
}
