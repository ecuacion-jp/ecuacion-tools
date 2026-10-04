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
package jp.ecuacion.tool.housekeepdb.bl;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import jp.ecuacion.tool.housekeepdb.bl.HousekeepAbnormalDataChecker.Result;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;

/**
 * Sends one warning email listing the records every abnormal data check task of a run found.
 *
 * <p>The recipients are the same as the system error email's
 *     ({@value #PROP_ADDRESS_CSV_ON_SYSTEM_ERROR}), and the subject is prefixed with
 *     {@value #PROP_TITLE_PREFIX} the same way. When the recipients or the mail server settings
 *     aren't configured, only a warning is logged - the found records are already logged at
 *     {@code WARN} by {@link HousekeepAbnormalDataChecker} anyway.</p>
 */
public class AbnormalDataWarnMailSender {

  /** The recipients of the system error email, reused for this warning email. */
  public static final String PROP_ADDRESS_CSV_ON_SYSTEM_ERROR =
      "jp.ecuacion.splib.mail.address-csv-on-system-error";

  /** The prefix prepended to the email subject. */
  public static final String PROP_TITLE_PREFIX = "jp.ecuacion.splib.mail.title-prefix";

  private final DetailLogger detailLogger;
  private final SplibMailUtil splibMailUtil;
  private final Environment env;

  /**
   * Creates the sender.
   *
   * @param detailLogger the logger to write progress to
   * @param splibMailUtil the mail sender
   * @param env the Spring {@link Environment}, used to resolve the recipients and subject prefix
   */
  public AbnormalDataWarnMailSender(DetailLogger detailLogger, SplibMailUtil splibMailUtil,
      Environment env) {
    this.detailLogger = detailLogger;
    this.splibMailUtil = splibMailUtil;
    this.env = env;
  }

  /**
   * Sends the warning email.
   *
   * @param resultList the records each abnormal data check task found, not empty
   * @param targetSystemName the optional name of the system whose DB records are housekept,
   *     appended to the email subject; may be {@code null}, in which case it's simply omitted
   */
  public void send(List<Result> resultList, @Nullable String targetSystemName) throws Exception {
    String addressCsv = env.getProperty(PROP_ADDRESS_CSV_ON_SYSTEM_ERROR);
    if (StringUtils.isEmpty(addressCsv)) {
      detailLogger.warn("Record(s) that should not exist found but no mails sent since '"
          + PROP_ADDRESS_CSV_ON_SYSTEM_ERROR + "' is not set.");
      return;
    }
    
    Objects.requireNonNull(addressCsv);

    List<@NonNull String> mailToList = Arrays.asList(addressCsv.split(",", -1));
    String title = env.getProperty(PROP_TITLE_PREFIX, "") + "[WARN] HousekeepDb"
        + (targetSystemName == null ? "" : ":" + targetSystemName);
    String content = createContent(resultList, InetAddress.getLocalHost().getHostName());

    detailLogger.debug(content);

    try {
      splibMailUtil.sendTextMail(mailToList, null, title, content);

    } catch (IllegalStateException ex) {
      // Thrown when the mail server settings (spring.mail.*) aren't configured.
      detailLogger.warn("Record(s) that should not exist found but no mails sent: "
          + ex.getMessage());
      return;
    }

    detailLogger.info("Sent a mail to notice the record(s) that should not exist.");
  }

  /**
   * Creates the email body.
   *
   * <p>Package-private for unit testing.</p>
   *
   * @param resultList the records each abnormal data check task found
   * @param hostname the name of the host running this tool
   * @return the email body
   */
  static String createContent(List<Result> resultList, String hostname) {
    StringBuilder sb = new StringBuilder();
    sb.append("hostname: " + hostname + "\n\n");
    sb.append("Record(s) that should not exist were found by the abnormal data check task(s) "
        + "below:\n\n");

    for (Result result : resultList) {
      sb.append("- Task ID: " + result.taskId() + "\n");
      sb.append("  Table  : " + result.table() + "\n");
      sb.append("  Count  : " + result.count() + "\n");
      sb.append("  " + result.idColumn() + ": " + HousekeepAbnormalDataChecker.getIdsText(result)
          + "\n");
    }

    return sb.toString();
  }
}
