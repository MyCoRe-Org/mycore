/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

import { MyCoReMap, ViewerFormatString } from "../../Utils";

export class LanguageModel {

  constructor(private _keyTranslationMap: MyCoReMap<string, string>) {
  }

  public getTranslation(key: string) {
    return this._keyTranslationMap.has(key) ? this._keyTranslationMap.get(key) : "???" + key + "???";
  }

  public getFormatedTranslation(key: string, ...format: string[]) {
    return this._keyTranslationMap.has(key) ? ViewerFormatString(this._keyTranslationMap.get(key), format) : "???" + key + "??? " + format.join(" ");
  }

  public hasTranslation(key: string) {
    return this._keyTranslationMap.has(key);
  }

  public translate(element: HTMLElement) {
    let that = this;
    let elements = element.querySelectorAll("[data-i18n]");
    for (let i = 0; i < elements.length; i++) {
      let sub = elements[i];
      let key: string = sub.getAttribute("data-i18n");
      if (!that.hasTranslation(key)) {
        continue;
      }
      sub.innerHTML = that.getTranslation(key);
    }
  }

}

