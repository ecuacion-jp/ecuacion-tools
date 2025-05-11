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
package jp.ecuacion.tool.housekeepfiles.blf;

import java.io.IOException;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.BeforeEach;

public class BlfTestTool extends TestTool {

  protected HousekeepFilesBlf blf;

  @BeforeEach
  public void before() throws IOException {
    blf = new HousekeepFilesBlf();
  }
  
  public HousekeepFilesForm getDefaultForm() {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().setSysName("test-system");

    return form;
  }
}
