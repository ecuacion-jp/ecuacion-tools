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

import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides utilities on date and time.
 */
public class DateTimeUtil {

  /**
   * Obtains year from YYYYMMDD string.
   *
   * @param date String:YYYYMMDD
   */
  public String getYear(String date) {
    return date.substring(0, 4);
  }

  /**
   * Obtains month from YYYYMMDD string.
   *
   * @param date String:YYYYMMDD
   */
  public String getMonth(String date) {
    return date.substring(4, 6);
  }

  /**
   * Obtains day of month from YYYYMMDD string.
   *
   * @param date String: YYYYMMDD
   */
  public String getDay(String date) {
    return date.substring(6, 8);
  }
  
  private String lpadZero(int number, int size) {
    return StringUtils.leftPad(Integer.toString(number), size, "0");
  }

  /**
   * Returns timestamp with YYYYMMDD-HHMMSS.sss format for filename and others.
   */
  public String getTimestampNumString() {
    Calendar cal = Calendar.getInstance();
    return String.valueOf(cal.get(Calendar.YEAR)) + lpadZero(cal.get(Calendar.MONTH) + 1, 2)
        + lpadZero(cal.get(Calendar.DAY_OF_MONTH), 2) + "-"
        + lpadZero(cal.get(Calendar.HOUR_OF_DAY), 2)
        + lpadZero(cal.get(Calendar.MINUTE), 2)
        + lpadZero(cal.get(Calendar.SECOND), 2) + "."
        + StringUtils.rightPad(String.valueOf(cal.get(Calendar.MILLISECOND)), 3, "0");
  }

  /**
   * Returns today's YYYYMMDD.
   */
  public String getDateStr8() {
    Calendar cal = Calendar.getInstance();
    return Integer.toString(cal.get(Calendar.YEAR))
        + lpadZero(cal.get(Calendar.MONTH) + 1, 2)
        + lpadZero(cal.get(Calendar.DAY_OF_MONTH), 2);
  }

  /**
   * Provides boolean whether designated time passes.
   */
  public boolean hasDesignatedTermPassed(long lastModified, int unit, int value) {
    // For example, consider a nightly batch process that copies files and then deletes those
    // copies after 3 days.
    // The file should disappear after 3 days, but a naive datetime comparison would check whether
    // 86400000*3 milliseconds have elapsed.
    // Instead, the logic compares only the date part for day-unit granularity, ignoring the time
    // component.

    // Calendar built from the file's lastModified timestamp.
    Calendar calFileLastModified = Calendar.getInstance();
    calFileLastModified.setTimeInMillis(lastModified);
    // Calendar representing the datetime that is the specified period before now.
    // Received from a separate method to facilitate testing.
    Calendar calDesignatedTime = getCurrentCal();

    // When value = 0, processing should always occur, so always return true.
    if (value == 0) {
      return true;
    }

    // Roll back by the specified period.
    if (unit == Calendar.SECOND) {
      calDesignatedTime.add(Calendar.SECOND, -1 * value);

    } else if (unit == Calendar.MINUTE) {
      calDesignatedTime.add(Calendar.MINUTE, -1 * value);

    } else if (unit == Calendar.HOUR) {
      calDesignatedTime.add(Calendar.HOUR, -1 * value);

    } else if (unit == Calendar.DAY_OF_MONTH) {
      calDesignatedTime.add(Calendar.DAY_OF_MONTH, -1 * value);

    } else if (unit == Calendar.MONTH) {
      calDesignatedTime.add(Calendar.MONTH, -1 * value);

    } else if (unit == Calendar.YEAR) {
      calDesignatedTime.add(Calendar.YEAR, -1 * value);
    }

    // For day-level comparison, the time is irrelevant — only the date must differ
    // (less than 24 hours is acceptable).
    // To require a full 24 hours to pass, specify 24 hours.
    // Calendar data includes milliseconds, so replace the lower-precision fields with fixed
    // values as needed.
    Date dateFileLastModified = makeUnusedCalendarUnitValToFixedVal(calFileLastModified, unit);
    Date dateDesignatedTime = makeUnusedCalendarUnitValToFixedVal(calDesignatedTime, unit);

    // Return the comparison result.
    return (dateFileLastModified.getTime() > dateDesignatedTime.getTime()) ? false : true;
  }

  /**
   * Provides calender instance with current dateTime.
   * 
   * <p>To make easier to execute unit tests, this method is extracted and protected scope.
   */
  protected Calendar getCurrentCal() {
    return Calendar.getInstance();
  }

  /*
   * Comparing using Calendar directly proved unreliable; examples found online suggest comparing
   * using Date, so this method returns a Date.
   */
  private Date makeUnusedCalendarUnitValToFixedVal(Calendar cal, int timeUnit) {
    final int year = cal.get(Calendar.YEAR);
    final int month = (timeUnit == Calendar.YEAR) ? 0 : cal.get(Calendar.MONTH);
    final int day = (timeUnit == Calendar.YEAR || timeUnit == Calendar.MONTH) ? 0
        : cal.get(Calendar.DAY_OF_MONTH);
    final int hour = (timeUnit == Calendar.YEAR || timeUnit == Calendar.MONTH
        || timeUnit == Calendar.DAY_OF_MONTH) ? 0 : cal.get(Calendar.HOUR_OF_DAY);
    final int minute = (timeUnit == Calendar.YEAR || timeUnit == Calendar.MONTH
        || timeUnit == Calendar.DAY_OF_MONTH || timeUnit == Calendar.HOUR) ? 0
            : cal.get(Calendar.MINUTE);
    final int second = (timeUnit == Calendar.YEAR || timeUnit == Calendar.MONTH
        || timeUnit == Calendar.DAY_OF_MONTH || timeUnit == Calendar.HOUR
        || timeUnit == Calendar.MINUTE) ? 0 : cal.get(Calendar.SECOND);

    Calendar rtnCal = Calendar.getInstance();
    // Set milliseconds to 0.
    rtnCal.clear();

    rtnCal.set(year, month, day, hour, minute, second);
    return rtnCal.getTime();
  }
}
