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

public class Copy extends AbstractTaskCopyOrMove {

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
        // FileUtilsのcopyDirectoryとはちょっと考え方が異なる。
        // FileUtilsでは、fromをhome/fromDir、toをhome/toDirとすると、fromDirの中に入っているものをtoDirの中にコピーする。
        // それは、home/fromDir/*と指定した場合に期待する動きであって、home/fromDirと指定した場合は、toには
        // home/toDir/fromDir となるのが正しい。
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
