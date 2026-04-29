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
import static jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum.change;
import static jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum.create;
import static jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum.createFromOriginal;
import static jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum.delete;

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
import org.jspecify.annotations.Nullable;

/**
 * @author yosuk_000
 *
 */
@SuppressWarnings("NullAway.Init")
public abstract class AbstractTask {

  protected TaskPtnEnum taskPtn;

  // 各タスクに対して、xml上の項目の入力必須・禁止・任意を規定
  protected TaskAttrCheckPtnEnum inputRuleForSrcPathInfo;
  protected TaskAttrCheckPtnEnum inputRuleForDestPathInfo;

  /**
   * その他、taskの動作にかかわる属性を定義。
   * 以下は、zip・unzipを想定し「xml上はTOを指定しないが、結果的にTOファイルができるもの」を表す。デフォルトではfalseとし、対象のタスクのみこの値を書き換えるものとする
   */
  protected boolean doesCreateOutputFileAutomatically = false;

  protected DetailLogger dlog = new DetailLogger(this);

  public AbstractTask() {
    TaskActionKindEnum taskActionKind = getTaskActionKind();

    // taskActionKindから設定可能な値を設定。例外的に異なるパターンが発生する場合は個別タスク側で上書き。
    if (taskActionKind == create) {
      inputRuleForSrcPathInfo = PROHIBITED;
      inputRuleForDestPathInfo = REQUIRED;

    } else if (taskActionKind == change) {
      inputRuleForSrcPathInfo = REQUIRED;
      inputRuleForDestPathInfo = REQUIRED;

    } else if (taskActionKind == delete) {
      inputRuleForSrcPathInfo = REQUIRED;
      inputRuleForDestPathInfo = PROHIBITED;

    } else if (taskActionKind == createFromOriginal) {
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

  public boolean doesCreateOutputFileAutomatically() {
    return doesCreateOutputFileAutomatically;
  }

  /** taskに対するHousekeepFilesTaskRecordのinput情報の内容チェック。task実行の前にcheckをする関係でdoTaskとは分けている。 */
  public void check(HousekeepFilesTaskRecord dtRec) {
    Violations violations = new Violations();
    checkNeedRemoteServer(dtRec, violations);
    checkRequiredOrProhibited(violations, dtRec);

    // ここまででnull値項目の正当性含めinput validation済み。
    // この後の処理でnullであるべきでない値がnullなのを気にしてnull条件を書くのは煩雑なので、一旦エラーがあれば返しておく。
    violations.throwIfAny();

    taskDependentCheck(dtRec, violations);

    violations.throwIfAny();
  }

  /** checkから呼び出される。各taskにてチェック実装。 */
  public abstract void taskDependentCheck(HousekeepFilesTaskRecord taskRec,
      Violations violations);

  protected void checkNeedRemoteServer(HousekeepFilesTaskRecord taskRec,
      Violations violations) {
    boolean containsRemoteAction = isSrcPathLocal() != null && !isSrcPathLocal()
        || isDestPathLocal() != null && !isDestPathLocal();

    checkTaskItemNoThrow(violations, taskRec.getTaskId(), taskPtn,
        containsRemoteAction ? TaskAttrCheckPtnEnum.REQUIRED : TaskAttrCheckPtnEnum.PROHIBITED,
        "remoteServer", taskRec.getRemoteServer());
  }

  private void checkRequiredOrProhibited(Violations violations,
      HousekeepFilesTaskRecord taskRec) {

    // 元パス関連。元パスが全入力または全空欄なのは事前に確認済みなので、ここでは代表でsrcPathのみをチェック
    checkTaskItemNoThrow(violations, taskRec.getTaskId(), taskPtn, getInputRuleForSrcPath(),
        "srcPath", taskRec.getSrcPath());

    // 先パス関連。先パスが全入力または全空欄なのは事前に確認済みなので、ここでは代表でdestPathのみをチェック
    checkTaskItemNoThrow(violations, taskRec.getTaskId(), taskPtn, getInputRuleForDestPath(),
        "destPath", taskRec.getDestPath());
  }

  void checkTaskItemNoThrow(Violations violations, String taskId, TaskPtnEnum taskPtn,
      TaskAttrCheckPtnEnum checkPtn, String itemTitle, Object itemValue) {
    checkTaskItem(violations, taskId, taskPtn, checkPtn, itemTitle, itemValue);
  }

  void checkTaskItem(Violations violations, String taskId, TaskPtnEnum taskPtn,
      TaskAttrCheckPtnEnum checkPtn, String itemTitle, Object itemValue) {
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

  /** taskの種類を指定。それによりfrom, toの入力要否などを設定。 */
  public abstract TaskActionKindEnum getTaskActionKind();

  /** "SFTP"などのprotocolを設定。localの場合はnull。 */
  public abstract @Nullable String getConnectionProtocol();

  /** それぞれのremote通信に対するconnection。 */
  public abstract @Nullable ConnectionToRemoteServer getConnection(String remoteServer,
      Map<String, HousekeepFilesAuthRecord> authMap) throws Exception;


  public abstract @Nullable Boolean isSrcPathLocal();

  public abstract @Nullable Boolean isDestPathLocal();

  protected abstract @Nullable FileInfo getRemoteFileInfo(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path);

  protected abstract @Nullable List<FileInfo> getRemoteFileInfoList(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path);

  /** task実行。外部からtaskを実行する際はこれを呼ぶ。 */
  public void doTask(@Nullable ConnectionToRemoteServer conn, HousekeepFilesTaskRecord taskRec,
      @Nullable String srcPath, @Nullable String destPath, List<BusinessViolation> warnList)
      throws Exception {
    doTaskInternal(conn, taskRec, srcPath, destPath, warnList);
  }

  /** doTaskから呼び出される。各taskにて処理実装。 */
  protected abstract void doTaskInternal(@Nullable ConnectionToRemoteServer conn,
      HousekeepFilesTaskRecord taskRec, @Nullable String srcPath, @Nullable String destPath,
      List<BusinessViolation> warnList) throws Exception;

  /** taskが元パス情報を保持しているかをbooleanで返す。 */
  public boolean hasSrcPathInfo() {
    if (getTaskActionKind() == create) {
      return false;

    } else {
      return true;
    }
  }

  /** taskが先パス情報を保持しているかをbooleanで返す。 */
  public boolean hasDestPathInfo() {
    if (getTaskActionKind() == delete) {
      return false;

    } else {
      return true;
    }
  }

  public @Nullable FileInfo getToPathFileInfo(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path)
      throws Exception {
    if (Objects.requireNonNull(isDestPathLocal())) {
      return getLocalFileInfo(path);

    } else {
      return getRemoteFileInfo(task, connection, isPathDir, path);
    }
  }

  public @Nullable List<FileInfo> getFromDirFileInfoList(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path) {
    if (Objects.requireNonNull(isSrcPathLocal())) {
      return getLocalFileInfoList(path);

    } else {
      return getRemoteFileInfoList(task, connection, isPathDir, path);
    }
  }

  // local用処理を本クラスに記載

  protected boolean localDirExists(String dirPath) {
    return new File(dirPath).exists() && new File(dirPath).isDirectory();
  }

  /** ローカルディスク上の一覧取得。 */
  protected @Nullable FileInfo getLocalFileInfo(String path) {
    File file = new File(path);

    // 存在しない場合はnullを返す
    if (!file.exists()) {
      return null;
    }

    FileInfo fi = new FileInfo();
    fi.setFilePath(path);
    fi.setDirectory(file.isDirectory());
    fi.setLastUpdTimeInMillis(file.lastModified());
    // ロックをチェック
    try {
      FileUtil.isLocked(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return fi;
  }

  /** ローカルディスク上の一覧取得。 */
  protected List<FileInfo> getLocalFileInfoList(String path) {
    List<FileInfo> rtnList = new ArrayList<FileInfo>();
    List<String> pathStrList = FileUtil.getPathListFromPathWithWildcard(path);
    for (String strPath : pathStrList) {
      FileInfo fi = new FileInfo();
      File fileObj = new File(strPath);
      fi.setFilePath(strPath);
      fi.setDirectory(fileObj.isDirectory());
      fi.setLastUpdTimeInMillis(fileObj.lastModified());
      // ロックをチェック
      try {
        FileUtil.isLocked(strPath);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      rtnList.add(fi);
    }
    return rtnList;
  }

  /** 設定がwarnかerrorかで処理を分ける。 */
  protected void treatIncident(@Nullable IncidentTreatedAsEnum pattern, BusinessViolation violation,
      List<BusinessViolation> warnList) {
    if (pattern == IncidentTreatedAsEnum.WARN) {
      warnList.add(violation);

    } else if (pattern == IncidentTreatedAsEnum.ERROR) {
      new Violations().add(violation).throwIfAny();
    }
  }

  protected void treatDestPathExists(HousekeepFilesTaskRecord taskRec, String destPath,
      List<BusinessViolation> warnList) {
    treatIncident(taskRec.getActionForDestFileExists(),
        new BusinessViolation("MSG_ERR_DEST_PATH_EXISTS", taskRec.getTaskId(),
            taskRec.getTaskName(), destPath),
        warnList);
  }
}
