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
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.tool.housekeepfiles.util.DateTimeUtil;
import org.apache.commons.io.FileUtils;

/**
 * Provides move task.
 */
@SuppressWarnings("NullAway")
public class Move extends AbstractTaskCopyOrMove {

  private DateTimeUtil dateUtil = new DateTimeUtil();

  /**
   * Constructs a new instance.
   */
  public Move() {
    taskPtn = TaskPtnEnum.MOVE;
  }

  @SuppressWarnings("null")
  @Override
  protected void doSpecificTask(HousekeepFilesTaskRecord taskRec, String fromPath, String toPath,
      TaskPtnEnum taskPtn, boolean isFromDir, boolean isToDir) {
    // Perform move/copy.
    File from = new File(fromPath);
    File to = new File(toPath);
    if (isFromDir) {
      // Adjust processing for the same reason as FileUtils.copyDirectory below.
      String newToPath = FileUtil.concatFilePaths(to.getAbsolutePath(), from.getName());
      // Because lock state cannot always be determined reliably, ignore items that cannot be
      // deleted.
      try {
        FileUtils.moveDirectory(from, new File(newToPath));
      } catch (Exception e) {
        dlog.debug("Skipping because the file is locked: " + from);
        dlog.warn(e);
      }
    } else {
      // Check whether the from file's elapsed time satisfies the condition.
      if (dateUtil.hasDesignatedTermPassed(from.lastModified(), taskRec.getUnit(),
          taskRec.getValue())) {
        // Because lock state cannot always be determined reliably, ignore items that cannot be
        // deleted.
        try {
          if (isToDir) {
            FileUtils.moveFileToDirectory(from, to, true);

          } else {
            FileUtils.moveFile(from, to);
          }
        } catch (Exception e) {
          dlog.debug("Skipping because the file is locked: " + from);
          dlog.warn(e);
        }
      }
    }
  }
}
