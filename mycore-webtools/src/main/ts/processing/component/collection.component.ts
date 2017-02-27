import { Component, Input, SimpleChanges } from '@angular/core';
import { Collection } from '../model/model';
import { HtmlService } from './../service/html.service';
import { JsonStringPipe } from './../pipe/json.pipe';

@Component( {
    selector: 'collection',
    templateUrl: 'html/collection.html',
    styleUrls: ['css/collection.css'],
    providers: [HtmlService]
})
export class CollectionComponent {

    @Input() model: Collection;

    constructor( private html: HtmlService ) {
    }

}
