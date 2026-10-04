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

import java.util.List;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import jp.ecuacion.tool.housekeepcommon.util.HousekeepWarnMailUtil;
import jp.ecuacion.tool.housekeepdb.bl.HousekeepAbnormalDataChecker.Result;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;

/**
 * Sends one warning email listing the records every abnormal data check task of a run found.
 *
 * <p>Sending itself (recipients, subject, and what happens when the mail settings aren't
 *     configured) is delegated to {@link HousekeepWarnMailUtil} - the found records are already
 *     logged at {@code WARN} by {@link HousekeepAbnormalDataChecker} regardless.</p>
 */
public class AbnormalDataWarnMailSender {

  private static final String TOOL_NAME_IN_TITLE = "HousekeepDb";

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
    HousekeepWarnMailUtil.send(detailLogger, splibMailUtil, env, TOOL_NAME_IN_TITLE,
        targetSystemName, createContent(resultList));
  }

  /**
   * Creates the email body (following the {@code hostname: ...} line).
   *
   * @param resultList the records each abnormal data check task found
   * @return the email body
   */
  private static String createContent(List<Result> resultList) {
    StringBuilder sb = new StringBuilder();
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
