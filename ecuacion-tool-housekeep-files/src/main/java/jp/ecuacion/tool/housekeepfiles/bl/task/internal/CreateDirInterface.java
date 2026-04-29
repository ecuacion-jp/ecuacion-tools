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

import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;

public interface CreateDirInterface {
  /** Performs task-dependent checks for create directory tasks. */
  @SuppressWarnings({"NullAway", "null"})
  public default void taskDependentCheckCreateDir(Violations violations,
      HousekeepFilesTaskRecord taskRec) {

    // 先パスがディレクトリ がFALSEは指定不可
    if (!taskRec.getIsDestPathDir()) {
      violations.add(new BusinessViolation("MSG_ERR_TASK_CANNOT_SET_IS_DEST_PATH_DIR_TO_VALUE",
          taskRec.getTaskId(), taskRec.taskPtnEnumName, "FALSE"));
    }

    // 先パス存在時上書き がTRUEは指定不可
    if (taskRec.getDoesOverwriteDestPath() == true) {
      violations.add(new BusinessViolation("MSG_ERR_TASK_CANNOT_SET_OVERWRITE_TO_VALUE",
          taskRec.getTaskId(), taskRec.taskPtnEnumName, "TRUE"));
    }
  }
}
