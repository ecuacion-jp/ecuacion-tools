package jp.ecuacion.tool.housekeepdb.bean;

/**
 * Stores condition string.
 */
public class ColumnAndValueStringBean implements SqlConditionInterface {

  private String conditionString;

  /**
   * Constructs a new instance.
   * 
   * @param conditionString conditionString
   */
  public ColumnAndValueStringBean(String conditionString) {
    this.conditionString = conditionString;
  }

  @Override
  public String getCondition() {
    return conditionString;
  }

}
