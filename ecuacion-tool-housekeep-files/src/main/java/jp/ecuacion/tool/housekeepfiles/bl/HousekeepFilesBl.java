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
import java.util.Set;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.lib.core.util.MailUtil;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.lib.core.util.ValidationUtil;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTask;
import jp.ecuacion.tool.housekeepfiles.bl.task.TaskAttrCheckPtnEnum;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.other.FileInfo;
import jp.ecuacion.tool.housekeepfiles.dto.other.HousekeepFilesExpandedPathsInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesHdRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesPathRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.IncidentTreatedAsEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.tool.housekeepfiles.util.DateTimeUtil;
import jp.ecuacion.tool.housekeepfiles.util.HkFileManipulateUtil;
import jp.ecuacion.tool.housekeepfiles.util.ParameterUtil;

public class HousekeepFilesBl extends AbstractBl {
  public static final String EXTENSION_NONE_WITH_DOT = "";
  public static final String EXTENSION_ZIP_WITH_DOT = ".zip";

  private DetailLogger dlog = new DetailLogger(this);
  private ParameterUtil pu = new ParameterUtil();
  private DateTimeUtil dateUtil = new DateTimeUtil();
  private HkFileManipulateUtil fmu = new HkFileManipulateUtil();


  public void consistencyCheckBetweenMultipleData(HousekeepFilesForm form) throws AppException {
    // taskInfoHdRecはreaderで読み込んでいない＝validation checkが動いていないので実施。
    // 実質sysNameの存在チェック。
    ValidationUtil.validateThenThrow(form.getTaskInfoHdRec());

    // taskがゼロの場合はエラー
    if (form.getTaskInfoHdRec().recList == null || form.getTaskInfoHdRec().recList.size() == 0) {
      throw new BizLogicAppException("MSG_ERR_AT_LEAST_ONE_TASK_NEEDED");
    }

    // taskId, taskNameが重複していないことを確認
    HashSet<String> taskIdSet = new HashSet<String>();
    HashSet<String> taskNameSet = new HashSet<String>();
    for (HousekeepFilesTaskRecord rec : form.getTaskInfoHdRec().recList) {
      // taskId
      if (taskIdSet.contains(rec.getTaskId())) {
        throw new BizLogicAppException("MSG_ERR_TASK_ID_DUPLICATED", rec.getTaskId());

      } else {
        taskIdSet.add(rec.getTaskId());
      }

      // taskName
      if (taskNameSet.contains(rec.getTaskName())) {
        throw new BizLogicAppException("MSG_ERR_TASK_NAME_DUPLICATED", rec.getTaskName());

      } else {
        taskNameSet.add(rec.getTaskName());
      }
    }
  }

  public HashMap<String, String> createPathInfoMap(HousekeepFilesForm form)
      throws UnknownHostException {
    HashMap<String, String> pathInfoMap = new HashMap<String, String>();
    for (HousekeepFilesPathRecord pathInfo : form.getPathInfoRecList()) {
      pathInfoMap.put(pathInfo.getKey(), pathInfo.getValue());
    }

    // defaultで用意される項目を追加
    pathInfoMap.put(Constants.ENV_VAR_SYS_NAME, form.getTaskInfoHdRec().getSysName());
    pathInfoMap.put(Constants.ENV_VAR_DATE, dateUtil.getDateStr8());
    pathInfoMap.put(Constants.ENV_VAR_TIMESTAMP, dateUtil.getTimestampNumString());
    pathInfoMap.put(Constants.ENV_VAR_HOSTNAME, InetAddress.getLocalHost().getHostName());

    return pathInfoMap;
  }

