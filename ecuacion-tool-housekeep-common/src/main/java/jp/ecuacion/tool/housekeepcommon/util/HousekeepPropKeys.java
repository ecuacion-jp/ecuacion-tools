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

/**
 * Holds the property key parts shared by housekeep-db's and housekeep-files' tasklets, so each
 * tool only needs to name itself (e.g. {@code "housekeep-db"}) and build its keys as
 * {@code PREFIX + toolName + SUFFIX_*}, e.g. {@code "jp.ecuacion.tool.housekeep-db.excel-path"}.
 *
 * <p>Kept as constants (rather than a key-building method) so the result stays usable as the
 * compile-time constant Spring's {@code @Value(...)} requires.</p>
 */
public final class HousekeepPropKeys {

  private HousekeepPropKeys() {}

  public static final String PREFIX = "jp.ecuacion.tool.";
  public static final String SUFFIX_EXCEL_PATH = ".excel-path";
  public static final String SUFFIX_TARGET_SYSTEM_NAME = ".target-system-name";
}
