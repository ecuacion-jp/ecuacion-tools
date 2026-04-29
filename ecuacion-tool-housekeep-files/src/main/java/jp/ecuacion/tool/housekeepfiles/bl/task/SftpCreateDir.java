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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.util.List;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToSftpServer;
import jp.ecuacion.tool.housekeepfiles.bl.task.internal.CreateDirInterface;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import org.jspecify.annotations.Nullable;

/**
 * Provides sftp create directory task.
 */
@SuppressWarnings("NullAway")
public class SftpCreateDir extends AbstractTaskSftp implements CreateDirInterface {

  /**
   * Constructs a new instance.
   */
  public SftpCreateDir() {
    taskPtn = TaskPtnEnum.SFTP_CREATE_DIR;
  }

  @Override
  public TaskActionKindEnum getTaskActionKind() {
    return TaskActionKindEnum.create;
  }

  @Override
  public @Nullable Boolean isSrcPathLocal() {
    return null;
  }

  @Override
  public Boolean isDestPathLocal() {
    return false;
  }

  @Override
  public void taskDependentCheck(HousekeepFilesTaskRecord taskRec, Violations violations) {
    taskDependentCheckCreateDir(violations, taskRec);
  }

  @Override
  protected void doSpecificTask(ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, String srcPath, String destPath,
      List<BusinessViolation> warnList) throws Exception {

    ChannelSftp channel = ((ConnectionToSftpServer) connection).getSftpChannel();

    // 作成対象ディレクトリが既に存在する場合
    if (remoteDirExists(channel, destPath)) {
      treatDestPathExists(taskRec, destPath, warnList);
      return;
    }

    // 以下、作成対象ディレクトリが存在しない場合

    // toPathがディレクトリでなくファイルとして存在する場合
    if (remoteFileExists(channel, destPath)) {
      new Violations().add(new BusinessViolation("MSG_ERR_DEST_PATH_IS_FILE",
          taskRec.getTaskId(), taskRec.getTaskName(), destPath)).throwIfAny();
    }

    // ファイル・ディレクトリとも存在しない場合。作成する
    createDirRecursively(channel, taskRec, destPath);
  }

  private void createDirRecursively(ChannelSftp channel, HousekeepFilesTaskRecord taskRec,
      String destPath) throws SftpException {

    String parentPath = new File(destPath).getParent();

    if (!remoteExists(channel, parentPath)) {
      // 親が存在しない場合、さらに親のディレクトリを作成しに行く
      createDirRecursively(channel, taskRec, parentPath);
    }

    if (remoteDirExists(channel, parentPath)) {
      // 親ディレクトリが存在するなら、自ディレクトリを作成して終了
      channel.mkdir(destPath);

    } else {
      // 親パスがファイルの場合。エラー終了。
      new Violations().add(new BusinessViolation("MSG_ERR_DEST_PATH_IS_FILE",
          taskRec.getTaskId(), taskRec.getTaskName(), destPath)).throwIfAny();
    }
  }
}
