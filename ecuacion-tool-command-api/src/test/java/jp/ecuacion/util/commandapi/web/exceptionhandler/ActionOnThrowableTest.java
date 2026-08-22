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
package jp.ecuacion.util.commandapi.web.exceptionhandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jp.ecuacion.splib.core.exceptionhandler.SplibRestExceptionHandlerAction;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link ActionOnThrowable} sends an error mail and is wired to
 * {@link SplibRestExceptionHandlerAction}, the extension point {@code SplibRestExceptionHandler}
 * uses for command-api's REST frontend.
 */
@DisplayName("ActionOnThrowable")
class ActionOnThrowableTest {

  @Test
  @DisplayName("implements SplibRestExceptionHandlerAction")
  void implementsRestActionInterface() {
    ActionOnThrowable action = new ActionOnThrowable(mock(SplibMailUtil.class));

    Assertions.assertThat(action).isInstanceOf(SplibRestExceptionHandlerAction.class);
  }

  @Test
  @DisplayName("execute sends an error mail for the given throwable")
  void executeSendsErrorMail() {
    SplibMailUtil splibMailUtil = mock(SplibMailUtil.class);
    ActionOnThrowable action = new ActionOnThrowable(splibMailUtil);
    RuntimeException exception = new RuntimeException("test");

    action.execute(exception);

    verify(splibMailUtil).sendErrorMail(exception);
  }
}
