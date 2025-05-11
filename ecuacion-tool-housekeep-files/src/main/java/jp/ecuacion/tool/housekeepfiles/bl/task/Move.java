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

public class Move extends AbstractTaskCopyOrMove {

  private DateTimeUtil dateUtil = new DateTimeUtil();

  public Move() {
    taskPtn = TaskPtnEnum.MOVE;
  }

  @Override
  protected void doSpecificTask(HousekeepFilesTaskRecord taskRec, String fromPath, String toPath,
      TaskPtnEnum taskPtn, boolean isFromDir, boolean isToDir) {
    // 移動・コピーを実施
    File from = new File(fromPath);
    File to = new File(toPath);
    if (isFromDir) {
      // 下のFileUtils.copyDirectoryと同じ理由で処理を少々変える
      String newToPath = FileUtil.concatFilePaths(to.getAbsolutePath(), from.getName());
      // どうしてもロック状態を正しく取得できない場合があるため、どうしても削除できないものは無視する
      try {
        FileUtils.moveDirectory(from, new File(newToPath));
      } catch (Exception e) {
        dlog.debug("ファイルがロックされているためスキップします：" + from);
        e.printStackTrace();
      }
    } else {
      // fromのファイルの期間が条件を満たすかを確認
      if (dateUtil.hasDesignatedTermPassed(from.lastModified(), taskRec.getUnit(),
          taskRec.getValue())) {
        // どうしてもロック状態を正しく取得できない場合があるため、どうしても削除できないものは無視する
        try {
          if (isToDir) {
            FileUtils.moveFileToDirectory(from, to, true);

          } else {
            FileUtils.moveFile(from, to);
          }
        } catch (Exception e) {
          dlog.debug("ファイルがロックされているためスキップします：" + from);
          e.printStackTrace();
        }
      }
    }
  }
}
