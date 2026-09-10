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
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.util.CompressUtil;

/**
 * Provides abstract unzip tasks.
 */
@SuppressWarnings("NullAway")
public abstract class AbstractTaskUnzip extends AbstractTaskLocal {

  @SuppressWarnings("null")
  @Override
  public void taskDependentCheck(HousekeepFilesTaskRecord taskRec, Violations violations) {
    // Extracting into a single file makes no sense, so "destination path is directory" cannot be
    // set to FALSE. (It may be left unset - see doTaskInternal for the default destination in
    // that case.)
    if (taskRec.getIsDestPathDir() != null && !taskRec.getIsDestPathDir()) {
      violations.add(new BusinessViolation("MSG_ERR_TASK_CANNOT_SET_IS_DEST_PATH_DIR_TO_VALUE",
          taskRec.getTaskId(), taskRec.taskPtnEnumName, "FALSE"));
    }
  }

  @SuppressWarnings("null")
  @Override
  protected void doTaskInternal(ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, String fromPath, String toPath,
      List<BusinessViolation> warnList) {
    CompressUtil cu = new CompressUtil();

    File from = new File(fromPath);
    // Branch based on whether a destination is specified. When absent, extract alongside the zip
    // file itself (from.getParentFile()).
    String toDirPath = taskRec.getIsDestPathDir() == null ? from.getParentFile().getAbsolutePath()
        : toPath;

    File toDir = new File(toDirPath);
    if (toDir.exists() && !toDir.isDirectory()) {
      new Violations().add(new BusinessViolation("MSG_ERR_DEST_PATH_IS_FILE", taskRec.getTaskId(),
          taskRec.getTaskName(), toDirPath)).throwIfAny();
    }

    try {
      cu.unzip(fromPath, toDirPath);
    } catch (Exception e) {
      dlog.debug("Skipping because the file is locked or not a valid zip file: " + fromPath);
      dlog.warn(e);
      // Deleting the original while skipping the unzip would be problematic, so stop here.
      return;
    }

    // Delete the original when deleteOrig is set.
    if (this.getClass().getSimpleName().contains("DeleteOrig")) {
      try {
        new File(fromPath).delete();

      } catch (Exception ignored) {
        dlog.debug("Skipping unzip source file deletion because it is locked: " + fromPath);
      }
    }
  }
}
