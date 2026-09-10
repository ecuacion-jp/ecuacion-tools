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
package jp.ecuacion.tool.housekeepfiles.bl;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.lib.core.util.MailUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.core.util.SplibLogUtil;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTask;
import jp.ecuacion.tool.housekeepfiles.bl.task.TaskAttrCheckPtnEnum;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.other.FileInfo;
import jp.ecuacion.tool.housekeepfiles.dto.other.HousekeepFilesExpandedPathsInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.IncidentTreatedAsEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.tool.housekeepfiles.util.DateTimeUtil;
import jp.ecuacion.tool.housekeepfiles.util.HkFileManipulateUtil;
import jp.ecuacion.tool.housekeepfiles.util.WildcardPathUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;

/**
 * Provides business logics.
 */
@SuppressWarnings("NullAway")
public class HousekeepFilesBl {
  public static final String EXTENSION_NONE_WITH_DOT = "";
  public static final String EXTENSION_ZIP_WITH_DOT = ".zip";

  private DetailLogger dlog = new DetailLogger(this);
  private DateTimeUtil dateUtil = new DateTimeUtil();
  private HkFileManipulateUtil fmu = new HkFileManipulateUtil();


  /** Validates cross-record consistency such as duplicate task IDs and task names. */
  public void consistencyCheckBetweenMultipleData(HousekeepFilesForm form) {
    // Error if task count is zero.
    if (form.getTaskInfoHdRec().recList == null || form.getTaskInfoHdRec().recList.size() == 0) {
      new Violations().add(new BusinessViolation("MSG_ERR_AT_LEAST_ONE_TASK_NEEDED")).throwIfAny();
    }

    // Verify that taskId and taskName are not duplicated.
    HashSet<String> taskIdSet = new HashSet<String>();
    HashSet<String> taskNameSet = new HashSet<String>();
    for (HousekeepFilesTaskRecord rec : form.getTaskInfoHdRec().recList) {
      // taskId
      if (taskIdSet.contains(rec.getTaskId())) {
        new Violations().add(new BusinessViolation("MSG_ERR_TASK_ID_DUPLICATED",
            rec.getTaskId())).throwIfAny();

      } else {
        taskIdSet.add(rec.getTaskId());
      }

      // taskName
      if (taskNameSet.contains(rec.getTaskName())) {
        new Violations().add(new BusinessViolation("MSG_ERR_TASK_NAME_DUPLICATED",
            rec.getTaskName())).throwIfAny();

      } else {
        taskNameSet.add(rec.getTaskName());
      }
    }
  }

  /** Creates a map of the built-in path variables (date, timestamp, hostname). */
  public Map<String, String> createBuiltInVariableMap() throws UnknownHostException {
    Map<String, String> builtInVariableMap = new HashMap<>();
    builtInVariableMap.put(Constants.ENV_VAR_DATE, dateUtil.getDateStr8());
    builtInVariableMap.put(Constants.ENV_VAR_DATETIME, dateUtil.getDateTimeStr15());
    builtInVariableMap.put(Constants.ENV_VAR_TIMESTAMP, dateUtil.getTimestampNumString());
    builtInVariableMap.put(Constants.ENV_VAR_HOSTNAME, InetAddress.getLocalHost().getHostName());

    return builtInVariableMap;
  }

  /**
   * Builds the ${VAR} value resolver used to expand srcPath/destPath: built-in variables (see
   * {@link #createBuiltInVariableMap}) take precedence and cannot be overridden; any other key
   * falls back to {@code env} (application.properties, OS environment variables, JVM system
   * properties, command-line arguments - anything Spring Boot's Environment can resolve).
   * {@code env} may be {@code null} (e.g. when exercised outside of Spring, such as in unit
   * tests), in which case any non-built-in key resolves to {@code null} (i.e. "not found").
   */
  public Function<String, String> createEnvVarValueGetter(Map<String, String> builtInVariableMap,
      @Nullable Environment env) {
    return key -> builtInVariableMap.containsKey(key) ? builtInVariableMap.get(key)
        : (env == null ? null : env.getProperty(key));
  }

  /**
   * Sets the ${VAR} value resolver on every task record, which eagerly expands srcPath/destPath
   * and throws (via EmbeddedVariableUtil.VariableNotFoundException, wrapped in RuntimeException)
   * if any referenced variable cannot be resolved. All records are processed before any task is
   * executed, so a missing variable anywhere fails the whole batch before it does anything.
   */
  public void setEnvVarValueGetterOnTasks(List<HousekeepFilesTaskRecord> taskRecList,
      Function<String, String> envVarValueGetter) {
    for (HousekeepFilesTaskRecord rec : taskRecList) {
      rec.setEnvVarValueGetter(envVarValueGetter);
    }
  }

