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

import {Component, ViewEncapsulation} from '@angular/core';

import {ProcessingService} from './../service/processing.service';
import {Settings} from './../settings';
import {Registry, Collection} from '../model/model';

@Component({
    selector: 'processing',
    templateUrl: 'html/app.html',
    styleUrls: ['css/app.css'],
    encapsulation: ViewEncapsulation.None,
    providers: [ProcessingService]
})
export class AppComponent {

    public errorCode: number;

    public registry: Registry;

    constructor(private processingService: ProcessingService) {
        this.connect();
    }

    public connect() {
        this.processingService.connect();
        this.processingService.observable.subscribe(
            (me: MessageEvent) => this.handleSubscription(me),
            (error: any) => this.handleError(error),
            () => this.handleDone()
        );
    }

    protected handleSubscription(messageEvent: MessageEvent) {
        this.handleMessage(JSON.parse(messageEvent.data));
    }

    protected handleError(error: any) {
        console.error(error);
    }

    protected handleDone() {
        this.connect();
    }

    protected handleMessage(data: any) {
        const dataType = data.type;
        if (dataType === 'error') {
            this.errorCode = parseInt(data.error);
            return;
        }
        if (dataType === 'registry') {
            this.errorCode = null;
            this.registry = new Registry();
        }
        if (dataType === 'addCollection') {
            this.registry.addCollection(data);
        }
        if (dataType === 'updateProcessable') {
            this.registry.updateProcessable(data);
        }
        if (dataType === 'updateCollectionProperty') {
            const collection: Collection = this.registry.getCollection(data.id);
            if (collection == null) {
                console.warn('Unable to find collection with id ' + data.id);
                return;
            }
            collection.setProperty(data.propertyName, data.propertyValue);
        }
    }

    public settings(): Settings {
        return Settings;
    }

}
