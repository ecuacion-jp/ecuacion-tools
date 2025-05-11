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

import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.util.poi.excel.table.reader.concrete.StringOneLineHeaderExcelTableToBeanReader;

public class ExcelTaskListReader
    extends StringOneLineHeaderExcelTableToBeanReader<HousekeepFilesTaskRecord> {

  private static final String[] headerLabels = new String[] {"タスクID", "タスク名", "処理パターン\n日本語名",
      "処理パターン", "接続先サーバ", "元パス", "元パスがディレクトリ", "元パス処理実施対象\n経過期間単位", "元パス処理実施対象\n経過期間値",
      "元パス存在なし時処理", "先パス", "先パスがディレクトリ", "先パス存在時上書き", "先パス存在時処理", "options"};

  public ExcelTaskListReader() {
    super(HousekeepFilesTaskRecord.class, "タスク設定", headerLabels, null, 1, null);
  }
}
