import { Component, Input } from '@angular/core';
import { Collection } from '../model/model';

@Component( {
    selector: 'collection',
    templateUrl: 'html/collection.html',
})
export class CollectionComponent {

    @Input() model: Collection;

    constructor() {

    }

}
