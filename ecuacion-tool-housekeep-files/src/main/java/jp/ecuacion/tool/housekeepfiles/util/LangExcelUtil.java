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
package jp.ecuacion.tool.housekeepfiles.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;

/**
 * Provides message IDs to internationalize the settings excel file.
 */
public class LangExcelUtil {
  public static final String TASK_SETTINGS = "EXCEL_SHEET_TASK_SETTINGS";
  public static final String SERVER_AUTH_SETTINGS = "EXCEL_SHEET_SERVER_AUTH_SETTINGS";

  private Locale locale;

  /**
   * Constructs a new instance.
   *
   * @param locale locale
   */
  public LangExcelUtil(Locale locale) {
    this.locale = locale;
  }

  /**
   * Gets localized message from {@code key}.
   *
   * @param key message ID
   * @return message
   */
  public String get(String key) {
    return PropertiesFileUtil.getMessage(locale, key);
  }

  /**
   * Provides a localized array of header labels.
   *
   * @param headerLabelKeys headerLabelKeys
   * @return localized header labels
   */
  @SuppressWarnings("null")
  public String[] getHeaderLabels(String[] headerLabelKeys) {
    List<String> list = Arrays.asList(headerLabelKeys).stream().map(key -> get(key)).toList();
    return list.toArray(new String[list.size()]);
  }

  /**
   * Contains message IDs related to TaskSettings.
   */
  public static class TaskSettings {
    public static final String TASK_ID = "EXCEL_TABLE_HEADER_TASK_ID";
    public static final String TASK_NAME = "EXCEL_TABLE_HEADER_TASK_NAME";
    public static final String TASK_PTN_DISPLAY = "EXCEL_TABLE_HEADER_TASK_PTN_DISPLAY";
    public static final String TASK_PTN = "EXCEL_TABLE_HEADER_TASK_PTN";
    public static final String REMOTE_SERVER = "EXCEL_TABLE_HEADER_REMOTE_SERVER";
    public static final String SRC_PATH = "EXCEL_TABLE_HEADER_SRC_PATH";
    public static final String IS_SRC_PATH_DIR = "EXCEL_TABLE_HEADER_IS_SRC_PATH_DIR";
    public static final String SRC_PATH_WAIT_DAYS = "EXCEL_TABLE_HEADER_SRC_PATH_WAIT_DAYS";
    public static final String ACTION_FOR_NO_SRC_PATH = "EXCEL_TABLE_HEADER_ACTION_FOR_NO_SRC_PATH";
    public static final String DEST_PATH = "EXCEL_TABLE_HEADER_DEST_PATH";
    public static final String IS_DEST_PATH_DIR = "EXCEL_TABLE_HEADER_IS_DEST_PATH_DIR";
    public static final String OVERWRITE_DEST_PATH = "EXCEL_TABLE_HEADER_OVERWRITE_DEST_PATH";
    public static final String ACTION_FOR_DEST_PATH_EXISTS =
        "EXCEL_TABLE_HEADER_ACTION_FOR_DEST_PATH_EXISTS";

    @SuppressWarnings("MutablePublicArray")
    public static final String[] HEADER_LABELS = new String[] {TASK_ID, TASK_NAME,
        TASK_PTN_DISPLAY, TASK_PTN, REMOTE_SERVER, SRC_PATH, IS_SRC_PATH_DIR, SRC_PATH_WAIT_DAYS,
        ACTION_FOR_NO_SRC_PATH, DEST_PATH, IS_DEST_PATH_DIR, OVERWRITE_DEST_PATH,
        ACTION_FOR_DEST_PATH_EXISTS};
  }

  /**
   * Contains message IDs related to ServerAuthSettings.
   */
  public static class ServerAuthSettings {
    public static final String SERVER_NAME = "EXCEL_TABLE_HEADER_SERVER_NAME";
    public static final String PROTOCOL = "EXCEL_TABLE_HEADER_PROTOCOL";
    public static final String PORT = "EXCEL_TABLE_HEADER_PORT";
    public static final String AUTH_TYPE = "EXCEL_TABLE_HEADER_AUTH_TYPE";
    public static final String USERNAME = "EXCEL_TABLE_HEADER_USERNAME";
    public static final String PASSWORD_OR_PASSPHRASE = "EXCEL_TABLE_HEADER_PASSWORD_OR_PASSPHRASE";
    public static final String KEY_PATH = "EXCEL_TABLE_HEADER_KEY_PATH";

    @SuppressWarnings("MutablePublicArray")
    public static final String[] HEADER_LABELS = new String[] {SERVER_NAME, PROTOCOL, PORT,
        AUTH_TYPE, USERNAME, PASSWORD_OR_PASSPHRASE, KEY_PATH};
  }
}
