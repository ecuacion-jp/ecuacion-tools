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
package jp.ecuacion.tool.housekeepfiles.dto.other;

import java.util.List;

/**
 * Stores parameter expanded paths info.
 */
public class HousekeepFilesExpandedPathsInfo {
  public List<String> fromFileList;
  // 本来toは一つでなければならないのだが、一旦チェックをせずに格納しておきたいのでこのリストが存在している。最終的にはtoPathに入れる
  public List<String> tmpToFileList;
  public String toPath;

  /**
   * Constructs a new instance.
   */
  public HousekeepFilesExpandedPathsInfo(List<String> fromFileList, List<String> tmpToFileList) {
    super();
    this.fromFileList = fromFileList;
    this.tmpToFileList = tmpToFileList;
  }
}