  /** Creates task instances for all task records and runs task-specific input validation. */
  public void createTaskAndTaskDependentCheck(HousekeepFilesForm form,
      Violations violations) throws Exception {
    for (HousekeepFilesTaskRecord dtRec : form.getTaskInfoHdRec().recList) {
      createTaskInstance(dtRec, dtRec.getTaskPtn());
      // Per-task required/prohibited field validation.
      try {
        dtRec.task.check(dtRec);
      } catch (ViolationException ex) {
        for (BusinessViolation bv : ex.getViolations().getBusinessViolations()) {
          violations.add(bv);
        }
      }
    }
  }

  /** Creates a task instance by reflection from the task pattern and sets it on the record. */
  public void createTaskInstance(HousekeepFilesTaskRecord dtRec, TaskPtnEnum taskPtn)
      throws Exception {
    @SuppressWarnings("unchecked")
    Class<AbstractTask> cls = (Class<AbstractTask>) Class.forName(
        Constants.PACKAGE_HK_TASK + "." + StringUtil.getUpperCamelFromSnake(taskPtn.toString()));
    dtRec.task = cls.getDeclaredConstructor().newInstance();
  }

  /** Expands all path patterns for the given task and returns source and destination path lists. */
  public HousekeepFilesExpandedPathsInfo expandAllPath(AbstractTask task,
      HousekeepFilesTaskRecord taskRec,
      @Nullable ConnectionToRemoteServer connection) throws Exception {

    List<String> fromPathList = new ArrayList<String>();
    List<String> toPathList = new ArrayList<String>();

    // src
    if (task.hasSrcPathInfo()) {
      expandFromPath(connection, taskRec, task, fromPathList);
    }

    // dest
    if (task.hasDestPathInfo()) {
      expandToPath(connection, taskRec, task, toPathList);
    }

    // The "to" path should ultimately be exactly one, but skip the check here
    // and keep it as an ArrayList.
    return new HousekeepFilesExpandedPathsInfo(fromPathList, toPathList);
  }

  @SuppressWarnings("null")
  private void expandFromPath(@Nullable ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, AbstractTask task, List<String> fromPathList) {

    List<FileInfo> tmpFromFileAndDirMixedList = task.getFromDirFileInfoList(task, connection,
        taskRec.getIsSrcPathDir(), taskRec.getEnvVarExpandedSrcPath());
    for (FileInfo fi : tmpFromFileAndDirMixedList) {
      // Keep only entries where isSrcPathDir matches the actual path type (file or directory);
      // skip otherwise.
      if (!(taskRec.getIsSrcPathDir() == true) == fi.isDirectory()) {
        continue;

      }
      // Further filter to entries whose last-modified date satisfies the elapsed-time condition.
      // Exclude entries that have not yet passed the required period.
      if (!dateUtil.hasDesignatedTermPassed(fi.getLastUpdTimeInMillis(), taskRec.getValue())) {
        continue;
      }

      // If the file is locked, skip adding it to the list and only output a warning log.
      // This applies only to supported protocols (currently only the local filesystem).
      if (fi.isLocked()) {
        logWithTaskId(taskRec.getTaskId(), "Skipping because the file is locked: "
            + fi.getFilePath());
        continue;
      }

      // Add to the list.
      fromPathList.add(fi.getFilePath());
    }
  }

  @SuppressWarnings("null")
  private void expandToPath(@Nullable ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, AbstractTask task, List<String> toPathList)
      throws Exception {
    // to
    String varSubstitutedToPath = taskRec.getEnvVarExpandedDestPath();

    // If no destination path is specified, there is nothing to expand.
    if (varSubstitutedToPath == null || varSubstitutedToPath.isEmpty()) {
      return;
    }

    List<String> tmpToFileAndDirMixedList = null;
    // Branch based on whether a wildcard is present.
    // If no wildcard, auto-create the directory.
    if (!WildcardPathUtil.containsWildCard(varSubstitutedToPath)) {
      // fu.getPathListFromPathWithWildcard includes a clean step, but this path bypasses it,
      // so call clean individually.
      varSubstitutedToPath = FileUtil.cleanPathStrWithSlash(varSubstitutedToPath);
      tmpToFileAndDirMixedList = new ArrayList<String>();
      tmpToFileAndDirMixedList.add(varSubstitutedToPath);

      // Create the directory if it does not exist.
      String dirPath = (taskRec.getIsDestPathDir() != null && taskRec.getIsDestPathDir() == true)
          ? varSubstitutedToPath
          : new File(varSubstitutedToPath).getParent();

      // Convert relative paths to absolute paths.
      if (dirPath != null && !dirPath.startsWith("/")) {
        String dirPathTmp = dirPath.startsWith("./") ? dirPath.substring(2) : dirPath;
        dirPath = FileUtil.concatFilePaths(System.getProperty("user.dir"), dirPathTmp);
      }

    } else {
      tmpToFileAndDirMixedList =
          WildcardPathUtil.getPathListFromPathWithWildcard(varSubstitutedToPath);
    }

    for (String path : tmpToFileAndDirMixedList) {
      // If the destination path file/directory already exists.
      FileInfo dirInfo = task.getToPathFileInfo(task, connection, true, path);
      FileInfo fileInfo = task.getToPathFileInfo(task, connection, false, path);
      if (dirInfo == null && fileInfo == null) {
        toPathList.add(path);

      } else {
        // Add to toPathList only when isDestPathDir matches the actual path type
        // (file or directory).
        if ((dirInfo != null && taskRec.getIsDestPathDir())
            || (fileInfo != null && !taskRec.getIsDestPathDir())) {
          toPathList.add(path);
        }
        // if ((dtE.getIsDestPathDir() == true) == task.getToPathFileInfo(connection,
        // path).isDirectory()) toPathList.add(path);
      }
    }
  }

