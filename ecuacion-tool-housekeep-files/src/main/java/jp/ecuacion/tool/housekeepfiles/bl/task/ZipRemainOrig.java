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

import java.util.List;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;

/**
 * Provides zip remaining original file task.
 */
public class ZipRemainOrig extends AbstractTaskZip {

  /**
   * Constructs a new instance.
   */
  public ZipRemainOrig() {
    taskPtn = TaskPtnEnum.ZIP_REMAIN_ORIG;
  }

  @Override
  public TaskActionKindEnum getTaskActionKind() {
    return TaskActionKindEnum.createFromOriginal;
  }

  @Override
  public void taskDependentCheck(
      HousekeepFilesTaskRecord taskRec, List<SingleAppException> exList) {

  }

}
