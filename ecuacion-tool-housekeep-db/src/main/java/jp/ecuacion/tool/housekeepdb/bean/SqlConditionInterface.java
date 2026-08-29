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
 * Proivdes interface for an SQL condition, usable as one item of a WHERE / SET clause.
 *
 * <p>A condition is either bound (its {@link #getSqlFragment()} contains exactly one {@code ?}
 *     placeholder and {@link #getBindValue()} returns the value to bind there - see
 *     {@link BoundCondition}) or literal (the value is embedded directly in the fragment text and
 *     {@link #getBindValue()} returns {@code null}). {@link SqlUtil#getWhere} /
 *     {@link SqlUtil#getUpdateSet} rely on this: they join every fragment in list order to build
 *     the SQL text, and separately collect the non-null bind values in that same order, so the
 *     Nth {@code ?} in the joined text always lines up with the Nth collected value.</p>
 */
public interface SqlConditionInterface {

  /**
   * Builds and returns a condition part of a WHERE / SET clause - either {@code "column = ?"}
   * (bound) or {@code "column = 'literal'"} (literal), depending on the implementation.
   *
   * @return condition fragment
   */
  public String getSqlFragment();

  /**
   * Returns the value to bind to this fragment's {@code ?} placeholder, or {@code null} if the
   * fragment has no placeholder (the value, if any, is already embedded in the fragment text).
   *
   * @return the bind value, or {@code null}
   */
  public @Nullable Object getBindValue();
}
