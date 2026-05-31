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
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bl.HousekeepFilesBl;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTask;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTaskLocal;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.other.HousekeepFilesExpandedPathsInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;

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
   */
  public void execute(HousekeepFilesForm form) throws Exception {
    // Log output.
    logJobStartMsg(form);

    // List to hold warning information.
    final List<BusinessViolation> warnList = new ArrayList<>();

    // Cross-record and cross-data-type validation.
    bl.consistencyCheckBetweenMultipleData(form);

    // Build envVarInfo as a Map.
    Map<String, String> envVarInfoMap = bl.createPathInfoMap(form);

    // Build authInfo as a Map. The key is "<server name>-<protocol>".
    final Map<String, HousekeepFilesAuthRecord> authMap =
        form.getAuthInfoRecList().stream().collect(
            Collectors.toMap(rec -> rec.getRemoteServer() + "-" + rec.getProtocol(), rec -> rec));

    // Verify that environment variables in srcPath and destPath exist in envVarInfoMap,
    // and set the expanded paths.
    bl.envVarExistenceCheckAndSetEnvBarExpandedPaths(form.getTaskInfoHdRec().recList,
        envVarInfoMap);

    // Per-task processing below.
    // Ideally the following would be a single loop, but grouping task creation and checks first
    // makes error messages easier to read - all tasks are validated before any execution.
    // File/directory existence is not checked here; only Excel-to-task consistency is validated.
    Violations violations = new Violations();
    bl.createTaskAndTaskDependentCheck(form, violations);
    violations.throwIfAny();

    // Map to store multiple connections.
    Map<String, ConnectionToRemoteServer> connectionMap = new HashMap<>();
    try {
      // Execute task.
      for (HousekeepFilesTaskRecord taskInfo : form.getTaskInfoHdRec().recList) {
        execEachTask(taskInfo.task, connectionMap, taskInfo, authMap, warnList);
      }

    } finally {
      // Close connection.
      for (Entry<String, ConnectionToRemoteServer> entry : connectionMap.entrySet()) {
        entry.getValue().closeConnection();
      }
    }

    // Send email if there are warnings.
    if (!warnList.isEmpty()) {
      bl.sendWarnMail(warnList, form.getTaskInfoHdRec());
    }

    // Log output.
    logJobFinishMsg(form);
  }

  private void logJobStartMsg(HousekeepFilesForm form) {
    dlog.debug("####################");
    dlog.debug("##### startJob :" + form.getTaskInfoHdRec().getSysName());
  }

  private void logJobFinishMsg(HousekeepFilesForm form) {
    dlog.debug("##### finishJob:" + form.getTaskInfoHdRec().getSysName());
  }

  /**
   * Execute by task.
   */
  protected void execEachTask(AbstractTask task,
      Map<String, ConnectionToRemoteServer> connectionMap, HousekeepFilesTaskRecord taskInfo,
      Map<String, HousekeepFilesAuthRecord> authMap,
      List<BusinessViolation> warnList) throws Exception {

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
    HousekeepFilesExpandedPathsInfo pathInfo =
        bl.expandAllPath(task, taskInfo, conn);

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
