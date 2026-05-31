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
import org.apache.commons.io.FileUtils;

/**
 * Provides copy task.
 */
public class Copy extends AbstractTaskCopyOrMove {

  /**
   * Constructs a new instance.
   */
  public Copy() {
    taskPtn = TaskPtnEnum.COPY;
  }

  @Override
  protected void doSpecificTask(HousekeepFilesTaskRecord taskRec, String fromPath, String toPath,
      TaskPtnEnum taskPtn, boolean isFromDir, boolean isToDir) {
    File from = new File(fromPath);
    File to = new File(toPath);
    try {
      if (isFromDir) {
        // The behavior here differs slightly from FileUtils.copyDirectory.
        // With FileUtils, if from is home/fromDir and to is home/toDir, the contents inside
        // fromDir are copied into toDir.
        // That is the behavior expected when specifying home/fromDir/*, but when specifying
        // home/fromDir, the destination should be home/toDir/fromDir.
        String newToPath = FileUtil.concatFilePaths(to.getAbsolutePath(), from.getName());
        FileUtils.copyDirectory(from, new File(newToPath));
      } else {
        if (isToDir) {
          FileUtils.copyFileToDirectory(from, to);

        } else {
          FileUtils.copyFile(from, to);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
