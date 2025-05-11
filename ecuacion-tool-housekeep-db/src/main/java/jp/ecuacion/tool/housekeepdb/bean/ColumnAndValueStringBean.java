package jp.ecuacion.tool.housekeepdb.bean;

public class ColumnAndValueStringBean implements SqlConditionInterface {

  private String conditionString;

  public ColumnAndValueStringBean(String conditionString) {
    this.conditionString = conditionString;
  }

  @Override
  public String getCondition() {
    return conditionString;
  }

}
