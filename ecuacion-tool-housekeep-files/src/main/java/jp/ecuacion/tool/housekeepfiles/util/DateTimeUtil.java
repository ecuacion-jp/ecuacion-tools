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
    // * たとえば、日次で夜間に動くバッチ処理で、ファイルコピーと、3日後にそのコピーしたファイルを削除する処理があるとする。
    // * その際、3日後にはファイルが消えてほしいのだが、普通の日時比較をすると、86400000*3ミリ秒経過したかどうか、 で判断してしまう。
    //  * そうではなくて、日単位なら時刻は見ずに日だけで比較する、というロジック。

    // fileのlastModifiedをもとに作成したCalendar
    Calendar calFileLastModified = Calendar.getInstance();
    calFileLastModified.setTimeInMillis(lastModified);
    // 現在から指定の期間だけさかのぼった日時のCalendar。テストしやすくするため別メソッドからの受取とする
    Calendar calDesignatedTime = getCurrentCal();

    // value = 0の場合は、必ず処理を行う、でよいので常にtrueを返す
    if (value == 0) {
      return true;
    }

    // さかのぼる処理
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

    // たとえば日で比較するときは、時間は無関係で、日付が違えばよい。（24時間たっていなくてもよい）
    // 24時間たってから、にしたいなら、24時間と指定する。
    // calendarデータにはミリ秒までデータが入っているため、必要な分だけそれを固定値に置き換える
    Date dateFileLastModified = makeUnusedCalendarUnitValToFixedVal(calFileLastModified, unit);
    Date dateDesignatedTime = makeUnusedCalendarUnitValToFixedVal(calDesignatedTime, unit);

    // 比較した結果をreturn
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
   * calendarのままで判断しようとするとどうにもうまくいかず、ネットで見るとdateで比較する例があったのでdateを返すこととする。
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
    // ミリ秒を0にセット
    rtnCal.clear();

    rtnCal.set(year, month, day, hour, minute, second);
    return rtnCal.getTime();
  }
}
