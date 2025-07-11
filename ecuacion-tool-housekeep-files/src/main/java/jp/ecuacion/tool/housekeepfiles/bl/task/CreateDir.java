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
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bl.task.internal.CreateDirInterface;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;

/**
 * Provides create directory task.
 */
public class CreateDir extends AbstractTaskLocal implements CreateDirInterface {

  /**
   * Constructs a new instance.
   */
  public CreateDir() {
    taskPtn = TaskPtnEnum.CREATE_FILE;
  }

  @Override
  public TaskActionKindEnum getTaskActionKind() {
    return TaskActionKindEnum.create;
  }

  @Override
  public void taskDependentCheck(HousekeepFilesTaskRecord taskRec,
      List<SingleAppException> exList) {

    taskDependentCheckCreateDir(exList, taskRec);
  }

  @Override
  protected void doTaskInternal(ConnectionToRemoteServer conn, HousekeepFilesTaskRecord taskRec,
      String srcPath, String destPath, List<AppException> warnList) throws Exception {

    File dest = new File(destPath);

    // 作成対象ディレクトリが既に存在する場合
    if (dest.exists() && dest.isDirectory()) {
      treatDestPathExists(taskRec, destPath, warnList);
      return;
    }
    
    // 以下、作成対象ディレクトリが存在しない場合

    // toPathがディレクトリでなくファイルとして存在する場合
    if (dest.exists() && !dest.isDirectory()) {
      throw new BizLogicAppException("MSG_ERR_DEST_PATH_IS_FILE", taskRec.getTaskId(),
          taskRec.getTaskName(), destPath);
    }

    // ファイル・ディレクトリとも存在しない場合。作成する
    new File(destPath).mkdirs();
  }
}
