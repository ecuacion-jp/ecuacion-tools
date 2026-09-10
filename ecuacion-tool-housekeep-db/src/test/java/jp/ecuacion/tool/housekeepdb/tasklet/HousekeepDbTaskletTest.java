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
package jp.ecuacion.tool.housekeepdb.tasklet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for {@link HousekeepDbTasklet} that don't need a real database - see
 * {@link AbstractHousekeepDbTaskletTest} (and its Postgresql/Mysql subclasses) for the full
 * integration tests.
 */
@SuppressWarnings("null")
@DisplayName("HousekeepDbTasklet")
class HousekeepDbTaskletTest {

  @Nested
  @DisplayName("createEnvVarValueGetter()")
  class CreateEnvVarValueGetter {

    @Test
    @DisplayName("a key resolves via env")
    void resolvesViaEnv() {
      HousekeepDbTasklet tasklet = new HousekeepDbTasklet(null, 1000);
      MockEnvironment env = new MockEnvironment();
      env.setProperty("DB_PASSWORD", "secret-value");
      tasklet.env = env;

      Function<String, String> getter = tasklet.createEnvVarValueGetter();

      assertThat(getter.apply("DB_PASSWORD")).isEqualTo("secret-value");
    }

    @Test
    @DisplayName("when env is null (e.g. tasklet built outside of Spring), every key resolves to "
        + "null")
    void resolvesToNullWhenEnvIsNull() {
      HousekeepDbTasklet tasklet = new HousekeepDbTasklet(null, 1000);

      Function<String, String> getter = tasklet.createEnvVarValueGetter();

      assertThat(getter.apply("DB_PASSWORD")).isNull();
    }

    @Test
    @DisplayName("an empty-string property value resolves to null (treated as \"not found\"), "
        + "not to an empty expansion")
    void emptyStringPropertyResolvesToNull() {
      HousekeepDbTasklet tasklet = new HousekeepDbTasklet(null, 1000);
      MockEnvironment env = new MockEnvironment();
      env.setProperty("DB_PASSWORD", "");
      tasklet.env = env;

      Function<String, String> getter = tasklet.createEnvVarValueGetter();

      assertThat(getter.apply("DB_PASSWORD")).isNull();
    }

    @Test
    @DisplayName("an unset key resolves to null")
    void unsetKeyResolvesToNull() {
      HousekeepDbTasklet tasklet = new HousekeepDbTasklet(null, 1000);
      tasklet.env = new MockEnvironment();

      Function<String, String> getter = tasklet.createEnvVarValueGetter();

      assertThat(getter.apply("DB_PASSWORD")).isNull();
    }
  }
}
