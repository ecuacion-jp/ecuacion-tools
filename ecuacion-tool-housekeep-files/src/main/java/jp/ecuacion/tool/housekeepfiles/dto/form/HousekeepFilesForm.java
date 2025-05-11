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
package jp.ecuacion.tool.housekeepfiles.dto.form;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesHdRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesPathRecord;
import jp.ecuacion.tool.housekeepfiles.reader.ExcelAuthListReader;
import jp.ecuacion.tool.housekeepfiles.reader.ExcelInfoListReader;
import jp.ecuacion.tool.housekeepfiles.reader.ExcelPathListReader;
import jp.ecuacion.tool.housekeepfiles.reader.ExcelTaskListReader;
import org.apache.poi.EncryptedDocumentException;

public class HousekeepFilesForm {

  /** infoはmapの形で保持。 */
  private Map<String, String> infoMap;

  // taskListを保持
  // ヘッダ情報があるので他と少々形が異なる
  private HousekeepFilesHdRecord taskInfoHdRec = null;

  // pathListを保持
  private List<HousekeepFilesPathRecord> pathInfoRecList = null;

  // authListを保持
  private List<HousekeepFilesAuthRecord> authInfoRecList = null;

  /** テスト用。 */
  public HousekeepFilesForm() {
    taskInfoHdRec = new HousekeepFilesHdRecord();
    pathInfoRecList = new ArrayList<>();
    authInfoRecList = new ArrayList<>();
  }

  public HousekeepFilesForm(String excelPath) throws AppException {
    readExcel(excelPath);
  }

  protected void readExcel(String excelPath) throws AppException {
    try {
      infoMap = new ExcelInfoListReader().readToMap(excelPath);
      taskInfoHdRec = new HousekeepFilesHdRecord();
      taskInfoHdRec.setSysName(infoMap.get("env-name"));
      taskInfoHdRec.recList = ((ExcelTaskListReader) (new ExcelTaskListReader()
          .ignoresAdditionalColumnsOfHeaderData(true))).readToBean(excelPath);
      pathInfoRecList = new ExcelPathListReader().readToBean(excelPath);
      authInfoRecList = new ExcelAuthListReader().readToBean(excelPath);

    } catch (EncryptedDocumentException | IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Map<String, String> getInfoMap() {
    return infoMap;
  }

  public HousekeepFilesHdRecord getTaskInfoHdRec() {
    return taskInfoHdRec;
  }

  public List<HousekeepFilesPathRecord> getPathInfoRecList() {
    return pathInfoRecList;
  }

  public List<HousekeepFilesAuthRecord> getAuthInfoRecList() {
    return authInfoRecList;
  }
}