  /** Performs logical checks on path existence and overwrite conditions after path expansion. */
  @SuppressWarnings("null")
  public List<BusinessViolation> logicalCheckTaskListAfterEnvVarExpansion(AbstractTask task,
      HousekeepFilesTaskRecord rec, HousekeepFilesExpandedPathsInfo pathInfo) {
    List<BusinessViolation> warnList = new ArrayList<>();

    // When the destination is a file, there must be exactly one source.
    if (rec.getIsDestPathDir() != null && rec.getIsDestPathDir() == false
        && pathInfo.fromFileList.size() > 1) {
      new Violations().add(new BusinessViolation(
          "MSG_ERR_FROM_PATH_MUST_BE_ONLY_ONE_WHEN_TO_PATH_IS_FILE",
          rec.getTaskId(), rec.getTaskName())).throwIfAny();
    }

    // Check that the source path exists.
    if (rec.getSrcPath() != null && task.isSrcPathLocal() != null && task.isSrcPathLocal()
        && pathInfo.fromFileList.size() == 0) {
      if (rec.getActionForNoSrcPath() == IncidentTreatedAsEnum.ERROR) {
        new Violations().add(new BusinessViolation("MSG_ERR_FROM_PATH_NOT_EXIST",
            rec.getTaskId(), rec.getTaskName(), rec.getSrcPath())).throwIfAny();
      }

      if (rec.getActionForNoSrcPath() == IncidentTreatedAsEnum.WARN) {
        warnList.add(new BusinessViolation("MSG_ERR_FROM_PATH_NOT_EXIST",
            rec.getTaskId(), rec.getTaskName(), rec.getSrcPath()));
      }
    }

    // Skip task patterns that have no destination.
    // For zip, no destination is specified but overwrite check is still performed,
    // so handle separately.
    if (task.getInputRuleForDestPath() == TaskAttrCheckPtnEnum.REQUIRED) {
      if (task.isDestPathLocal()) {
        // Check that the destination path exists. The destination must be exactly one.
        if (pathInfo.tmpToFileList.size() == 0) {
          new Violations().add(new BusinessViolation("MSG_ERR_TO_PATH_DOESNT_EXIST",
              rec.getTaskId(), rec.getTaskName())).throwIfAny();

        } else if (pathInfo.tmpToFileList.size() > 1) {
          new Violations().add(new BusinessViolation("MSG_ERR_TO_PATH_NOT_ONE",
              rec.getTaskId(), rec.getTaskName())).throwIfAny();
        }
      }

      for (String fromPath : pathInfo.fromFileList) {
        // If the destination file/directory exists, follow the actionForToFileExists setting.
        String toPath = pathInfo.tmpToFileList.get(0);
        boolean doesFileOrDirExists =
            fmu.checkIfToOverwrittenFileOrDirExists(rec, fromPath, toPath);
        if (doesFileOrDirExists) {
          // For directory-to-directory copy where the destination directory already exists,
          // tracking overwrite behavior across nested contents is complex, and copying the full
          // hierarchy is too destructive, so treat it as an unconditional error.
          // Note: if the source is a directory with compression specified, it is treated as a file,
          // so that case is excluded.
          if (rec.getIsSrcPathDir() == true && rec.getIsDestPathDir() == true) {
            new Violations().add(new BusinessViolation(
                "MSG_ERR_TO_DIR_EXISTS_AND_COPY_SETTING_VAGUE",
                rec.getTaskId(), rec.getTaskName())).throwIfAny();
          } else {
            BusinessViolation blV = new BusinessViolation("MSG_ERR_DEST_PATH_EXISTSS",
                rec.getTaskId(), rec.getTaskName(), toPath);
            if (rec.getActionForDestFileExists() == IncidentTreatedAsEnum.ERROR) {
              new Violations().add(blV).throwIfAny();
            }

            if (rec.getActionForDestFileExists() == IncidentTreatedAsEnum.WARN) {
              warnList.add(blV);
            }
          }
        }
      }
    }

    return warnList;
  }

