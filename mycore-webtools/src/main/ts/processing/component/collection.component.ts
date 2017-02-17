import { Component, Input } from '@angular/core';
import { Collection } from '../model/model';

@Component( {
    selector: 'collection',
    templateUrl: 'html/collection.html',
    styleUrls:  ['css/collection.css']
})
export class CollectionComponent {

    @Input() model: Collection;

    constructor() {

    }

}
