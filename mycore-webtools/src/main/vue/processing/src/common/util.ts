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

export class Util {

  public static mixin(source: any, target: any) {
    for (const prop in source) {
      if (source.hasOwnProperty(prop)) {
        target[prop] = source[prop];
      }
    }
  }

  public static remove(array: any[], element: any) {
    const index = array.indexOf(element);
    if (index > -1) {
      array.splice(index, 1);
    }
  }

  public static getBasePath(path: String) {
    const pathArray = location.pathname.split('/');
    pathArray.splice(-4);
    return pathArray.join('/');
  }

  public static useJsonString(data: any): string {
    function toHTML(json: any): string {
      if (json === undefined) {
        return formatNull('undefined');
      }
      if (json === null) {
        return formatNull('null');
      }
      const t = typeof json;
      if (t === 'boolean' || t === 'number' || t === 'string' || t === 'symbol') {
        return formatPrimitive(json);
      }
      if (t === 'function') {
        return formatFunction(json);
      }
      const s = Object.prototype.toString.call(json);
      if (s === '[object Date]') {
        return formatDate(json);
      }
      if (s === '[object Object]') {
        return formatObject(json);
      }
      if (s === '[object Array]') {
        return formatArray(json);
      }
      return `unknown json ${t} ${s}`;
    }

    function formatNull(nullOrUndefined: any): string {
      return `<div class='json-string-null'>${nullOrUndefined}</div>`;
    }

    function formatPrimitive(primitive: any): string {
      return `<div class='json-string-primitive'>${primitive}</div>`;
    }

    function formatFunction(func: Function): string {
      return `<div class='json-string-function'>${func.name}</div>`;
    }

    function formatDate(date: Date): string {
      return `<div class='json-string-date'>${date.toString()}</div>`;
    }

    function formatObject(json: any): string {
      let html = `<div class='json-string-object'>{`;
      for (const key in json) {
        if (!json.hasOwnProperty(key)) continue;
        html += `<div class='json-string-row json-string-object-row'>`;
        html += `<div class='json-string-object-property-name'>${key}</div>`;
        html += `<div class='json-string-object-property-value'>${toHTML(json[key])}</div>`;
        html += `}</div>`;
      }
      html += `</div>`;
      return html;
    }

    function formatArray(array: any[]): string {
      let html = `<div class='json-string-array'>[`;
      const length = array.length <= 10 ? array.length : 10;
      for (let i = 0; i < length; i++) {
        html += `<div class='json-string-row json-string-array-entry'>${toHTML(array[i])}</div>`;
      }
      if (array.length > 10) {
        html += `<div class='json-string-row'>${array.length - 10} more...</div>`;
      }
      html += `]</div>`;
      return html;
    }

    return toHTML(data);
  }

}
