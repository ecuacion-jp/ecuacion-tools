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
package jp.ecuacion.tool.housekeepfiles.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests for {@link DateTimeUtil}. */
@DisplayName("DateTimeUtil")
class DateTimeUtilTest {

  @Nested
  @DisplayName("hasDesignatedTermPassed()")
  class HasDesignatedTermPassed {

    @ParameterizedTest(name = "value={0}, elapsed: {2} {1}(s) -> {3}")
    @CsvSource({
        // value, elapsedUnit, elapsedAmount, expected
        "0, DAY, 0, true",
        "0, DAY, 1, true",
        "3, DAY, 2, false",
        "3, DAY, 3, true",
        "3, DAY, 31, true",
        "3, HOUR, 5, false"})
    @DisplayName("designated term (in days) is judged as passed based on date-only granularity")
    void hasDesignatedTermPassed(int value, String elapsedUnit, int elapsedAmount,
        boolean expected) {
      Calendar lastModified = Calendar.getInstance();
      Calendar current = (Calendar) lastModified.clone();

      // Fix "now" to make the test deterministic.
      DateTimeUtil util = new DateTimeUtil() {
        @Override
        protected Calendar getCurrentCal() {
          return (Calendar) current.clone();
        }
      };

      int calendarField = elapsedUnit.equals("DAY") ? Calendar.DAY_OF_MONTH : Calendar.HOUR;
      lastModified.add(calendarField, -elapsedAmount);

      assertThat(util.hasDesignatedTermPassed(lastModified.getTimeInMillis(), value))
          .isEqualTo(expected);
    }
  }
}
