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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * aaa.
 *
 * @author 庸介
 *
 */
public class DateUtiHhasDesignatedTimePassed {

  Calendar calLastModifiedTimestamp = null;
  Calendar calCurrentTimestampMinusValue = null;
  DateTimeUtil bl = null;

  /** */
  @BeforeAll
  public static void beforeClass() {}

  /** */
  @BeforeEach
  public void before() {
    calLastModifiedTimestamp = Calendar.getInstance();
    calCurrentTimestampMinusValue = (Calendar) calLastModifiedTimestamp.clone();
    // blを作成
    bl = new DateTimeUtil() {
      @Override
      protected Calendar getCurrentCal() {
        return calCurrentTimestampMinusValue;
      }
    };
  }

  /** */
  @Test
  public void test01_hasDesignatedTermPassed_秒_設定値_0_差_0() {
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.SECOND, 0));
  }

  /** */
  @Test
  public void test02_hasDesignatedTermPassed_秒_設定値_0_差_1() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.SECOND, -1);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.SECOND, 0));
  }

  /** */
  @Test
  public void test03_hasDesignatedTermPassed_秒_設定値_3_差_2() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.SECOND, -2);
    Assertions.assertEquals(false,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.SECOND, 3));
  }

  /** */
  @Test
  public void test04_hasDesignatedTermPassed_秒_設定値_3_差_3() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.SECOND, -3);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.SECOND, 3));
  }

  /** */
  @Test
  public void test05_hasDesignatedTermPassed_秒_設定値_3_差_1分() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MINUTE, -1);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.SECOND, 3));
  }

  /** */
  @Test
  public void test11_hasDesignatedTermPassed_分_設定値_0_差_0() {
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MINUTE, 0));
  }

  /** */
  @Test
  public void test12_hasDesignatedTermPassed_分_設定値_0_差_1() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MINUTE, -1);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MINUTE, 0));
  }

  /** */
  @Test
  public void test13_hasDesignatedTermPassed_分_設定値_3_差_2() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MINUTE, -2);
    Assertions.assertEquals(false,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MINUTE, 3));
  }

  /** */
  @Test
  public void test14_hasDesignatedTermPassed_分_設定値_3_差_3() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MINUTE, -3);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MINUTE, 3));
  }

  /** */
  @Test
  public void test15_hasDesignatedTermPassed_分_設定値_3_差_1時間() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.HOUR_OF_DAY, -1);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MINUTE, 3));
  }

  /** */
  @Test
  public void test16_hasDesignatedTermPassed_分_設定値_3_差_5秒() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.SECOND, -5);
    Assertions.assertEquals(false,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MINUTE, 3));
  }

  /** */
  @Test
  public void test21_hasDesignatedTermPassed_時_設定値_0_差_0() {
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.HOUR, 0));
  }

  /** */
  @Test
  public void test22_hasDesignatedTermPassed_時_設定値_0_差_1() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.HOUR_OF_DAY, -1);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.HOUR, 0));
  }

  /** */
  @Test
  public void test23_hasDesignatedTermPassed_時_設定値_3_差_2() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.HOUR_OF_DAY, -2);
    Assertions.assertEquals(false,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.HOUR, 3));
  }

  /** */
  @Test
  public void test24_hasDesignatedTermPassed_時_設定値_3_差_3() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.HOUR_OF_DAY, -3);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.HOUR, 3));
  }

  /** */
  @Test
  public void test25_hasDesignatedTermPassed_時_設定値_3_差_1日() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.DAY_OF_MONTH, -1);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.HOUR, 3));
  }

  /** */
  @Test
  public void test26_hasDesignatedTermPassed_時_設定値_3_差_5分() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MINUTE, -5);
    Assertions.assertEquals(false,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.HOUR, 3));
  }

  /** */
  @Test
  public void test31_hasDesignatedTermPassed_日_設定値_0_差_0() {
    Assertions.assertEquals(true, bl.hasDesignatedTermPassed(
        calLastModifiedTimestamp.getTimeInMillis(), Calendar.DAY_OF_MONTH, 0));
  }

  /** */
  @Test
  public void test32_hasDesignatedTermPassed_日_設定値_0_差_1() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.DAY_OF_MONTH, -1);
    Assertions.assertEquals(true, bl.hasDesignatedTermPassed(
        calLastModifiedTimestamp.getTimeInMillis(), Calendar.DAY_OF_MONTH, 0));
  }

  /** */
  @Test
  public void test33_hasDesignatedTermPassed_日_設定値_3_差_2() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.DAY_OF_MONTH, -2);
    Assertions.assertEquals(false, bl.hasDesignatedTermPassed(
        calLastModifiedTimestamp.getTimeInMillis(), Calendar.DAY_OF_MONTH, 3));
  }

  /** */
  @Test
  public void test34_hasDesignatedTermPassed_日_設定値_3_差_3() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.DAY_OF_MONTH, -3);
    Assertions.assertEquals(true, bl.hasDesignatedTermPassed(
        calLastModifiedTimestamp.getTimeInMillis(), Calendar.DAY_OF_MONTH, 3));
  }

  /** */
  @Test
  public void test35_hasDesignatedTermPassed_日_設定値_3_差_1カ月() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MONTH, -1);
    Assertions.assertEquals(true, bl.hasDesignatedTermPassed(
        calLastModifiedTimestamp.getTimeInMillis(), Calendar.DAY_OF_MONTH, 3));
  }

  /** */
  @Test
  public void test36_hasDesignatedTermPassed_日_設定値_3_差_5時間() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.HOUR_OF_DAY, -5);
    Assertions.assertEquals(false, bl.hasDesignatedTermPassed(
        calLastModifiedTimestamp.getTimeInMillis(), Calendar.DAY_OF_MONTH, 3));
  }

  /** */
  @Test
  public void test41_hasDesignatedTermPassed_月_設定値_0_差_0() {
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MONTH, 0));
  }

  /** */
  @Test
  public void test42_hasDesignatedTermPassed_月_設定値_0_差_1() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MONTH, -1);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MONTH, 0));
  }

  /** */
  @Test
  public void test43_hasDesignatedTermPassed_月_設定値_3_差_2() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MONTH, -2);
    Assertions.assertEquals(false,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MONTH, 3));
  }

  /** */
  @Test
  public void test44_hasDesignatedTermPassed_月_設定値_3_差_3() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MONTH, -3);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MONTH, 3));
  }

  /** */
  @Test
  public void test45_hasDesignatedTermPassed_月_設定値_3_差_1年() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.YEAR, -1);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MONTH, 3));
  }

  /** */
  @Test
  public void test46_hasDesignatedTermPassed_月_設定値_3_差_5日() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.DAY_OF_MONTH, -5);
    Assertions.assertEquals(false,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MONTH, 3));
  }

  /** */
  @Test
  public void test51_hasDesignatedTermPassed_年_設定値_0_差_0() {
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.MONTH, 0));
  }

  /** */
  @Test
  public void test52_hasDesignatedTermPassed_年_設定値_0_差_1() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.YEAR, -1);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.YEAR, 0));
  }

  /** */
  @Test
  public void test53_hasDesignatedTermPassed_年_設定値_3_差_2() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.YEAR, -2);
    Assertions.assertEquals(false,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.YEAR, 3));
  }

  /** */
  @Test
  public void test54_hasDesignatedTermPassed_年_設定値_3_差_3() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.YEAR, -3);
    Assertions.assertEquals(true,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.YEAR, 3));
  }

  /** */
  @Test
  public void test55_hasDesignatedTermPassed_年_設定値_3_差_5カ月() {
    // 差を指定
    calLastModifiedTimestamp.add(Calendar.MONTH, -5);
    Assertions.assertEquals(false,
        bl.hasDesignatedTermPassed(calLastModifiedTimestamp.getTimeInMillis(), Calendar.YEAR, 3));
  }
}
