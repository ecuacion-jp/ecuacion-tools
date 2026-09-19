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
package jp.ecuacion.tool.housekeepcommon.util;

import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.util.SplibLogUtil;
import jp.ecuacion.splib.core.util.SplibLogUtil.LogKeyValue;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

/**
 * Logs the startup / finish messages shared by housekeep-db's and housekeep-files' tasklets.
 */
public final class HousekeepLogUtil {

  private HousekeepLogUtil() {}

  /**
   * Logs the tool name, the excel settings file path, and (when present) the target system name.
   *
   * @param toolName e.g. {@code "housekeep-db"} / {@code "housekeep-files"}
   * @param targetSystemName omitted from the log when {@code null}
   */
  public static void logStarted(DetailLogger detailLogger, String toolName, String excelPath,
      @Nullable String targetSystemName) {
    detailLogger.info(toolName + " started.");

    List<LogKeyValue> list = new ArrayList<>();
    list.add(new LogKeyValue("Excel File Path", excelPath));
    if (targetSystemName != null) {
      list.add(new LogKeyValue("Target System Name", targetSystemName));
    }

    SplibLogUtil.logKeyValueList(detailLogger, Level.INFO, 0, list);
  }

  /**
   * Logs the excel settings file's format version and locale, once read.
   */
  public static void logExcelFormatInfo(DetailLogger detailLogger,
      @Nullable String formatVersion, @Nullable String locale) {
    List<LogKeyValue> list = List.of(
        new LogKeyValue("Format Excel Version", String.valueOf(formatVersion)),
        new LogKeyValue("Locale", String.valueOf(locale)));

    SplibLogUtil.logKeyValueList(detailLogger, Level.INFO, 0, list);
  }

  /**
   * Logs successful completion.
   *
   * @param toolName e.g. {@code "housekeep-db"} / {@code "housekeep-files"}
   */
  public static void logFinishedSuccessfully(DetailLogger detailLogger, String toolName) {
    detailLogger.info(toolName + " finished successfully.");
  }
}
