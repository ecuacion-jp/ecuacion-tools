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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesHdRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.reader.ExcelInfoListReader;
import jp.ecuacion.tool.housekeepfiles.util.LangExcelUtil;
import jp.ecuacion.util.excel.table.reader.concrete.StringOneLineHeaderExcelTableToBeanReader;
import org.jspecify.annotations.Nullable;

/**
 * Stores multiple records.
 */
@SuppressWarnings("NullAway.Init")
public class HousekeepFilesForm {

  // format-version / locale from the hidden Info sheet.
  private @Nullable String formatVersion;
  private @Nullable String locale;

  // Holds the task list.
  // Slightly different structure from others because it has header information.
  private HousekeepFilesHdRecord taskInfoHdRec;

  // Holds the auth list.
  private List<HousekeepFilesAuthRecord> authInfoRecList;

  /** only for unit-test. */
  @SuppressWarnings("null")
  public HousekeepFilesForm() {
    taskInfoHdRec = new HousekeepFilesHdRecord();
    authInfoRecList = new ArrayList<>();
  }

  /**
   * Constructs a new instance.
   *
   * @param excelPath excelPath
   */
  @SuppressWarnings("null")
  public HousekeepFilesForm(String excelPath) {
    readExcel(excelPath);
  }

  /**
   * Constructs a new instance.
   *
   * @param excelPath excelPath
   */
  protected void readExcel(String excelPath) {
    try {
      Map<String, String> infoMap = new ExcelInfoListReader().readToMap(excelPath);
      formatVersion = infoMap.get("format-version");
      locale = infoMap.get("locale");
      LangExcelUtil lang = new LangExcelUtil(Locale.of(Objects.requireNonNull(locale)));

      taskInfoHdRec = new HousekeepFilesHdRecord();
      taskInfoHdRec.recList =
          new StringOneLineHeaderExcelTableToBeanReader<HousekeepFilesTaskRecord>(
              HousekeepFilesTaskRecord.class, lang.get(LangExcelUtil.TASK_SETTINGS),
              lang.getHeaderLabels(HousekeepFilesTaskRecord.HEADER_LABEL_KEYS))
                  .withIgnoresAdditionalColumnsOfHeaderData(true).readToBean(excelPath, true);
      authInfoRecList = new StringOneLineHeaderExcelTableToBeanReader<HousekeepFilesAuthRecord>(
          HousekeepFilesAuthRecord.class, lang.get(LangExcelUtil.SERVER_AUTH_SETTINGS),
          lang.getHeaderLabels(HousekeepFilesAuthRecord.HEADER_LABEL_KEYS)).readToBean(excelPath,
              true);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public @Nullable String getFormatVersion() {
    return formatVersion;
  }

  public @Nullable String getLocale() {
    return locale;
  }

  public HousekeepFilesHdRecord getTaskInfoHdRec() {
    return taskInfoHdRec;
  }

  public List<HousekeepFilesAuthRecord> getAuthInfoRecList() {
    return authInfoRecList;
  }
}
