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
package jp.ecuacion.tool.housekeepfiles.bl.task;

import static jp.ecuacion.tool.housekeepfiles.bl.task.TaskAttrCheckPtnEnum.ARBITRARY;
import static jp.ecuacion.tool.housekeepfiles.bl.task.TaskAttrCheckPtnEnum.PROHIBITED;
import static jp.ecuacion.tool.housekeepfiles.bl.task.TaskAttrCheckPtnEnum.REQUIRED;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.other.FileInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.IncidentTreatedAsEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.tool.housekeepfiles.util.WildcardPathUtil;
import org.jspecify.annotations.Nullable;

/**
 * Provides abstract task definitions and common task utilities.
 *
 * @author yosuk_000
 *
 */
@SuppressWarnings("NullAway.Init")
public abstract class AbstractTask {

  protected TaskPtnEnum taskPtn;

  // Defines required, prohibited, and optional fields in the XML for each task.
  protected TaskAttrCheckPtnEnum inputRuleForSrcPathInfo;
  protected TaskAttrCheckPtnEnum inputRuleForDestPathInfo;

  /**
   * Also defines attributes that affect task behavior.
   * The following represents tasks where no destination is specified in the XML, but a destination
   * file is created as a result (intended for zip/unzip). Defaults to false; only relevant tasks
   * override this.
   */
  protected boolean doesCreateOutputFileAutomatically = false;

  protected DetailLogger dlog = new DetailLogger(this);

  /** Initializes task attributes based on task action kind. */
  @SuppressWarnings("null")
  public AbstractTask() {
    TaskActionKindEnum taskActionKind = getTaskActionKind();

    // Set allowable values based on taskActionKind. Override in individual task classes when
    // exceptional patterns occur.
    if (taskActionKind == TaskActionKindEnum.create) {
      inputRuleForSrcPathInfo = PROHIBITED;
      inputRuleForDestPathInfo = REQUIRED;

    } else if (taskActionKind == TaskActionKindEnum.change) {
      inputRuleForSrcPathInfo = REQUIRED;
      inputRuleForDestPathInfo = REQUIRED;

    } else if (taskActionKind == TaskActionKindEnum.delete) {
      inputRuleForSrcPathInfo = REQUIRED;
      inputRuleForDestPathInfo = PROHIBITED;

    } else if (taskActionKind == TaskActionKindEnum.createFromOriginal) {
      inputRuleForSrcPathInfo = REQUIRED;
      inputRuleForDestPathInfo = ARBITRARY;
    }
  }

  public TaskAttrCheckPtnEnum getInputRuleForSrcPath() {
    return inputRuleForSrcPathInfo;
  }

  public TaskAttrCheckPtnEnum getInputRuleForDestPath() {
    return inputRuleForDestPathInfo;
  }

  /** Returns true if this task automatically creates output files. */
  public boolean doesCreateOutputFileAutomatically() {
    return doesCreateOutputFileAutomatically;
  }

  /**
   * Validates the HousekeepFilesTaskRecord input for the task. Separated from doTask because
   * validation runs before task execution.
   */
  public void check(HousekeepFilesTaskRecord dtRec) {
    Violations violations = new Violations();
    checkNeedRemoteServer(dtRec, violations);
    checkRequiredOrProhibited(violations, dtRec);

    // Input validation including null-value legitimacy is complete up to this point.
    // Writing null checks for values that should never be null would be cumbersome,
    // so return early if there are errors.
    violations.throwIfAny();

    taskDependentCheck(dtRec, violations);

    violations.throwIfAny();
  }

  /** Called from check. Each task implements its own validation. */
  public abstract void taskDependentCheck(HousekeepFilesTaskRecord taskRec,
      Violations violations);

