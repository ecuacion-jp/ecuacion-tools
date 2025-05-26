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
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.reader.ExcelInfoListReader;
import jp.ecuacion.util.poi.excel.table.reader.concrete.StringOneLineHeaderExcelTableToBeanReader;
import org.apache.poi.EncryptedDocumentException;

/**
 * Stores multiple records.
 */
public class HousekeepFilesForm {

  /** info records are stored as map format. */
  private Map<String, String> infoMap;

  // taskListを保持
  // ヘッダ情報があるので他と少々形が異なる
  private HousekeepFilesHdRecord taskInfoHdRec = null;

  // pathListを保持
  private List<HousekeepFilesPathRecord> pathInfoRecList = null;

  // authListを保持
  private List<HousekeepFilesAuthRecord> authInfoRecList = null;

  private static final String[] HEADER_LABELS_TASK = new String[] {"タスクID", "タスク名", "処理パターン\n日本語名",
      "処理パターン", "接続先サーバ", "元パス", "元パスがディレクトリ", "元パス処理実施対象\n経過期間単位", "元パス処理実施対象\n経過期間値",
      "元パス存在なし時処理", "先パス", "先パスがディレクトリ", "先パス存在時上書き", "先パス存在時処理", "options"};
  private static final String[] HEADER_LABELS_PATH = new String[] {"パス変数名", "パス値"};
  private static final String[] HEADER_LABELS_AUTH =
      new String[] {"サーバ名", "protocol", "port", "認証方式", "ユーザ名", "password / passphrase", "秘密鍵パス"};

  /** only for unit-test. */
  public HousekeepFilesForm() {
    taskInfoHdRec = new HousekeepFilesHdRecord();
    pathInfoRecList = new ArrayList<>();
    authInfoRecList = new ArrayList<>();
  }

  /**
   * Constructs a new instance.
   * 
   * @param excelPath excelPath
   * @throws AppException AppException
   */
  public HousekeepFilesForm(String excelPath) throws AppException {
    readExcel(excelPath);
  }

  /**
   * Constructs a new instance.
   * 
   * @param excelPath excelPath
   * @throws AppException AppException
   */
  protected void readExcel(String excelPath) throws AppException {
    try {
      infoMap = new ExcelInfoListReader().readToMap(excelPath);
      taskInfoHdRec = new HousekeepFilesHdRecord();
      taskInfoHdRec.setSysName(infoMap.get("env-name"));
      taskInfoHdRec.recList =
          (new StringOneLineHeaderExcelTableToBeanReader<HousekeepFilesTaskRecord>(
              HousekeepFilesTaskRecord.class, "タスク設定", HEADER_LABELS_TASK, null, 1, null)
                  .ignoresAdditionalColumnsOfHeaderData(true)).readToBean(excelPath);
      pathInfoRecList = new StringOneLineHeaderExcelTableToBeanReader<HousekeepFilesPathRecord>(
          HousekeepFilesPathRecord.class, "パス設定", HEADER_LABELS_PATH, null, 1, null)
              .readToBean(excelPath);
      authInfoRecList = new StringOneLineHeaderExcelTableToBeanReader<HousekeepFilesAuthRecord>(
          HousekeepFilesAuthRecord.class, "サーバ認証設定", HEADER_LABELS_AUTH, null, 1, null)
              .readToBean(excelPath);

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
