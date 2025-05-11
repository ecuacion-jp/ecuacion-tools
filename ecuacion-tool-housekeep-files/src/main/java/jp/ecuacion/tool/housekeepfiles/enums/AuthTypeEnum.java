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
package jp.ecuacion.tool.housekeepfiles.enums;


import java.util.Locale;
import jp.ecuacion.lib.core.util.PropertyFileUtil;

/** 
 * 
 */
public enum AuthTypeEnum {

  /** 
   * 
   */
  PASSWORD("01"),

  /** 
   * 
   */
  KEY("11"),

  /** 
   * 
   */
  KERBEROS("21");

  private String code;

  private AuthTypeEnum(String code) {
    this.code = code;
  }

  /**
   * codeを返す。 codeがnull, 空文字の場合は、Enum生成時にチェックエラーとなるため考慮不要
   */
  public String getCode() {
    return code;
  }

  /**
   * nameを返す。 nameがnull, 空文字の場合は、Enum生成時にチェックエラーとなるため考慮不要
   */
  public String getName() {
    return this.toString();
  }

  /**
   * 画面で表示するための名称を返す。 この名称は、getはできるがそれをもとにenumを取得することはできない。 localizeされた言語で返す。
   * 明らかに日本語専用のサイトを作成する場合も多いし、その場合にこの仕組みのほうが楽なので。 またどこかで変わるかもしれないけど。
   */
  public String getDispName(Locale locale) {
    return PropertyFileUtil.getEnumName(locale,
        this.getClass().getSimpleName() + "." + this.toString());
  }

  /**
   * defaultのLocaleを使用。
   */
  public String getDispName() {
    return PropertyFileUtil.getEnumName(Locale.getDefault(),
        this.getClass().getSimpleName() + "." + this.toString());
  }

  /**
   * 引数のcodeがEnum内に存在すればtrue、しなければfalseを返す。<br>
   * codeがnullまたは空文字の場合はfalseを返す。
   */
  public static boolean hasEnum(String code) {
    for (AuthTypeEnum enu : AuthTypeEnum.values()) {
      if (code != null && code.equals(enu.getCode())) {
        return true;
      }
    }

    return false;
  }

  /**
   * 引数のnameがEnum内に存在すればtrue、しなければfalseを返す。<br>
   * nameがnullまたは空文字の場合はfalseを返す。
   */
  public static boolean hasEnumFromName(String name) {
    for (AuthTypeEnum enu : AuthTypeEnum.values()) {
      if (name != null && name.equals(enu.getName())) {
        return true;
      }
    }

    return false;
  }
}
