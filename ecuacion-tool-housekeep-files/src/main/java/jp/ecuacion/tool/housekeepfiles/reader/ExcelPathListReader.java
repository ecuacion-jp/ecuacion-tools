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

import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesPathRecord;
import jp.ecuacion.util.poi.excel.table.reader.concrete.StringOneLineHeaderExcelTableToBeanReader;

public class ExcelPathListReader
    extends StringOneLineHeaderExcelTableToBeanReader<HousekeepFilesPathRecord> {

  private static final String[] headerLabels = new String[] {"パス変数名", "パス値"};

  public ExcelPathListReader() {
    super(HousekeepFilesPathRecord.class, "パス設定", headerLabels, null, 1, null);
  }

}
