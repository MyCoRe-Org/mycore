import { Pipe, PipeTransform } from '@angular/core';

@Pipe( {
    name: 'jsonString',
    pure: false
})
export class JsonStringPipe implements PipeTransform {

    transform( data: any ): any {
        return JsonStringPipe.toHTML( data );
    }

    static toHTML( json: any ): string {
        if ( json === undefined ) {
            return JsonStringPipe.formatNull( 'undefined' );
        }
        if ( json === null ) {
            return JsonStringPipe.formatNull( 'null' );
        }
        let t: string = typeof json;
        if ( t === 'boolean' || t === 'number' || t === 'string' || t === 'symbol' ) {
            return JsonStringPipe.formatPrimitve( json );
        }
        if ( t === 'function' ) {
            return JsonStringPipe.formatFunction( json );
        }
        let s: string = Object.prototype.toString.call( json );
        if ( s === '[object Date]' ) {
            return JsonStringPipe.formatDate( json );
        }
        if ( s === '[object Object]' ) {
            return JsonStringPipe.formatObject( json );
        }
        if ( s === '[object Array]' ) {
            return JsonStringPipe.formatArray( json );
        }
        return `unknown json ` + t + ` ` + s;
    }

    static formatNull( nullOrUndefined: any ): string {
        return `<div class='json-string-null'>` + nullOrUndefined + `</div>`;
    }

    static formatPrimitve( primitive: any ): string {
        return `<div class='json-string-primitive'>` + primitive + `</div>`;
    }

    static formatFunction( func: Function ): string {
        return `<div class='json-string-function'>` + func.name + `</div>`;
    }

    static formatDate( date: Date ): string {
        return `<div class='json-string-date'>` + date.toString() + `</div>`;
    }

    static formatObject( json: any ): string {
        let html = `<div class='json-string-object'>{`;
        for ( let key in json ) {
            if(!json.hasOwnProperty(key)) {
                continue;
            }
            html += `<div class='json-string-row json-string-object-row'>`;
            html += `<div class='json-string-object-property-name'>` + key + `</div>`;
            html += `<div class='json-string-object-property-value'>` + JsonStringPipe.toHTML( json[key] ) + `</div>`;
            html += `}</div>`;
        }
        html += `</div>`;
        return html;
    }

    static formatArray( array: Array<any> ): string {
        let html = `<div class='json-string-array'>[`;
        let length = array.length <= 10 ? array.length : 10;
        for ( let i = 0, len = length; i < len; i++ ) {
            html += `<div class='json-string-row json-string-array-entry'>` + this.toHTML( array[i] ) + `</div>`;
        }
        if(array.length > 10) {
            html += `<div class='json-string-row'>` + (array.length - 10) + ` more...</div>`;
        }
        html += `]</div>`;
        return html;
    }

}
