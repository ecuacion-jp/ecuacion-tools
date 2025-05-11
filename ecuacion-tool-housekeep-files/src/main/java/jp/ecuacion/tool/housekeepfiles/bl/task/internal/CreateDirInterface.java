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
package jp.ecuacion.tool.housekeepfiles.bl.task.internal;

import java.util.List;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;

public interface CreateDirInterface {
  public default void taskDependentCheckCreateDir(List<SingleAppException> exList,
      HousekeepFilesTaskRecord taskRec) {

    // 先パスがディレクトリ がFALSEは指定不可
    if (!taskRec.getIsDestPathDir()) {
      exList.add(new BizLogicAppException("MSG_ERR_TASK_CANNOT_SET_IS_DEST_PATH_DIR_TO_VALUE",
          taskRec.getTaskId(), taskRec.taskPtnEnumName, "FALSE"));
    }

    // 先パス存在時上書き がTRUEは指定不可
    if (taskRec.getDoesOverwriteDestPath() == true) {
      exList.add(new BizLogicAppException("MSG_ERR_TASK_CANNOT_SET_OVERWRITE_TO_VALUE",
          taskRec.getTaskId(), taskRec.taskPtnEnumName, "TRUE"));
    }
  }
}
