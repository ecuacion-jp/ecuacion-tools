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

  private static int calendarField(String unitName) {
    return switch (unitName) {
      case "SECOND" -> Calendar.SECOND;
      case "MINUTE" -> Calendar.MINUTE;
      case "HOUR" -> Calendar.HOUR;
      case "DAY" -> Calendar.DAY_OF_MONTH;
      case "MONTH" -> Calendar.MONTH;
      case "YEAR" -> Calendar.YEAR;
      default -> throw new IllegalArgumentException(unitName);
    };
  }

  @Nested
  @DisplayName("hasDesignatedTermPassed()")
  class HasDesignatedTermPassed {

    @ParameterizedTest(name = "unit={0}, value={1}, elapsed: {3} {2}(s) -> {4}")
    @CsvSource({
        // unit, value, elapsedUnit, elapsedAmount, expected
        "SECOND, 0, SECOND, 0, true",
        "SECOND, 0, SECOND, 1, true",
        "SECOND, 3, SECOND, 2, false",
        "SECOND, 3, SECOND, 3, true",
        "SECOND, 3, MINUTE, 1, true",
        "MINUTE, 0, MINUTE, 0, true",
        "MINUTE, 0, MINUTE, 1, true",
        "MINUTE, 3, MINUTE, 2, false",
        "MINUTE, 3, MINUTE, 3, true",
        "MINUTE, 3, HOUR, 1, true",
        "MINUTE, 3, SECOND, 5, false",
        "HOUR, 0, HOUR, 0, true",
        "HOUR, 0, HOUR, 1, true",
        "HOUR, 3, HOUR, 2, false",
        "HOUR, 3, HOUR, 3, true",
        "HOUR, 3, DAY, 1, true",
        "HOUR, 3, MINUTE, 5, false",
        "DAY, 0, DAY, 0, true",
        "DAY, 0, DAY, 1, true",
        "DAY, 3, DAY, 2, false",
        "DAY, 3, DAY, 3, true",
        "DAY, 3, MONTH, 1, true",
        "DAY, 3, HOUR, 5, false",
        "MONTH, 0, MONTH, 0, true",
        "MONTH, 0, MONTH, 1, true",
        "MONTH, 3, MONTH, 2, false",
        "MONTH, 3, MONTH, 3, true",
        "MONTH, 3, YEAR, 1, true",
        "MONTH, 3, DAY, 5, false",
        "YEAR, 0, YEAR, 0, true",
        "YEAR, 0, YEAR, 1, true",
        "YEAR, 3, YEAR, 2, false",
        "YEAR, 3, YEAR, 3, true",
        "YEAR, 3, MONTH, 5, false"})
    @DisplayName("designated term is judged as passed based on the designated unit granularity")
    void hasDesignatedTermPassed(String unit, int value, String elapsedUnit, int elapsedAmount,
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

      lastModified.add(calendarField(elapsedUnit), -elapsedAmount);

      assertThat(util.hasDesignatedTermPassed(lastModified.getTimeInMillis(),
          calendarField(unit), value)).isEqualTo(expected);
    }
  }
}
