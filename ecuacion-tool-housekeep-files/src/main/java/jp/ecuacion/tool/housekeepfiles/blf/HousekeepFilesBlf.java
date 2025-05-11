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
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bl.HousekeepFilesBl;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTask;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTaskLocal;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.other.HousekeepFilesExpandedPathsInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;

public class HousekeepFilesBlf {
  DetailLogger dlog = new DetailLogger(this);
  HousekeepFilesBl bl = new HousekeepFilesBl();

  public HousekeepFilesBlf() {

  }

  /** test用。 */
  public HousekeepFilesBlf(HousekeepFilesBl bl) {
    this.bl = bl;
  }

  public void execute(HousekeepFilesForm form) throws Exception {
    // ログ出力
    logJobStartMsg(form);

    // WARN情報を保持するリスト
    List<AppException> warnList = new ArrayList<AppException>();

    // 複数レコード・複数データ種別間のチェック
    bl.consistencyCheckBetweenMultipleData(form);

    // envVarInfoをMap形式で作成
    Map<String, String> envVarInfoMap = bl.createPathInfoMap(form);

    // authInfoをmap形式で作成。キーは"<サーバ名>-<プロトコル>"とする
    Map<String, HousekeepFilesAuthRecord> authMap = form.getAuthInfoRecList().stream().collect(
        Collectors.toMap(rec -> rec.getRemoteServer() + "-" + rec.getProtocol(), rec -> rec));

    // srcPath, destPathに指定している環境変数のenvVarInfoMap存在チェックと環境変数展開済みパスの設定
    bl.envVarExistenceCheckAndSetEnvBarExpandedPaths(form.getTaskInfoHdRec().recList,
        envVarInfoMap);

    // 以下、task別処理。
    // 本来は以降は一つのループで回したいのだが、taskごとのexcel記載に対する詳細チェックがタスク別にエラー表示されると面倒、
    // 全タスク分まとめて先にチェック・エラーメッセージ表示したいことから先にタスク生成とチェックのみ実施。
    // ここでは実際のファイル・フォルダの有無などはチェックしていない。あくまで指定taskとそれに対するexcel記述の整合性をチェック。
    List<SingleAppException> exList = new ArrayList<>();
    bl.createTaskAndTaskDependentCheck(form, exList);
    if (exList.size() > 0) {
      throw new MultipleAppException(exList);
    }

    // 複数のconnectionを格納するためのMap
    Map<String, ConnectionToRemoteServer> connectionMap = new HashMap<>();
    try {
      // タスク実行
      for (HousekeepFilesTaskRecord taskInfo : form.getTaskInfoHdRec().recList) {
        execEachTask(taskInfo.task, connectionMap, taskInfo, envVarInfoMap, authMap, warnList);
      }

    } finally {
      // connecitonをclose
      for (Entry<String, ConnectionToRemoteServer> entry : connectionMap.entrySet()) {
        entry.getValue().closeConnection();
      }
    }

    // ワーニングがあればメール
    if (!warnList.isEmpty()) {
      bl.sendWarnMail(warnList, form.getTaskInfoHdRec());
    }

    // ログ出力
    logJobFinishMsg(form);
  }

  private void logJobStartMsg(HousekeepFilesForm form) {
    dlog.debug("####################");
    dlog.debug("##### startJob :" + form.getTaskInfoHdRec().getSysName());
  }

  private void logJobFinishMsg(HousekeepFilesForm form) {
    dlog.debug("##### finishJob:" + form.getTaskInfoHdRec().getSysName());
  }

  protected void execEachTask(AbstractTask task,
      Map<String, ConnectionToRemoteServer> connectionMap, HousekeepFilesTaskRecord taskInfo,
      Map<String, String> envVarInfoMap, Map<String, HousekeepFilesAuthRecord> authMap,
      List<AppException> warnList) throws Exception {

    // 保持してなければconnection取得
    final String connectionKey = taskInfo.getRemoteServer() + "." + task.getConnectionProtocol();
    if (!(task instanceof AbstractTaskLocal)) {
      if (!connectionMap.containsKey(connectionKey)) {
        connectionMap.put(connectionKey, task.getConnection(taskInfo.getRemoteServer(), authMap));
      }
    }

    // 本タスクで使用するconnectionを取得
    ConnectionToRemoteServer conn = connectionMap.get(connectionKey);

    // ${VAR}および、PATHに含まれるワイルドカードを展開
    HousekeepFilesExpandedPathsInfo pathInfo =
        bl.expandAllPath(task, taskInfo, envVarInfoMap, conn);

    // チェックが通ったのでpathInfoMapのtoPathを埋める
    // toが指定されないtaskPtn（削除、zip）の場合は、pathInfo.tmpToFileListが0件となるのでそれを考慮
    if (pathInfo.tmpToFileList.size() > 0) {
      pathInfo.toPath = pathInfo.tmpToFileList.get(0);
    }

    warnList.addAll(bl.logicalCheckTaskListAfterEnvVarExpansion(task, taskInfo, pathInfo));

    // 処理を実行
    bl.doTaskForMultipleFiles(taskInfo, pathInfo, conn, warnList);
  }
}
