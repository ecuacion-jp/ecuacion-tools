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
package jp.ecuacion.tool.housekeepfiles.tasklet;

import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;

public class Test02_01_excel設定ファイル不正関連_ extends TaskletTestTool {

  @Test
  public void test01_ファイルが存在しない場合() {
    try {
      execute("./testpath/test.xlsx");
      fail();
      
    } catch (Exception e) {
      assertTrue(e instanceof RuntimeException);
      assertTrue(e.getCause() instanceof FileNotFoundException);
    }
  }
}