  /** Validates the remoteServer field based on whether this task uses a remote connection. */
  @SuppressWarnings("null")
  protected void checkNeedRemoteServer(HousekeepFilesTaskRecord taskRec,
      Violations violations) {
    boolean containsRemoteAction = (isSrcPathLocal() != null && !isSrcPathLocal())
        || (isDestPathLocal() != null && !isDestPathLocal());

    checkTaskItemNoThrow(violations, taskRec.getTaskId(), taskPtn,
        containsRemoteAction ? TaskAttrCheckPtnEnum.REQUIRED : TaskAttrCheckPtnEnum.PROHIBITED,
        "remoteServer", taskRec.getRemoteServer());
  }

  private void checkRequiredOrProhibited(Violations violations,
      HousekeepFilesTaskRecord taskRec) {

    // Source path related. Since all-or-nothing for source path fields has already been verified,
    // only srcPath is checked here as a representative.
    checkTaskItemNoThrow(violations, taskRec.getTaskId(), taskPtn, getInputRuleForSrcPath(),
        "srcPath", taskRec.getSrcPath());

    // Destination path related. Since all-or-nothing for destination path fields has already been
    // verified, only destPath is checked here as a representative.
    checkTaskItemNoThrow(violations, taskRec.getTaskId(), taskPtn, getInputRuleForDestPath(),
        "destPath", taskRec.getDestPath());
  }

  void checkTaskItemNoThrow(Violations violations, String taskId, TaskPtnEnum taskPtn,
      TaskAttrCheckPtnEnum checkPtn, String itemTitle, Object itemValue) {
    checkTaskItem(violations, taskId, taskPtn, checkPtn, itemTitle, itemValue);
  }

  void checkTaskItem(Violations violations, String taskId, TaskPtnEnum taskPtn,
      TaskAttrCheckPtnEnum checkPtn, String itemTitle, @Nullable Object itemValue) {
    String taskPtnName = (taskPtn == null) ? "" : taskPtn.toString();
    boolean isEmpty = itemValue == null || (itemValue instanceof String && itemValue.equals(""));
    if (checkPtn == REQUIRED && isEmpty) {
      violations.add(new BusinessViolation("MSG_ERR_TASK_REQUIRED_CHECK", taskId, taskPtnName,
          PropertiesFileUtil.getItemName(Locale.getDefault(), "HousekeepFilesTask." + itemTitle)));
    }

    if (checkPtn == PROHIBITED && !isEmpty) {
      violations.add(new BusinessViolation("MSG_ERR_TASK_PROHIBITED_CHECK", taskId, taskPtnName,
          PropertiesFileUtil.getItemName(Locale.getDefault(), "HousekeepFilesTask." + itemTitle)));
    }
  }

  // abstract methods.

  /** Specifies the task type, which determines whether from/to are required, etc. */
  public abstract TaskActionKindEnum getTaskActionKind();

  /** Sets the protocol such as "SFTP". Null for local. */
  public abstract @Nullable String getConnectionProtocol();

  /** Connection for each remote communication. */
  public abstract @Nullable ConnectionToRemoteServer getConnection(String remoteServer,
      Map<String, HousekeepFilesAuthRecord> authMap) throws Exception;


  /** Returns true if the source path is local, null if not applicable. */
  public abstract @Nullable Boolean isSrcPathLocal();

  /** Returns true if the destination path is local, null if not applicable. */
  public abstract @Nullable Boolean isDestPathLocal();

  /** Returns remote file info for the specified path, or null if not found. */
  protected abstract @Nullable FileInfo getRemoteFileInfo(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path);

  /** Returns a list of remote file infos matching the specified path pattern. */
  protected abstract @Nullable List<FileInfo> getRemoteFileInfoList(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path);

  /** Executes the task. Call this to run the task from outside. */
  public void doTask(@Nullable ConnectionToRemoteServer conn, HousekeepFilesTaskRecord taskRec,
      @Nullable String srcPath, @Nullable String destPath, List<BusinessViolation> warnList)
      throws Exception {
    doTaskInternal(conn, taskRec, srcPath, destPath, warnList);
  }

