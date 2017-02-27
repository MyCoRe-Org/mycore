import { Component, ViewChild, ViewEncapsulation } from '@angular/core';

import { CollectionComponent } from './collection.component';
import { ProcessingService } from './../service/processing.service';
import { Registry, Collection, Processable } from '../model/model';

import { Subject } from 'rxjs/Rx';

@Component( {
    selector: 'processing',
    templateUrl: 'html/app.html',
    styleUrls: ['css/app.css'],
    encapsulation: ViewEncapsulation.None,
    providers: [ProcessingService]
})
export class AppComponent {

    errorCode: number;

    registry: Registry;

    constructor( private processingService: ProcessingService ) {
        this.connect();
    }

    connect() {
        this.processingService.connect();
        this.processingService.observable.subscribe(
            ( me: MessageEvent ) => this.handleSubscription( me ),
            ( error: any ) => this.handleError( error ),
            () => this.handleDone()
        );
    }

    handleSubscription( messageEvent: MessageEvent ) {
        this.handleMessage( JSON.parse( messageEvent.data ) );
    }

    handleError( error: any ) {
        console.log( error );
    }

    handleDone() {
        this.connect();
    }

    handleMessage( data: any ) {
        var dataType = data.type;

        if ( dataType == "error" ) {
            this.errorCode = parseInt( data.error );
            return;
        }
        if ( dataType == "registry" ) {
            this.errorCode = null;
            this.registry = new Registry();
        }
        if ( dataType == "addCollection" ) {
            var collection: Collection = new Collection();
            this.mixin( data, collection );
            collection.updatePropertyKeys();
            this.registry.collections.push( collection );
        }
        if ( dataType == "addProcessable" ) {
            var processable: Processable = new Processable();
            this.mixin( data, processable );
            var collection = this.registry.getCollection( data.collectionId );
            if ( collection == null ) {
                console.log( "Unable to find collection with id " + data.collectionId );
                return;
            }
            collection.processables.push( processable );
        }
        if ( dataType == "updateProcessable" ) {
            var processable: Processable = this.registry.getProcessable( data.id );
            if ( processable == null ) {
                console.log( "Unable to find processable with id " + data.id );
                return;
            }
            this.mixin( data, processable );
        }
        if ( dataType == "removeProcessable" ) {
            var oldProcessable: Processable = this.registry.removeProcessable( data.id );
            if ( oldProcessable == null ) {
                console.log( "Unable to remove processable with id " + data.id + " cause its not in any collection." );
            }
        }
        if( dataType == "updateCollectionProperty") {
            var collection: Collection = this.registry.getCollection(data.id);
            if(collection == null) {
                console.log("Unable to find collection with id " + data.id);
                return;
            }
            collection.setProperty(data.propertyName, data.propertyValue);
        }
    }

    mixin( source: any, target: any ) {
        for ( var prop in source ) {
            if ( source.hasOwnProperty( prop ) ) {
                target[prop] = source[prop];
            }
        }
    }

}
