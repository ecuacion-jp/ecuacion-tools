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
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.tool.housekeepfiles.util.HkFileManipulateUtil;

/**
 * Provides abstract task for copy and move.
 */
@SuppressWarnings("NullAway")
public abstract class AbstractTaskCopyOrMove extends AbstractTaskLocal {
  private HkFileManipulateUtil fmu = new HkFileManipulateUtil();

  @Override
  public TaskActionKindEnum getTaskActionKind() {
    return TaskActionKindEnum.change;
  }

  /**
   * Executes task.
   */
  protected abstract void doSpecificTask(HousekeepFilesTaskRecord taskRec, String fromPath,
      String toPath, TaskPtnEnum taskPtn, boolean isFromDir, boolean isToDir);

  @Override
  public void taskDependentCheck(HousekeepFilesTaskRecord taskRec,
      Violations violations) {

  }

  @SuppressWarnings("null")
  @Override
  protected void doTaskInternal(ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, String srcPath, String destPath,
      List<BusinessViolation> warnList) {
    TaskPtnEnum taskPtn = taskRec.getTaskPtn();
    boolean doesOverwrittenFileOrDirExist = true;

    // First check whether a file to overwrite exists on the destination side. Any exception would
    // have already been thrown on the previous call to this method, so suppress exceptions here
    // assuming no errors occur.
    try {
      doesOverwrittenFileOrDirExist =
          fmu.checkIfToOverwrittenFileOrDirExists(taskRec, srcPath, destPath);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Hold flags indicating whether from and to are directories or files. At this point, if
    // compression is used, from becomes a file, so the result already accounts for that.
    boolean isFromDir = (taskRec.getIsSrcPathDir() == true);
    boolean isToDir = (taskRec.getIsDestPathDir() == true);

    // If a file to overwrite exists.
    if (doesOverwrittenFileOrDirExist) {
      if (taskRec.getDoesOverwriteDestPath() == false) {
        // If overwrite is disabled, stop processing.
        return;
      } else {
        // When overwriting, delete the destination file first to prevent FileExistsException.
        String toFilePath =
            isToDir ? FileUtil.concatFilePaths(destPath, new File(srcPath).getName()) : destPath;
        new File(toFilePath).delete();
      }
    }

    doSpecificTask(taskRec, srcPath, destPath, taskPtn, isFromDir, isToDir);
  }
}
