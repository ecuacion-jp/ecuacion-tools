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
package jp.ecuacion.tool.housekeepfiles.util;

import java.io.File;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;

/**
 * Provides utilities.
 */
@SuppressWarnings("NullAway")
public class HkFileManipulateUtil {

  /**
   * Checks if overwritten files or directories exist.
   */
  public boolean checkIfToOverwrittenFileOrDirExists(HousekeepFilesTaskRecord taskRec,
      String fromPath, String toPath) {

    if (taskRec.getIsDestPathDir() == false) {
      // When the destination is a file.
      // Taking file extension into account.
      // toPath = toPath + extension;
      return (new File(toPath).exists()) ? true : false;

    } else {
      // When the destination is a directory, check inside that directory.
      // Also branch based on whether from is a directory. Note that even if from is a directory,
      // it is treated as a file when zip is specified, so include that in the condition.
      boolean isSrcPathDir;
      try {
        isSrcPathDir = taskRec.getIsSrcPathDir() == true;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      if (isSrcPathDir) {
        // This case is: from=directory, to=directory.
        // Check whether a folder with the same name as the from directory exists under to.
        return (new File(FileUtil.concatFilePaths(toPath, new File(fromPath).getName())).exists())
            ? true
            : false;

      } else {
        // This case is: from=file, to=directory.
        // fromPath = fromPath + extension;
        return (new File(FileUtil.concatFilePaths(toPath, new File(fromPath).getName())).exists())
            ? true
            : false;
      }
    }
  }

}
