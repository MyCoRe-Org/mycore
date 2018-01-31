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

import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
    name: 'jsonString',
    pure: false
})
export class JsonStringPipe implements PipeTransform {

    public transform(data: any): any {
        return JsonStringPipe.toHTML(data);
    }

    protected static toHTML(json: any): string {
        if (json === undefined) {
            return JsonStringPipe.formatNull('undefined');
        }
        if (json === null) {
            return JsonStringPipe.formatNull('null');
        }
        const t: string = typeof json;
        if (t === 'boolean' || t === 'number' || t === 'string' || t === 'symbol') {
            return JsonStringPipe.formatPrimitve(json);
        }
        if (t === 'function') {
            return JsonStringPipe.formatFunction(json);
        }
        const s: string = Object.prototype.toString.call(json);
        if (s === '[object Date]') {
            return JsonStringPipe.formatDate(json);
        }
        if (s === '[object Object]') {
            return JsonStringPipe.formatObject(json);
        }
        if (s === '[object Array]') {
            return JsonStringPipe.formatArray(json);
        }
        return `unknown json ` + t + ` ` + s;
    }

    protected static formatNull(nullOrUndefined: any): string {
        return `<div class='json-string-null'>` + nullOrUndefined + `</div>`;
    }

    protected static formatPrimitve(primitive: any): string {
        return `<div class='json-string-primitive'>` + primitive + `</div>`;
    }

    protected static formatFunction(func: Function): string {
        return `<div class='json-string-function'>` + func.name + `</div>`;
    }

    protected static formatDate(date: Date): string {
        return `<div class='json-string-date'>` + date.toString() + `</div>`;
    }

    protected static formatObject(json: any): string {
        let html = `<div class='json-string-object'>{`;
        for (const key in json) {
            if (!json.hasOwnProperty(key)) {
                continue;
            }
            html += `<div class='json-string-row json-string-object-row'>`;
            html += `<div class='json-string-object-property-name'>` + key + `</div>`;
            html += `<div class='json-string-object-property-value'>` + JsonStringPipe.toHTML(json[key]) + `</div>`;
            html += `}</div>`;
        }
        html += `</div>`;
        return html;
    }

    protected static formatArray(array: any[]): string {
        let html = `<div class='json-string-array'>[`;
        const length = array.length <= 10 ? array.length : 10;
        for (let i = 0; i < length; i++) {
            html += `<div class='json-string-row json-string-array-entry'>` + this.toHTML(array[i]) + `</div>`;
        }
        if (array.length > 10) {
            html += `<div class='json-string-row'>` + (array.length - 10) + ` more...</div>`;
        }
        html += `]</div>`;
        return html;
    }

}
