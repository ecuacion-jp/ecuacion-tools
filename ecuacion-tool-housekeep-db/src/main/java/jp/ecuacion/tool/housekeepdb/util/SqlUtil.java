package jp.ecuacion.tool.housekeepdb.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;

/**
 * Provides utilities to build sql sentence.
 */
public class SqlUtil {

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

    sb.append("\nwhere ");
    sb.append(StringUtil.getSeparatedValuesString(
        list.stream().map(bean -> bean.getCondition()).toList(), " and "));

    return sb.toString();
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
