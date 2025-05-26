package jp.ecuacion.tool.housekeepdb.bean;

/**
 * Proivdes interface for an SQL condition.
 */
public interface SqlConditionInterface {
  
  /**
   * Builds string and returns a condition part o fcondition statement.
   * 
   * @return condition string
   */
  public String getCondition();
}
