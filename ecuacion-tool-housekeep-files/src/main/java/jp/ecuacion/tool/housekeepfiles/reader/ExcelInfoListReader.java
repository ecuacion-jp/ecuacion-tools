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
import jp.ecuacion.util.excel.exception.FarLeftHeaderLabelNotFoundException;
import jp.ecuacion.util.excel.table.reader.concrete.StringOneLineHeaderExcelTableReader;

/**
 * Reads the hidden Info sheet of the settings excel (format-version, locale, etc.).
 *
 * <p>The Info sheet's own header labels are localized too ("項目"/"値" in the ja sample,
 * "item"/"value" in the en sample), but locale isn't known yet at this point - it's the very
 * value this class is reading. So the ja labels are tried first, falling back to the en labels
 * if not found, rather than looking up a locale-specific header label.</p>
 */
public class ExcelInfoListReader {

  private static final String[] HEADER_LABELS_JA = new String[] {"項目", "値"};
  private static final String[] HEADER_LABELS_EN = new String[] {"item", "value"};

  /**
   * Returns excel data as map format.
   */
  public Map<String, String> readToMap(String excelPath) throws IOException {
    // Retrieve the table data as a list.
    List<List<String>> rowList;
    try {
      rowList = readRows(excelPath, HEADER_LABELS_JA);
    } catch (FarLeftHeaderLabelNotFoundException ex) {
      try {
        rowList = readRows(excelPath, HEADER_LABELS_EN);
      } catch (IOException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Map<String, String> rtnMap = new HashMap<>();
    for (List<String> colList : rowList) {
      rtnMap.put(colList.get(0), colList.get(1));
    }

    return rtnMap;
  }

  private List<List<String>> readRows(String excelPath, String[] headerLabels) throws Exception {
    return new StringOneLineHeaderExcelTableReader("Info", headerLabels).read(excelPath);
  }
}
