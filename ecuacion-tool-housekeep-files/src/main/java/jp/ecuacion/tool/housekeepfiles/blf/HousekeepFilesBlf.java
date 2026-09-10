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
package jp.ecuacion.tool.housekeepfiles.blf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.core.util.SplibLogUtil;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bl.HousekeepFilesBl;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTask;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTaskLocal;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.other.HousekeepFilesExpandedPathsInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;

/**
 * Provides business logics for housekeeping files.
 */
public class HousekeepFilesBlf {
  DetailLogger dlog = new DetailLogger(this);
  HousekeepFilesBl bl = new HousekeepFilesBl();

  /**
   * Construct a new instance.
   */
  public HousekeepFilesBlf() {

  }

  /** only for unit test. */
  public HousekeepFilesBlf(HousekeepFilesBl bl) {
    this.bl = bl;
  }

  /**
   * Executes housekeeping.
   *
   * <p>Convenience overload for callers with no Spring Environment (e.g. most existing unit
   * tests) - only built-in path variables (DATE/DATETIME/TIMESTAMP/HOSTNAME) resolve, and the
   * optional system name (see {@link Constants#PROP_SYSTEM_NAME}) is omitted from logs/emails.</p>
   */
  public void execute(HousekeepFilesForm form) throws Exception {
    execute(form, null);
  }

  /**
   * Executes housekeeping.
   *
   * @param env the Spring Environment used to resolve ${VAR} references in srcPath/destPath
   *     that aren't one of the built-in variables (DATE/DATETIME/TIMESTAMP/HOSTNAME), and to look up
   *     the optional system name (see {@link Constants#PROP_SYSTEM_NAME}) shown in job
   *     start/finish logs and the warning email subject; may be {@code null}, in which case only
   *     built-in variables resolve and the system name is omitted.
   */
  public void execute(HousekeepFilesForm form, @Nullable Environment env) throws Exception {
    final String systemName = env == null ? null : env.getProperty(Constants.PROP_SYSTEM_NAME);

    // List to hold warning information.
    final List<BusinessViolation> warnList = new ArrayList<>();

    // Cross-record and cross-data-type validation.
    bl.consistencyCheckBetweenMultipleData(form);

    // Build the ${VAR} value resolver: built-in variables + env fallback.
    Map<String, String> builtInVariableMap = bl.createBuiltInVariableMap();
    Function<String, String> envVarValueGetter =
        bl.createEnvVarValueGetter(builtInVariableMap, env);

    // Build authInfo as a Map. The key is "<server name>-<protocol>".
    final Map<String, HousekeepFilesAuthRecord> authMap =
        form.getAuthInfoRecList().stream().collect(
            Collectors.toMap(rec -> rec.getRemoteServer() + "-" + rec.getProtocol(), rec -> rec));

    // Resolve ${VAR} references in every task's srcPath/destPath up front (fails fast).
    bl.setEnvVarValueGetterOnTasks(form.getTaskInfoHdRec().recList, envVarValueGetter);
    // Resolve ${VAR} references in every auth record's password/passphrase up front, so it can be
    // kept out of the settings Excel file and supplied via environment variable instead.
    bl.setEnvVarValueGetterOnAuthRecords(form.getAuthInfoRecList(), envVarValueGetter);

    // Per-task processing below.
    // Ideally the following would be a single loop, but grouping task creation and checks first
    // makes error messages easier to read - all tasks are validated before any execution.
    // File/directory existence is not checked here; only Excel-to-task consistency is validated.
    Violations violations = new Violations();
    bl.createTaskAndTaskDependentCheck(form, violations);
    violations.throwIfAny();

    // Map to store multiple connections.
    Map<String, ConnectionToRemoteServer> connectionMap = new HashMap<>();
    dlog.info("Per-task procedure started.");
    try {
      // Execute task.
      for (HousekeepFilesTaskRecord taskInfo : form.getTaskInfoHdRec().recList) {
        SplibLogUtil.info(dlog, "Task started  : " + taskInfo.getTaskId(), 1);

        execEachTask(taskInfo.task, connectionMap, taskInfo, authMap, warnList);

        SplibLogUtil.info(dlog, "Task finished : " + taskInfo.getTaskId(), 1);
      }

    } finally {
      // Close connection.
      for (Entry<String, ConnectionToRemoteServer> entry : connectionMap.entrySet()) {
        entry.getValue().closeConnection();
      }
    }

    // Send email if there are warnings.
    if (!warnList.isEmpty()) {
      bl.sendWarnMail(warnList, systemName);
    }

    dlog.info("housekeep-files finished successfully.");
  }

  /**
   * Execute by task.
   */
  protected void execEachTask(AbstractTask task,
      Map<String, ConnectionToRemoteServer> connectionMap, HousekeepFilesTaskRecord taskInfo,
      Map<String, HousekeepFilesAuthRecord> authMap, List<BusinessViolation> warnList)
      throws Exception {

    // Retrieve connection if not already held.
    final String connectionKey = taskInfo.getRemoteServer() + "." + task.getConnectionProtocol();
    if (!(task instanceof AbstractTaskLocal)) {
      if (!connectionMap.containsKey(connectionKey)) {
        connectionMap.put(connectionKey, task.getConnection(taskInfo.getRemoteServer(), authMap));
      }
    }

    // Retrieve the connection used by this task.
    ConnectionToRemoteServer conn = connectionMap.get(connectionKey);

    // Expand ${VAR} references and wildcards in PATH.
    HousekeepFilesExpandedPathsInfo pathInfo = bl.expandAllPath(task, taskInfo, conn);

    // Checks passed, so populate toPath in pathInfoMap.
    // For task patterns with no destination (delete, zip), pathInfo.tmpToFileList will be
    // empty - account for this.
    if (pathInfo.tmpToFileList.size() > 0) {
      pathInfo.toPath = pathInfo.tmpToFileList.get(0);
    }

    warnList.addAll(bl.logicalCheckTaskListAfterEnvVarExpansion(task, taskInfo, pathInfo));

    // Execute the process.
    bl.doTaskForMultipleFiles(taskInfo, pathInfo, conn, warnList);
  }
}
