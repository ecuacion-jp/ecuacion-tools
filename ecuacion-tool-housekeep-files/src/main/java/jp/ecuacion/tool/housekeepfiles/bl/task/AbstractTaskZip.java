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
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.util.CompressUtil;
import org.apache.commons.io.FileUtils;

/**
 * Provides abstract zip tasks.
 */
@SuppressWarnings("NullAway")
public abstract class AbstractTaskZip extends AbstractTaskLocal {

  @SuppressWarnings("null")
  @Override
  protected void doTaskInternal(ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, String fromPath, String toPath,
      List<BusinessViolation> warnList) {
    CompressUtil cu = new CompressUtil();

    File from = new File(fromPath);
    // Branch based on whether a destination is specified.
    String toFilePath = null;
    if (taskRec.getIsDestPathDir() == null) {
      // When no destination is specified.
      // Whether from is a file or directory, from.getParentFile() is the target location for the
      // created zip file.
      File toDir = from.getParentFile();
      toFilePath = FileUtil.concatFilePaths(toDir.getAbsolutePath(), from.getName() + ".zip");

    } else {
      // When a destination is specified.
      toFilePath =
          taskRec.getIsDestPathDir() ? FileUtil.concatFilePaths(toPath, from.getName() + ".zip")
              : toPath;
    }

    if (new File(toFilePath).exists()) {
      treatDestPathExists(taskRec, taskRec.getSrcPath() + ".zip", warnList);
      return;
    }

    try {
      if (from.isDirectory()) {
        cu.zipDirectory(fromPath, toFilePath);
      } else {
        cu.zipFile(fromPath, toFilePath);
      }
    } catch (Exception e) {
      dlog.debug("Skipping because the file is locked: " + fromPath);
      dlog.warn(e);
      // Deleting the original while skipping the zip would be problematic, so stop here.
      return;
    }

    // Delete the original when deleteOrig is set.
    if (this.getClass().getSimpleName().contains("DeleteOrig")) {
      try {
        File fromFile = new File(fromPath);
        if (fromFile.isDirectory()) {
          FileUtils.deleteDirectory(new File(fromPath));

        } else {
          fromFile.delete();
        }

      } catch (Exception ignored) {
        dlog.debug("Skipping zip source file because it is locked: " + fromPath);
      }
    }

  }
}
