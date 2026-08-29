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
package jp.ecuacion.tool.housekeepdb.bean;

import org.jspecify.annotations.Nullable;

/**
 * Stores a pre-built, literal condition string with no bind value - e.g. the java-computed
 * expiration-check inequality from {@code SqlUtil#getExpirationCondition}, which embeds only
 * values this tool itself formatted (not attacker- or even excel-config-controlled), so there is
 * nothing to bind.
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
  public String getSqlFragment() {
    return conditionString;
  }

  @Override
  public @Nullable Object getBindValue() {
    return null;
  }
}
