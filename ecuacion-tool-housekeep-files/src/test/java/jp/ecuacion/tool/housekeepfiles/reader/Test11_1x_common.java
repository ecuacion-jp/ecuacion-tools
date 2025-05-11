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
import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;

public class Test11_1x_common extends TestTool {
  protected ExcelTaskListReader reader;

  protected void createReader(HousekeepFilesTaskRecord rec) {
    List<HousekeepFilesTaskRecord> list = new ArrayList<>();
    list.add(rec);
    reader = new ExcelTaskListReader() {
      @Override
      protected List<HousekeepFilesTaskRecord> excelTableToBeanList(String filePath)
          throws AppException, IOException {
        return list;
      }
    };
  }
}