  public void envVarExistenceCheckAndSetEnvBarExpandedPaths(
      List<HousekeepFilesTaskRecord> taskRecList, Map<String, String> envVarInfoMap)
      throws MultipleAppException, BizLogicAppException {
    List<SingleAppException> exArr = new ArrayList<>();

    // pathFrom, pathToのチェック
    for (HousekeepFilesTaskRecord rec : taskRecList) {
      // パスに含まれる${xxx}のxxx値がパスリストに存在するかを確認
      if (rec.getSrcPath() != null) {
        analyzePathVarAndCheckIfExistsInSet(rec, envVarInfoMap.keySet(), exArr, rec.getSrcPath());
      }

      if (rec.getDestPath() != null) {
        analyzePathVarAndCheckIfExistsInSet(rec, envVarInfoMap.keySet(), exArr, rec.getDestPath());
      }

      // 問題なければ、envVarInfoMapをtaskRecに設定することで環境変数展開済みパスを生成
      rec.setEnvVarInfoMap(envVarInfoMap);
    }
  }

  private void analyzePathVarAndCheckIfExistsInSet(HousekeepFilesTaskRecord taskRec,
      Set<String> pathKeySet, List<SingleAppException> exArr, String path)
      throws BizLogicAppException {

    // 一つのパスの中に、複数のパス変数が設定されている場合があるので、ループ処理
    String pathPart = path;
    String envVar = null;

    while (true) {
      // 「${」を含まない場合は終了
      if (!pathPart.contains("${")) {
        break;
      }

      envVar = pu.getFirstUnixEnvVar(pathPart);
      pathPart = pathPart.substring(pathPart.indexOf(envVar) + envVar.length() + 1);
      // pathKeySetの中にpathVarが存在しない場合はエラー
      // ただし、TASK_NAME、YYYYMMDD、TIMESTAMPという名前は固定でエラーとしないようにする（SYS_NAMEは既に追加済み）
      if (!pathKeySet.contains(envVar) && !envVar.equals(Constants.ENV_VAR_TASK_NAME)
          && !envVar.equals(Constants.ENV_VAR_DATE) && !envVar.equals(Constants.ENV_VAR_TIMESTAMP)
          && !envVar.equals(Constants.ENV_VAR_HOSTNAME)) {
        exArr.add(
            new BizLogicAppException("MSG_ERR_PATH_VAR_NOT_EXIST", taskRec.getTaskName(), envVar));
      }
    }
  }

  public void createTaskAndTaskDependentCheck(HousekeepFilesForm form,
      List<SingleAppException> exList) throws Exception {
    for (HousekeepFilesTaskRecord dtRec : form.getTaskInfoHdRec().recList) {
      createTaskInstance(exList, dtRec, dtRec.getTaskPtn());
      // task別の入力必須・禁止のチェック処理
      try {
        dtRec.task.check(dtRec);
      } catch (AppException ex) {
        if (ex instanceof MultipleAppException) {
          exList.addAll(((MultipleAppException) ex).getList());

        } else {
          exList.add(((SingleAppException) ex));
        }
      }
    }
  }

  public void createTaskInstance(List<SingleAppException> exList, HousekeepFilesTaskRecord dtRec,
      TaskPtnEnum taskPtn) throws Exception {
    @SuppressWarnings("unchecked")
    Class<AbstractTask> cls = (Class<AbstractTask>) Class.forName(Constants.PACKAGE_HK_TASK + "."
        + StringUtil.getUpperCamelFromSnake(taskPtn.getName()));
    dtRec.task = cls.getDeclaredConstructor().newInstance();
  }

  public HousekeepFilesExpandedPathsInfo expandAllPath(AbstractTask task,
      HousekeepFilesTaskRecord taskRec, Map<String, String> envVarInfoMap,
      ConnectionToRemoteServer connection) throws Exception {

    List<String> fromPathList = new ArrayList<String>();
    List<String> toPathList = new ArrayList<String>();

    // src
    if (task.hasSrcPathInfo()) {
      expandFromPath(envVarInfoMap, connection, taskRec, task, fromPathList);
    }

    // dest
    if (task.hasDestPathInfo()) {
      expandToPath(envVarInfoMap, connection, taskRec, task, toPathList);
    }

    // toについては、本来一つでなければならないが、ここではチェックせずArrayListのまま保持させる
    return new HousekeepFilesExpandedPathsInfo(fromPathList, toPathList);
  }

