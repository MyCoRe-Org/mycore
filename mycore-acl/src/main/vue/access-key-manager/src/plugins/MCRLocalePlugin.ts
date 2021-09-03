/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import _Vue from 'vue';

export default new class MCRLocalePlugin {

  public async install(Vue: typeof _Vue, dict: Record<string, string>) {

    Vue.prototype.$t = function(key: string, ...args: string[]) {
      let str = dict[key];
      if (str != null) {
        for (let i = 0; i < args.length; i++) {
          str = str.replaceAll(`{${i}}`, args[i]);
        }
        return str;
      } else {
        // eslint-disable-next-line no-console
        console.error("unknown key: %s", key);
        return "";
      }
    }
  }
}