  /** Called from doTask. Each task implements its own processing. */
  protected abstract void doTaskInternal(@Nullable ConnectionToRemoteServer conn,
      HousekeepFilesTaskRecord taskRec, @Nullable String srcPath, @Nullable String destPath,
      List<BusinessViolation> warnList) throws Exception;

  /** Returns whether the task holds source path information. */
  public boolean hasSrcPathInfo() {
    if (getTaskActionKind() == TaskActionKindEnum.create) {
      return false;

    } else {
      return true;
    }
  }

  /** Returns whether the task holds destination path information. */
  public boolean hasDestPathInfo() {
    if (getTaskActionKind() == TaskActionKindEnum.delete) {
      return false;

    } else {
      return true;
    }
  }

  /** Returns file info for the destination path, delegating to local or remote implementation. */
  public @Nullable FileInfo getToPathFileInfo(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path)
      throws Exception {
    if (Objects.requireNonNull(isDestPathLocal())) {
      return getLocalFileInfo(path);

    } else {
      return getRemoteFileInfo(task, connection, isPathDir, path);
    }
  }

  /** Returns a list of file infos for source files matching the specified path pattern. */
  public @Nullable List<FileInfo> getFromDirFileInfoList(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path) {
    if (Objects.requireNonNull(isSrcPathLocal())) {
      return getLocalFileInfoList(path);

    } else {
      return getRemoteFileInfoList(task, connection, isPathDir, path);
    }
  }

  // Local processing is defined in this class.

  /** Returns true if the specified local directory path exists. */
  protected boolean localDirExists(String dirPath) {
    return new File(dirPath).exists() && new File(dirPath).isDirectory();
  }

  /** Retrieves the file information from the local disk. */
  protected @Nullable FileInfo getLocalFileInfo(String path) {
    File file = new File(path);

    // Returns null if the path does not exist.
    if (!file.exists()) {
      return null;
    }

    FileInfo fi = new FileInfo();
    fi.setFilePath(path);
    fi.setDirectory(file.isDirectory());
    fi.setLastUpdTimeInMillis(file.lastModified());
    // Check for file lock.
    try {
      FileUtil.isLocked(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return fi;
  }

  /** Retrieves the file list from the local disk. */
  @SuppressWarnings("null")
  protected List<FileInfo> getLocalFileInfoList(String path) {
    List<FileInfo> rtnList = new ArrayList<FileInfo>();
    List<String> pathStrList = WildcardPathUtil.getPathListFromPathWithWildcard(path);
    for (String strPath : pathStrList) {
      FileInfo fi = new FileInfo();
      File fileObj = new File(strPath);
      fi.setFilePath(strPath);
      fi.setDirectory(fileObj.isDirectory());
      fi.setLastUpdTimeInMillis(fileObj.lastModified());
      // Check for file lock.
      try {
        FileUtil.isLocked(strPath);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      rtnList.add(fi);
    }
    return rtnList;
  }

  /** Branches processing based on whether the setting is warn or error. */
  protected void treatIncident(@Nullable IncidentTreatedAsEnum pattern, BusinessViolation violation,
      List<BusinessViolation> warnList) {
    if (pattern == IncidentTreatedAsEnum.WARN) {
      warnList.add(violation);

    } else if (pattern == IncidentTreatedAsEnum.ERROR) {
      new Violations().add(violation).throwIfAny();
    }
  }

  /** Handles a conflict with an existing destination path based on task configuration. */
  protected void treatDestPathExists(HousekeepFilesTaskRecord taskRec, String destPath,
      List<BusinessViolation> warnList) {
    treatIncident(taskRec.getActionForDestFileExists(),
        new BusinessViolation("MSG_ERR_DEST_PATH_EXISTS", taskRec.getTaskId(),
            taskRec.getTaskName(), destPath),
        warnList);
  }
}