  private void expandFromPath(Map<String, String> envVarInfoMap,
      ConnectionToRemoteServer connection, HousekeepFilesTaskRecord taskRec, AbstractTask task,
      List<String> fromPathList) throws AppException {

    List<FileInfo> tmpFromFileAndDirMixedList = task.getFromDirFileInfoList(task, connection,
        taskRec.getIsSrcPathDir(), taskRec.getEnvVarExpandedSrcPath());
    for (FileInfo fi : tmpFromFileAndDirMixedList) {
      // isSrcPathDirと実際のパスの内容（ファイルかディレクトリか）が一致している場合のみを対象に残すものとするので、そうでない場合はスキップ
      if (!(taskRec.getIsSrcPathDir() == true) == fi.isDirectory()) {
        continue;

      }
      // さらに最終更新日の条件を満たすもののみを対象とする。必要な期間が経過していないものは除外。
      if (!dateUtil.hasDesignatedTermPassed(fi.getLastUpdTimeInMillis(), taskRec.getUnit(),
          taskRec.getValue())) {
        continue;
      }

      // ファイルにロックがかかっている場合はリストに追加せず、ワーニングのログのみ出力する
      // これは、対応しているプロトコルとそうでないものがある（現時点では、ローカルファイルシステムにのみ対応）
      if (fi.isLocked()) {
        logWithTaskId(taskRec.getTaskId(), "ファイルがロックされているためスキップします：" + fi.getFilePath());
        continue;
      }

      // リストに追加
      fromPathList.add(fi.getFilePath());
    }
  }

  private void expandToPath(Map<String, String> envVarInfoMap, ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, AbstractTask task, List<String> toPathList)
      throws Exception {
    // to
    String varSubstitutedToPath = taskRec.getEnvVarExpandedDestPath();

    List<String> tmpToFileAndDirMixedList = null;
    // ワイルドカードが存在するかどうかで処理を分ける
    // ワイルドカードが存在しない場合は、ディレクトリを自動生成する
    if (!FileUtil.containsWildCard(varSubstitutedToPath)) {
      // fu.getPathListFromPathWithWildcardにはclean処理が含まれるが、こちらの場合は通らないので個別に呼び出しておく
      varSubstitutedToPath = FileUtil.cleanPathStrWithSlash(varSubstitutedToPath);
      tmpToFileAndDirMixedList = new ArrayList<String>();
      tmpToFileAndDirMixedList.add(varSubstitutedToPath);

      // ディレクトリがない場合は作成する
      String dirPath = (taskRec.getIsDestPathDir() != null && taskRec.getIsDestPathDir() == true)
          ? varSubstitutedToPath
          : new File(varSubstitutedToPath).getParent();

      // 相対パスの場合は絶対パスに書き換え
      if (dirPath != null && !dirPath.startsWith("/")) {
        String dirPathTmp = dirPath.startsWith("./") ? dirPath.substring(2) : dirPath;
        dirPath = FileUtil.concatFilePaths(System.getProperty("user.dir"), dirPathTmp);
      }

    } else {
      tmpToFileAndDirMixedList = FileUtil.getPathListFromPathWithWildcard(varSubstitutedToPath);
    }

    for (String path : tmpToFileAndDirMixedList) {
      // TOのpathのファイル・ディレクトリが既に存在している場合
      FileInfo dirInfo = task.getToPathFileInfo(task, connection, true, path);
      FileInfo fileInfo = task.getToPathFileInfo(task, connection, false, path);
      if (dirInfo == null && fileInfo == null) {
        toPathList.add(path);

      } else {
        // isDestPathDirと実際のパスの内容（ファイルかディレクトリか）が一致している場合のみtoPathListに追加
        if (dirInfo != null && taskRec.getIsDestPathDir()
            || fileInfo != null && !taskRec.getIsDestPathDir()) {
          toPathList.add(path);
        }
        // if ((dtE.getIsDestPathDir() == true) == task.getToPathFileInfo(connection,
        // path).isDirectory()) toPathList.add(path);
      }
    }
  }

