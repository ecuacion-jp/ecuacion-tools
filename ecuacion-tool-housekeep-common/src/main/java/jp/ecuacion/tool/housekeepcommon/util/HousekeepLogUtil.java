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

import jp.ecuacion.lib.core.logging.DetailLogger;
import org.jspecify.annotations.Nullable;

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
    detailLogger.info("- Excel File Path     : " + excelPath);
    if (targetSystemName != null) {
      detailLogger.info("- Target System Name  : " + targetSystemName);
    }
  }

  /**
   * Logs the excel settings file's format version and locale, once read.
   */
  public static void logExcelFormatInfo(DetailLogger detailLogger,
      @Nullable String formatVersion, @Nullable String locale) {
    detailLogger.info("- Format Excel Version: " + formatVersion);
    detailLogger.info("- Locale              : " + locale);
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
