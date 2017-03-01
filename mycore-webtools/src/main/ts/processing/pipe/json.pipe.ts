import { Pipe, PipeTransform } from '@angular/core';

@Pipe( {
    name: 'jsonString',
    pure: false
})
export class JsonStringPipe implements PipeTransform {

    transform( data: any ): any {
        return this.toHTML( data );
    }

    toHTML( json: any ): string {
        if ( json === undefined ) {
            return this.formatNull( 'undefined' );
        }
        if ( json === null ) {
            return this.formatNull( 'null' );
        }
        var t: string = typeof json;
        if ( t === 'boolean' || t === 'number' || t === 'string' || t === 'symbol' ) {
            return this.formatPrimitve( json );
        }
        if ( t === 'function' ) {
            return this.formatFunction( json );
        }
        var s: string = Object.prototype.toString.call( json );
        if ( s === '[object Date]' ) {
            return this.formatDate( json );
        }
        if ( s === '[object Object]' ) {
            return this.formatObject( json );
        }
        if ( s === '[object Array]' ) {
            return this.formatArray( json );
        }
        return `unknown json ` + t + ` ` + s;
    }

    formatNull( nullOrUndefined: any ): string {
        return `<div class='json-string-null'>` + nullOrUndefined + `</div>`;
    }

    formatPrimitve( primitive: any ): string {
        return `<div class='json-string-primitive'>` + primitive + `</div>`;
    }

    formatFunction( func: Function ): string {
        return `<div class='json-string-function'>` + func.name + `</div>`;
    }

    formatDate( date: Date ): string {
        return `<div class='json-string-date'>` + date.toString() + `</div>`;
    }

    formatObject( json: any ): string {
        var html = `<div class='json-string-object'>{`;
        for ( var key in json ) {
            html += `<div class='json-string-row json-string-object-row'>`;
            html += `<div class='json-string-object-property-name'>` + key + `</div>`;
            html += `<div class='json-string-object-property-value'>` + this.toHTML( json[key] ) + `</div>`;
            html += `}</div>`;
        }
        html += `</div>`;
        return html;
    }

    formatArray( array: Array<any> ): string {
        var html = `<div class='json-string-array'>[`;
        for ( var i = 0, len = array.length; i < len; i++ ) {
            html += `<div class='json-string-row json-string-array-entry'>` + this.toHTML( array[i] ) + `</div>`;
        }
        html += `]</div>`;
        return html;
    }


}