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

import java.util.Map;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;

/**
 * @author 庸介
 *
 */
public class ParameterUtil {

  /* ■□■□ unixパラメータ関連 ■□■□ */

  /**
   * UNIXシェルの変数${VAR}のパラメータを含む文字列を引数に渡すと、最初のパラメータを戻す。<br>
   * 戻り値は「${VAR}」ではなく「VAR」なので注意。
   */
  public String getFirstUnixEnvVar(String str) throws BizLogicAppException {
    int pathVarStartIndex = 0;
    int pathVarEndIndex = 0;
    // 「${VAR}」を抽出
    // 開始位置と終了位置をそれぞれ取得
    pathVarStartIndex = str.indexOf("${");
    pathVarEndIndex = str.indexOf("}");
    // 変数が存在しない場合はnullを返す
    if (pathVarEndIndex <= 0) {
      return null;
    }

    // startIndexがendIndexより大きい場合はエラー
    if (pathVarStartIndex > pathVarEndIndex) {
      throw new BizLogicAppException("MSG_ERR_UNIX_ENV_FORMAT_INCORRECT", str);
    }

    // 戻り値は「VAR」の形で返す
    return str.substring(pathVarStartIndex + 2, pathVarEndIndex);
  }

  /**
   * UNIXシェルの変数${VAR}のパラメータを含む文字列と、そのパラメータをキーに持つMapを引数に渡すと、 パラメータを代入した文字列を返す。
   */
  public String substituteUnixEnvVars(String str, Map<String, String> map)
      throws BizLogicAppException {
    String var = null;
    while (true) {
      var = getFirstUnixEnvVar(str);

      // var == null、つまり変数が文字列中に存在しない場合は終了
      if (var == null) {
        break;
      }

      // キーがmapの中に含まれていない場合はエラー
      if (!map.containsKey(var)) {
        throw new BizLogicAppException("MSG_ERR_MAP_DOESNT_CONTAIN_PARAM_KEY", var);
      }

      // 変数を代入
      str = str.replace("${" + var + "}", map.get(var));
    }

    return str;
  }
}
