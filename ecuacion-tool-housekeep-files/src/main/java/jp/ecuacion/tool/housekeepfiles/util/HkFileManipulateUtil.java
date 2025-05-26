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
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;

/**
 * Provides utilities.
 */
public class HkFileManipulateUtil {

  /**
   * Checks if overwritten files or directories exist.
   */
  public boolean checkIfToOverwrittenFileOrDirExists(HousekeepFilesTaskRecord taskRec,
      String fromPath, String toPath) throws BizLogicAppException {

    if (taskRec.getIsDestPathDir() == false) {
      // toがファイルの場合
      // extensionを考慮
      // toPath = toPath + extension;
      return (new File(toPath).exists()) ? true : false;

    } else {
      // toがディレクトリの場合、そのディレクトリの中まで確認してチェックを行う
      // fromがディレクトリかどうかでも処理を分ける。しかも、fromがディレクトリでも、zip指定があれば結局ファイルの扱いになるのでそれも分岐条件に入れる。
      if (taskRec.getIsSrcPathDir() == true) {
        // 結局、ここはfrom=ディレクトリ、to=ディレクトリ
        // toの下のフォルダにfrom側のディレクトリ名があるかを確認。
        return (new File(FileUtil.concatFilePaths(toPath, new File(fromPath).getName())).exists())
            ? true
            : false;

      } else {
        // ここは、from=ファイル、to=ディレクトリ
        // fromPath = fromPath + extension;
        return (new File(FileUtil.concatFilePaths(toPath, new File(fromPath).getName())).exists())
            ? true
            : false;
      }
    }
  }

}
