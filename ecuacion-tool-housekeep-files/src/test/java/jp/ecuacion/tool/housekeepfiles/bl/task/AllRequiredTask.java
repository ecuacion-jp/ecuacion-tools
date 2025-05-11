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

import static jp.ecuacion.tool.housekeepfiles.bl.task.TaskAttrCheckPtnEnum.REQUIRED;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.other.FileInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;

/**
 * テスト用task。 taskPtnとしてはMOVEを使いながら、すべてをREQUIREDにしている。
 *
 * @author yosuk_000
 *
 */
public class AllRequiredTask extends AbstractTaskLocal {

  public AllRequiredTask() {
    taskPtn = TaskPtnEnum.MOVE;
    inputRuleForSrcPathInfo = REQUIRED;
    inputRuleForDestPathInfo = REQUIRED;
  }

  @Override
  public TaskActionKindEnum getTaskActionKind() {
    return TaskActionKindEnum.create;
  }

  @Override
  protected void doTaskInternal(ConnectionToRemoteServer conn, HousekeepFilesTaskRecord taskRec,
      String srcPath, String destPath, List<AppException> warnList) throws Exception {
    
  }

  @Override
  public ConnectionToRemoteServer getConnection(String remoteServer,
      Map<String, HousekeepFilesAuthRecord> authMap) throws Exception {
    return null;
  }

  @Override
  protected FileInfo getRemoteFileInfo(AbstractTask task, ConnectionToRemoteServer connection,
      boolean isPathDir, String path) {
    return null;
  }

  @Override
  protected List<FileInfo> getRemoteFileInfoList(AbstractTask task,
      ConnectionToRemoteServer connection, boolean isPathDir, String path) {
    return null;
  }

  @Override
  public void taskDependentCheck(
      HousekeepFilesTaskRecord taskRec, List<SingleAppException> exList) {

  }
}