  public List<AppException> logicalCheckTaskListAfterEnvVarExpansion(AbstractTask task,
      HousekeepFilesTaskRecord rec, HousekeepFilesExpandedPathsInfo pathInfo)
      throws BizLogicAppException {
    List<AppException> warnList = new ArrayList<AppException>();

    // toがファイルの場合は、fromもひとつでなければならない
    if (rec.getIsDestPathDir() != null && rec.getIsDestPathDir() == false
        && pathInfo.fromFileList.size() > 1) {
      throw new BizLogicAppException("MSG_ERR_FROM_PATH_MUST_BE_ONLY_ONE_WHEN_TO_PATH_IS_FILE",
          rec.getTaskId(), rec.getTaskName());
    }

    // fromの存在チェックを行う
    if (rec.getSrcPath() != null && task.isSrcPathLocal() != null && task.isSrcPathLocal()
        && pathInfo.fromFileList.size() == 0) {
      if (rec.getActionForNoSrcPath() == IncidentTreatedAsEnum.ERROR) {
        throw new BizLogicAppException("MSG_ERR_FROM_PATH_NOT_EXIST", rec.getTaskId(),
            rec.getTaskName(), rec.getSrcPath());
      }

      if (rec.getActionForNoSrcPath() == IncidentTreatedAsEnum.WARN) {
        warnList.add(new BizLogicAppException("MSG_ERR_FROM_PATH_NOT_EXIST", rec.getTaskId(),
            rec.getTaskName(), rec.getSrcPath()));
      }
    }

    // toが存在しないtaskPtnの場合はスキップ。また、zipの場合はtoは記載しないがoverwriteチェックだけ行うので別だし
    if (task.getInputRuleForDestPath() == TaskAttrCheckPtnEnum.REQUIRED) {
      if (task.isDestPathLocal()) {
        // toの存在チェックを行う。toは、一つでなければならない
        if (pathInfo.tmpToFileList.size() == 0) {
          throw new BizLogicAppException("MSG_ERR_TO_PATH_DOESNT_EXIST", rec.getTaskId(),
              rec.getTaskName());

        } else if (pathInfo.tmpToFileList.size() > 1) {
          throw new BizLogicAppException("MSG_ERR_TO_PATH_NOT_ONE", rec.getTaskId(),
              rec.getTaskName());
        }
      }

      for (String fromPath : pathInfo.fromFileList) {
        // toのファイル・ディレクトリが存在する場合は、actionForToFileExistsの設定に従う。
        String toPath = pathInfo.tmpToFileList.get(0);
        boolean doesFileOrDirExists =
            fmu.checkIfToOverwrittenFileOrDirExists(rec, fromPath, toPath);
        if (doesFileOrDirExists) {
          // ディレクトリからディレクトリへのコピーで、かつディレクトリ名が先に存在する、となると、その下のコピー内容が、上書きしたりなんだりを追うのが大変だし、
          // それら下の階層を含めすべてのコピーをするのはそもそも処理として非常に乱暴なので、有無を言わさずエラーとする
          // 尚、fromがディレクトリでcompress指定がある場合は、結局ファイルとしての扱いになるのでその場合は除外
          if (rec.getIsSrcPathDir() == true && rec.getIsDestPathDir() == true) {
            throw new BizLogicAppException("MSG_ERR_TO_DIR_EXISTS_AND_COPY_SETTING_VAGUE",
                rec.getTaskId(), rec.getTaskName());
          } else {
            BizLogicAppException blEx = new BizLogicAppException("MSG_ERR_DEST_PATH_EXISTSS",
                rec.getTaskId(), rec.getTaskName(), toPath);
            if (rec.getActionForDestFileExists() == IncidentTreatedAsEnum.ERROR) {
              throw blEx;
            }

            if (rec.getActionForDestFileExists() == IncidentTreatedAsEnum.WARN) {
              warnList.add(blEx);
            }
          }
        }
      }
    }

    return warnList;
  }

