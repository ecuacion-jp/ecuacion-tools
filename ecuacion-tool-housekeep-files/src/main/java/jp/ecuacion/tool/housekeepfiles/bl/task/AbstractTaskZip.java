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
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.util.CompressUtil;
import org.apache.commons.io.FileUtils;

public abstract class AbstractTaskZip extends AbstractTaskLocal {

  @Override
  protected void doTaskInternal(ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, String fromPath, String toPath, List<AppException> warnList)
      throws AppException {
    CompressUtil cu = new CompressUtil();

    File from = new File(fromPath);
    // Toを指定する場合としない場合があるので分岐
    String toFilePath = null;
    if (taskRec.getIsDestPathDir() == null) {
      // TOを指定しない場合
      // fromがfileでもdirでも、from.getParentFile()が結局作成したzipファイルの置きたい場所となる
      File toDir = from.getParentFile();
      toFilePath = FileUtil.concatFilePaths(toDir.getAbsolutePath(), from.getName() + ".zip");

    } else {
      // TOを指定する場合
      toFilePath =
          (taskRec.getIsDestPathDir()) ? FileUtil.concatFilePaths(toPath, from.getName() + ".zip")
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
      dlog.debug("ファイルがロックされているためスキップします：" + fromPath);
      e.printStackTrace();
      // zipをスキップしているのに元ファイルを削除するのは問題なのでここで終了。
      return;
    }

    // deleteOrigの場合は削除
    if (this.getClass().getSimpleName().contains("DeleteOrig")) {
      try {
        File fromFile = new File(fromPath);
        if (fromFile.isDirectory()) {
          FileUtils.deleteDirectory(new File(fromPath));

        } else {
          fromFile.delete();
        }

      } catch (Exception e) {
        dlog.debug("zip元ファイルがロックされているためスキップします：" + fromPath);
      }
    }

  }
}
