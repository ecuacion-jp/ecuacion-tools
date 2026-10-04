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
package jp.ecuacion.tool.housekeepcommon.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

/** Tests for {@link HousekeepWarnMailUtil}. */
@SuppressWarnings("null")
@DisplayName("HousekeepWarnMailUtil")
class HousekeepWarnMailUtilTest {

  private final DetailLogger detailLogger = new DetailLogger(this);

  private static MockEnvironment env(String addressCsv) {
    MockEnvironment env = new MockEnvironment();
    env.setProperty(HousekeepWarnMailUtil.PROP_ADDRESS_CSV_ON_SYSTEM_ERROR, addressCsv);
    env.setProperty(HousekeepWarnMailUtil.PROP_TITLE_PREFIX, "[test]");
    return env;
  }

  @Test
  @DisplayName("sends to the system error recipients, with the subject built from the title "
      + "prefix, tool name and target system name, and the body led by the hostname")
  void sendsMail() throws Exception {
    SplibMailUtil mailUtil = mock(SplibMailUtil.class);

    HousekeepWarnMailUtil.send(detailLogger, mailUtil, env("a@example.com,b@example.com"),
        "HousekeepDb", "my-system", "body text");

    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(mailUtil).sendTextMail(eq(List.of("a@example.com", "b@example.com")), isNull(),
        eq("[test][WARN] HousekeepDb:my-system"), body.capture());
    assertThat(body.getValue()).startsWith("hostname: ").endsWith("\n\nbody text");
  }

  @Test
  @DisplayName("omits the target system name from the subject when it's null")
  void omitsTargetSystemName() throws Exception {
    SplibMailUtil mailUtil = mock(SplibMailUtil.class);

    HousekeepWarnMailUtil.send(detailLogger, mailUtil, env("a@example.com"), "HousekeepFiles",
        null, "body text");

    verify(mailUtil).sendTextMail(any(), any(), eq("[test][WARN] HousekeepFiles"), any());
  }

  @Test
  @DisplayName("sends nothing when the recipients aren't configured")
  void noRecipients() throws Exception {
    SplibMailUtil mailUtil = mock(SplibMailUtil.class);

    HousekeepWarnMailUtil.send(detailLogger, mailUtil, new MockEnvironment(), "HousekeepDb",
        null, "body text");

    verify(mailUtil, never()).sendTextMail(any(), any(), any(), any());
  }

  @Test
  @DisplayName("sends nothing (without throwing) when not running in a Spring context")
  void noSpringContext() {
    assertThatCode(() -> HousekeepWarnMailUtil.send(detailLogger, null, null, "HousekeepDb", null,
        "body text")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("doesn't throw when the mail server settings aren't configured")
  void noMailServerSettings() throws Exception {
    SplibMailUtil mailUtil = mock(SplibMailUtil.class);
    doThrow(new IllegalStateException("not configured")).when(mailUtil)
        .sendTextMail(any(), any(), any(), any());

    assertThatCode(() -> HousekeepWarnMailUtil.send(detailLogger, mailUtil, env("a@example.com"),
        "HousekeepDb", null, "body text")).doesNotThrowAnyException();
  }
}
