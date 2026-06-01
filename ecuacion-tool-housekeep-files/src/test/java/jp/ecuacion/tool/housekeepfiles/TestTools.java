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
package jp.ecuacion.tool.housekeepfiles;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

public class TestTools {

  //
  // Define test methods here to avoid cumbersome static imports.
  //

  public void assertTrue(boolean bl) {
    Assertions.assertTrue(bl);
  }

  public void assertFalse(boolean bl) {
    Assertions.assertFalse(bl);
  }

  public void assertEquals(@Nullable Object expected, @Nullable Object actual) {
    Assertions.assertEquals(expected, actual);
  }

  public void fail() {
    Assertions.fail();
  }

  // assertThat(..) is not easy to simplify, so recording the target class in a comment.
  // When using assertThat, import the following.
  // org.assertj.core.api.Assertions

}
