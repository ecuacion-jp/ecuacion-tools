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

import jp.ecuacion.splib.core.exceptionhandler.SplibRestExceptionHandlerAction;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import org.springframework.stereotype.Component;

/**
 * Sends a mail on a system error, when mail settings exist.
 *
 * <p>Implements {@link SplibRestExceptionHandlerAction} — the extension point
 *     {@code SplibRestExceptionHandler} uses for command-api's REST frontend (e.g.
 *     {@code /api/**} endpoints). command-api has no web/batch frontend, so there's no
 *     {@code SplibExceptionHandlerAction} consumer to also implement here.</p>
 */
@Component
public class ActionOnThrowable implements SplibRestExceptionHandlerAction {

  private final SplibMailUtil splibMailUtil;

  /**
   * Constructs a new instance.
   */
  public ActionOnThrowable(SplibMailUtil splibMailUtil) {
    this.splibMailUtil = splibMailUtil;
  }

  @Override
  public void execute(Throwable th) {
    splibMailUtil.sendErrorMail(th);
  }
}