  /** Executes the task for all matched source files using the expanded path info. */
  public void doTaskForMultipleFiles(HousekeepFilesTaskRecord taskRec,
      HousekeepFilesExpandedPathsInfo pathInfo, @Nullable ConnectionToRemoteServer conn,
      List<BusinessViolation> warnList) throws Exception {
    // Log output.
    logTaskStartMsg(taskRec);

    AbstractTask task = getTaskInstance(taskRec);

    if (task.getTaskActionKind() == TaskActionKindEnum.create) {
      task.doTask(conn, taskRec, taskRec.getEnvVarExpandedSrcPath(), pathInfo.toPath, warnList);

    } else {
      // Loop over each source file/directory.
      for (String fromPath : pathInfo.fromFileList) {
        task.doTask(conn, taskRec, fromPath, pathInfo.toPath, warnList);
      }
    }
  }

  private void logTaskStartMsg(HousekeepFilesTaskRecord rec) {
    SplibLogUtil.debug(dlog, "- taskName              : " + rec.getTaskName(), 1);
    SplibLogUtil.debug(dlog, "- taskPtn               : " + rec.getTaskPtn(), 1);
    SplibLogUtil.debug(dlog, "- remoteServer          : " + rec.getRemoteServer(), 1);
    SplibLogUtil.debug(dlog, "- pathFrom              : " + rec.getSrcPath(), 1);
    SplibLogUtil.debug(dlog, "- isSrcPathDir          : " + rec.getIsSrcPathDir(), 1);
    SplibLogUtil.debug(dlog, "- value                 : " + rec.getValue(), 1);
    SplibLogUtil.debug(dlog, "- getActionForNoSrcPath : " + rec.getActionForNoSrcPath(), 1);
    SplibLogUtil.debug(dlog, "- pathTo                : " + rec.getDestPath(), 1);
    SplibLogUtil.debug(dlog, "- isDestPathDir         : " + rec.getIsDestPathDir(), 1);
    SplibLogUtil.debug(dlog, "- doesOverwriteDestPath : " + rec.getDoesOverwriteDestPath(), 1);
    SplibLogUtil.debug(dlog, "- actionForToFileExists : " + rec.getActionForDestFileExists(), 1);
  }

  private void logWithTaskId(String taskId, String msg) {
    dlog.debug("[" + taskId + "] " + msg);
  }

  /** Creates and returns a task instance by reflection based on the task pattern in the record. */
  protected AbstractTask getTaskInstance(HousekeepFilesTaskRecord taskRec)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
      SecurityException {
    AbstractTask task;
    String taskName = StringUtil.getUpperCamelFromSnake(taskRec.getTaskPtn().toString());
    @SuppressWarnings("unchecked")
    Class<AbstractTask> cls =
        (Class<AbstractTask>) Class.forName(Constants.PACKAGE_HK_TASK + "." + taskName);
    task = cls.getDeclaredConstructor().newInstance();
    return task;
  }

  /**
   * Sends a warning email listing all accumulated violations to the configured recipients.
   *
   * @param systemName optional system name (from {@link Constants#PROP_SYSTEM_NAME}) appended to
   *     the email subject; may be {@code null}, in which case it's simply omitted.
   */
  public void sendWarnMail(List<BusinessViolation> warnList, @Nullable String systemName)
      throws Exception {
    // Retrieve the list of error messages.
    List<String> msgList = new ArrayList<>();
    Violations warnViolations = new Violations();
    warnList.forEach(warnViolations::add);
    try {
      warnViolations.throwIfAny();
    } catch (ViolationException ex) {
      msgList.addAll(ExceptionUtil.getMessageList(ex, Locale.JAPANESE));
    }

    // Build the message.
    final String title = PropertiesFileUtil.getApplication("jp.ecuacion.lib.core.mail.title-prefix")
        + "[WARN] HousekeepFiles" + (systemName == null ? "" : ":" + systemName);
    String hostname = InetAddress.getLocalHost().getHostName();
    StringBuilder msg = new StringBuilder();
    msg.append("hostname: " + hostname + "\n\n" + "You've got warnings: \n\n");
    for (String additionalMsg : msgList) {
      msg.append("- " + additionalMsg + "\n");
    }

    // Log output.
    dlog.debug(msg.toString());
    // Send email.
    List<String> mailTo = new ArrayList<String>();
    for (String to : PropertiesFileUtil
        .getApplication("jp.ecuacion.lib.core.mail.address-csv-on-system-error").split(",", -1)) {
      mailTo.add(to);
    }

    MailUtil.sendTextMail(mailTo, null, title, msg.toString());
  }
}
