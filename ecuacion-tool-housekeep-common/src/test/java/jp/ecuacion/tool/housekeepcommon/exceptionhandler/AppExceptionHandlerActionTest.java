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
package jp.ecuacion.tool.housekeepcommon.exceptionhandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.Objects;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link AppExceptionHandlerAction} sends an error mail and is wired to
 * {@link SplibExceptionHandlerAction}, the extension point housekeep-db's and housekeep-files'
 * batch frontends use.
 */
@DisplayName("ActionOnThrowable")
class AppExceptionHandlerActionTest {

  @Test
  @DisplayName("implements SplibExceptionHandlerAction")
  void implementsBatchActionInterface() {
    @SuppressWarnings("null")
    AppExceptionHandlerAction action = new AppExceptionHandlerAction(mock(SplibMailUtil.class));

    Assertions.assertThat(action).isInstanceOf(SplibExceptionHandlerAction.class);
  }

  @Test
  @DisplayName("execute sends an error mail for the given throwable")
  void executeSendsErrorMail() {
    SplibMailUtil splibMailUtil = Objects.requireNonNull(mock(SplibMailUtil.class));
    AppExceptionHandlerAction action = new AppExceptionHandlerAction(splibMailUtil);
    RuntimeException exception = new RuntimeException("test");

    action.execute(exception);

    verify(splibMailUtil).sendErrorMail(exception);
  }
}