  public void doTaskForMultipleFiles(HousekeepFilesTaskRecord taskRec,
      HousekeepFilesExpandedPathsInfo pathInfo, ConnectionToRemoteServer conn,
      List<AppException> warnList) throws Exception {
    // ログ
    logTaskStartMsg(taskRec);

    AbstractTask task = getTaskInstance(taskRec);

    if (task.getTaskActionKind() == TaskActionKindEnum.create) {
      task.doTask(conn, taskRec, taskRec.getEnvVarExpandedSrcPath(), pathInfo.toPath, warnList);

    } else {
      // fromのファイル・ディレクトリごとにループ
      for (String fromPath : pathInfo.fromFileList) {
        task.doTask(conn, taskRec, fromPath, pathInfo.toPath, warnList);
      }
    }

    // ログ
    logTaskFinishMsg(taskRec, pathInfo);
  }

  private void logTaskStartMsg(HousekeepFilesTaskRecord rec) throws BizLogicAppException {
    String taskId = rec.getTaskId();
    dlog.debug("### startTask  :" + taskId);
    logWithTaskId(taskId, "taskName              = " + rec.getTaskName());
    logWithTaskId(taskId, "taskPtn               = " + rec.getTaskPtn());
    logWithTaskId(taskId, "remoteServer          = " + rec.getRemoteServer());
    logWithTaskId(taskId, "pathFrom              = " + rec.getSrcPath());
    logWithTaskId(taskId, "isSrcPathDir         = " + rec.getIsSrcPathDir());
    logWithTaskId(taskId, "unit                  = " + rec.getUnit());
    logWithTaskId(taskId, "value                 = " + rec.getValue());
    logWithTaskId(taskId, "actionForNoSrcPath   = " + rec.getActionForNoSrcPath());
    logWithTaskId(taskId, "pathTo                = " + rec.getDestPath());
    logWithTaskId(taskId, "isDestPathDir           = " + rec.getIsDestPathDir());
    logWithTaskId(taskId, "doesOverwriteDestPath   = " + rec.getDoesOverwriteDestPath());
    logWithTaskId(taskId, "actionForToFileExists = " + rec.getActionForDestFileExists());
    logWithTaskId(taskId, "options               = " + rec.options);
  }

  private void logTaskFinishMsg(HousekeepFilesTaskRecord taskRec,
      HousekeepFilesExpandedPathsInfo pathInfo) {
    dlog.debug("### finishTask :" + taskRec.getTaskId() + " | 処理ファイル／ディレクトリ数:"
        + pathInfo.fromFileList.size());
  }

  private void logWithTaskId(String taskId, String msg) {
    dlog.debug("[" + taskId + "] " + msg);
  }

  protected AbstractTask getTaskInstance(HousekeepFilesTaskRecord taskRec)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
      SecurityException {
    AbstractTask task;
    String taskName =
        StringUtil.getUpperCamelFromSnake(taskRec.getTaskPtn().getName());
    @SuppressWarnings("unchecked")
    Class<AbstractTask> cls =
        (Class<AbstractTask>) Class.forName(Constants.PACKAGE_HK_TASK + "." + taskName);
    task = cls.getDeclaredConstructor().newInstance();
    return task;
  }

  public void sendWarnMail(List<AppException> warnList, HousekeepFilesHdRecord hdE)
      throws Exception {
    // エラーメッセージの一覧を取得
    List<String> msgList = new ArrayList<>();
    for (AppException ae : warnList) {
      msgList.addAll(ExceptionUtil.getAppExceptionMessageList(ae, Locale.JAPANESE));
    }

    // メッセージを作成
    final String title = PropertyFileUtil.getApp("jp.ecuacion.lib.core.mail.title-prefix")
        + "[WARN] HousekeepFiles:" + hdE.getSysName();
    String hostname = InetAddress.getLocalHost().getHostName();
    StringBuilder msg = new StringBuilder();
    msg.append("hostname: " + hostname + "\n\n" + "You've got warnings: \n\n");
    for (String additionalMsg : msgList) {
      msg.append("- " + additionalMsg + "\n");
    }

    // ログ
    dlog.debug(msg.toString());
    // メール送信
    List<String> mailTo = new ArrayList<String>();
    for (String to : PropertyFileUtil
        .getApp("jp.ecuacion.lib.core.mail.address-csv-on-system-error").split(",")) {
      mailTo.add(to);
    }

    MailUtil.sendMail(mailTo, null, title, msg.toString());
  }
}
