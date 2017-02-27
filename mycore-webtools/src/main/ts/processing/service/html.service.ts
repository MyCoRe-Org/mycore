import { Injectable } from '@angular/core';

@Injectable()
export class HtmlService {

    keys( object: {}) {
        return Object.keys( object );
    }

}
