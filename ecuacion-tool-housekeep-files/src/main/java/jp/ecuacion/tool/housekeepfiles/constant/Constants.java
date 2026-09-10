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
package jp.ecuacion.tool.housekeepfiles.constant;

/**
 * Stores constants.
 */
public class Constants {
  public static final String PACKAGE_HK_TASK = "jp.ecuacion.tool.housekeepfiles.bl.task";
  public static final String HOUSEKEEP_FILES_PATH_LIST_XML = "housekeepFile-pathList.xml";

  public static final String ENV_VAR_TASK_NAME = "TASK_NAME";
  public static final String ENV_VAR_DATE = "DATE";
  public static final String ENV_VAR_DATETIME = "DATETIME";
  public static final String ENV_VAR_TIMESTAMP = "TIMESTAMP";
  public static final String ENV_VAR_HOSTNAME = "HOSTNAME";

  /**
   * Set to {@code false} (in {@code application.properties} /
   * {@code application_profile.properties}
   * or as a JVM {@code -D} system property) to disable SFTP host key verification, e.g. for quick
   * local trials. Never disable it against a production or otherwise untrusted network.
   */
  public static final String PROP_SFTP_STRICT_HOST_KEY_CHECKING =
      "jp.ecuacion.tool.housekeep-files.sftp.strict-host-key-checking";

  /**
   * Optional system name shown in job start/finish logs and the warning email subject. When
   * unset, that part of the log/email is simply omitted.
   */
  public static final String PROP_SYSTEM_NAME = "jp.ecuacion.tool.housekeep-files.system-name";

  /**
   * SFTP session/channel connect timeout in milliseconds (in {@code application.properties} /
   * {@code application_profile.properties} or as a JVM {@code -D} system property). Without a
   * timeout, a server that never responds to the TCP handshake or the SSH negotiation hangs the
   * batch indefinitely. Defaults to {@link #DEFAULT_SFTP_CONNECT_TIMEOUT_MILLIS} when unset or
   * not a valid integer.
   */
  public static final String PROP_SFTP_CONNECT_TIMEOUT_MILLIS =
      "jp.ecuacion.tool.housekeep-files.sftp.connect-timeout-millis";

  public static final int DEFAULT_SFTP_CONNECT_TIMEOUT_MILLIS = 30000;

  /**
   * Upper bound, in bytes, on the total uncompressed size an {@code UNZIP_*} task will write for a
   * single archive (in {@code application.properties} / {@code application_profile.properties} or
   * as a JVM {@code -D} system property). Guards against a "zip bomb" - a small archive that
   * decompresses to an enormous size and fills the disk - placed in a monitored directory by a
   * less-trusted party. Defaults to {@link #DEFAULT_UNZIP_MAX_TOTAL_BYTES} when unset or not a
   * valid long.
   */
  public static final String PROP_UNZIP_MAX_TOTAL_BYTES =
      "jp.ecuacion.tool.housekeep-files.unzip.max-total-bytes";

  public static final long DEFAULT_UNZIP_MAX_TOTAL_BYTES = 10L * 1024 * 1024 * 1024; // 10 GiB

}
