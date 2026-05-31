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
package jp.ecuacion.tool.housekeepdb.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides utilities to build sql sentence.
 */
public class SqlUtil {

  /**
   * Prevents other classes from instantiating it.
   */
  private SqlUtil() {

  }

  /**
   * Provides current date-time string considering database kinds.
   * 
   * @param protocol database kind like 'postgresql'
   * @return date-time string
   */
  public static String getTimestampNow(String protocol) {
    if (protocol.equals("postgresql")) {
      return OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

    } else {
      throw new RuntimeException("Protocol not recognized. protocol: " + protocol);
    }
  }

  /**
   * Creates where clause.
   * 
   * @param list a list of {@code SqlConditionInterface}
   * @return where clause
   */
  public static String getWhere(List<SqlConditionInterface> list) {
    StringBuilder sb = new StringBuilder();

    sb.append(StringUtil.getSeparatedValuesString(
        list.stream().map(bean -> bean.getCondition()).toList(), " and "));

    return (StringUtils.isEmpty(sb.toString()) ? "" : "\nwhere ") +  sb.toString();
  }

  /**
   * Creates where clause.
   * 
   * @param array an array of {@code SqlConditionInterface}
   * @return where clause
   */
  public static String getWhere(SqlConditionInterface... array) {
    return getWhere(Arrays.asList(array));
  }

  /**
   * Creates set clause in update sentence.
   * 
   * @param list a list of {@code SqlConditionInterface}
   * @return set clause
   */
  @SuppressWarnings("null")
  public static String getUpdateSet(List<SqlConditionInterface> list) {
    StringBuilder sb = new StringBuilder();

    sb.append("\nset ");
    sb.append(StringUtil.getCsvWithSpace(list.stream().map(bean -> bean.getCondition()).toList()));

    return sb.toString();
  }

  /**
   * Creates set clause in update sentence.
   * 
   * @param array an array of {@code SqlConditionInterface}
   * @return set clause
   */
  public static String getUpdateSet(SqlConditionInterface... array) {
    return getUpdateSet(Arrays.asList(array));
  }
}
