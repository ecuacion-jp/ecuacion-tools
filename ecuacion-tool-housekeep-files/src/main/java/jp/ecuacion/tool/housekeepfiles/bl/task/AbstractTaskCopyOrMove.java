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

import java.io.File;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.tool.housekeepfiles.util.HkFileManipulateUtil;

public abstract class AbstractTaskCopyOrMove extends AbstractTaskLocal {
  private HkFileManipulateUtil fmu = new HkFileManipulateUtil();

  @Override
  public TaskActionKindEnum getTaskActionKind() {
    return TaskActionKindEnum.change;
  }

  protected abstract void doSpecificTask(HousekeepFilesTaskRecord taskRec, String fromPath,
      String toPath, TaskPtnEnum taskPtn, boolean isFromDir, boolean isToDir);

  @Override
  public void taskDependentCheck(HousekeepFilesTaskRecord taskRec,
      List<SingleAppException> exList) {

  }

  @Override
  protected void doTaskInternal(ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, String srcPath, String destPath,
      List<AppException> warnList) throws BizLogicAppException {
    TaskPtnEnum taskPtn = taskRec.getTaskPtn();
    boolean doesOverwrittenFileOrDirExist = true;

    // 先にto側に上書きするファイルが存在するかを確認する。例外が上がるなら既に前回本メソッドを読んだ際に発生しているはずなので、ここではエラーが出ない前提でExceptionを握りつぶす
    try {
      doesOverwrittenFileOrDirExist =
          fmu.checkIfToOverwrittenFileOrDirExists(taskRec, srcPath, destPath);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // from, toそれぞれがディレクトリかファイルかのフラグを持っておく。この時点で、圧縮した場合はfromは結局ファイルになるのでそれを加味した状態の判断結果としておく
    boolean isFromDir = (taskRec.getIsSrcPathDir() == true);
    boolean isToDir = (taskRec.getIsDestPathDir() == true);

    // 上書きファイルがある場合
    if (doesOverwrittenFileOrDirExist) {
      if (taskRec.getDoesOverwriteDestPath() == false) {
        // 上書きしない設定の場合は終了
        return;
      } else {
        // 上書きする場合は、先のファイルを削除しておかないと「FileExistsExceptionが発生してしまうため削除
        String toFilePath =
            (isToDir) ? FileUtil.concatFilePaths(destPath, new File(srcPath).getName()) : destPath;
        new File(toFilePath).delete();
      }
    }

    doSpecificTask(taskRec, srcPath, destPath, taskPtn, isFromDir, isToDir);
  }
}
