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

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;

/**
 * Sends the warning email of housekeep-db / housekeep-files through {@link SplibMailUtil}, the
 * same way (same mail server settings and recipients) as the system error email sent by
 * {@code AppExceptionHandlerAction}.
 *
 * <p>The recipients are {@value #PROP_ADDRESS_CSV_ON_SYSTEM_ERROR}, and the subject is
 *     {@code <title-prefix>[WARN] <tool name>:<target system name>}. When the recipients or the
 *     mail server settings aren't configured, only a warning is logged and processing goes on -
 *     a warning shouldn't make the job fail.</p>
 */
public final class HousekeepWarnMailUtil {

  /** The recipients of the system error email, reused for the warning email. */
  public static final String PROP_ADDRESS_CSV_ON_SYSTEM_ERROR =
      "jp.ecuacion.splib.mail.address-csv-on-system-error";

  /** The prefix prepended to the email subject. */
  public static final String PROP_TITLE_PREFIX = "jp.ecuacion.splib.mail.title-prefix";

  private HousekeepWarnMailUtil() {}

  /**
   * Sends the warning email.
   *
   * @param detailLogger the logger to write progress to
   * @param splibMailUtil the mail sender, may be {@code null} when not running in a Spring
   *     context (e.g. in tests), in which case no email is sent
   * @param env the Spring {@link Environment} used to resolve the recipients and subject prefix,
   *     may be {@code null} when not running in a Spring context, in which case no email is sent
   * @param toolNameInTitle the tool name shown in the subject, e.g. {@code "HousekeepDb"}
   * @param targetSystemName the optional name of the system housekept, appended to the subject;
   *     may be {@code null}, in which case it's simply omitted
   * @param content the email body, following the {@code hostname: ...} line
   */
  public static void send(DetailLogger detailLogger, @Nullable SplibMailUtil splibMailUtil,
      @Nullable Environment env, String toolNameInTitle, @Nullable String targetSystemName,
      String content) throws Exception {
    if (splibMailUtil == null || env == null) {
      detailLogger.warn("Warning(s) occurred but no mails sent since not running in a Spring "
          + "context.");
      return;
    }

    String addressCsv = env.getProperty(PROP_ADDRESS_CSV_ON_SYSTEM_ERROR);
    if (StringUtils.isEmpty(addressCsv)) {
      detailLogger.warn("Warning(s) occurred but no mails sent since '"
          + PROP_ADDRESS_CSV_ON_SYSTEM_ERROR + "' is not set.");
      return;
    }
    
    Objects.requireNonNull(addressCsv);

    List<@NonNull String> mailToList = Arrays.asList(addressCsv.split(",", -1));
    String title = env.getProperty(PROP_TITLE_PREFIX, "") + "[WARN] " + toolNameInTitle
        + (targetSystemName == null ? "" : ":" + targetSystemName);
    String body = "hostname: " + InetAddress.getLocalHost().getHostName() + "\n\n" + content;

    detailLogger.debug(body);

    try {
      splibMailUtil.sendTextMail(mailToList, null, title, body);

    } catch (IllegalStateException ex) {
      // Thrown when the mail server settings (spring.mail.*) aren't configured.
      detailLogger.warn("Warning(s) occurred but no mails sent: " + ex.getMessage());
      return;
    }

    detailLogger.info("Sent a mail to notice the warning(s).");
  }
}
