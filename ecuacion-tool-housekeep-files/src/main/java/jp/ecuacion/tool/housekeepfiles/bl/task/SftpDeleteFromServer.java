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
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToSftpServer;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;

/**
 * Provides sftp delete from server task.
 */
public class SftpDeleteFromServer extends AbstractTaskSftp {

  /**
   * Constructs a new instance.
   */
  public SftpDeleteFromServer() {
    taskPtn = TaskPtnEnum.SFTP_DELETE_FROM_SERVER;
  }

  @Override
  public TaskActionKindEnum getTaskActionKind() {
    return TaskActionKindEnum.delete;
  }

  @Override
  public Boolean isSrcPathLocal() {
    return false;
  }

  @Override
  public Boolean isDestPathLocal() {
    return null;
  }

  @Override
  public void taskDependentCheck(HousekeepFilesTaskRecord taskRec,
      List<SingleAppException> exList) {

  }

  @Override
  protected void doSpecificTask(ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, String fromPath, String toPath, List<AppException> warnList)
      throws Exception {
    ChannelSftp sftp = ((ConnectionToSftpServer) connection).getSftpChannel();
    // 指定のディレクトリがそもそも存在しない場合は作成する
    // makeRemoteDirs(connection, fromPath);
    sftp.rm(fromPath);
  }
}
