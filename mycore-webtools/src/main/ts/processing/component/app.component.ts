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

import {Component, ViewEncapsulation, ChangeDetectorRef} from '@angular/core';

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

    private dirty: boolean;

    constructor(private processingService: ProcessingService, private changeDetector: ChangeDetectorRef) {
        this.dirty = false;
        this.connect();
        this.changeDetector.detach();
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

    private triggerDelayedUpdate() {
        if (this.dirty) {
            return;
        }
        this.dirty = true;
        window.requestAnimationFrame(() =>{
            this.changeDetector.detectChanges();
            this.dirty = false;
        });
    }

    protected handleMessage(data: any) {
        switch (data.type) {
            case 'error':
                this.errorCode = parseInt(data.error);
                this.changeDetector.detectChanges();
                break;
            case 'registry':
                this.errorCode = null;
                this.registry = new Registry();
                this.changeDetector.detectChanges();
                break;
            case 'addCollection':
                this.registry.addCollection(data);
                this.changeDetector.detectChanges();
                break;
            case 'updateProcessable':
                this.registry.updateProcessable(data);
                this.triggerDelayedUpdate();
                break;
            case 'updateCollectionProperty':
                const collection: Collection = this.registry.getCollection(data.id);
                if (collection == null) {
                    console.warn('Unable to find collection with id ' + data.id);
                    return;
                }
                collection.setProperty(data.propertyName, data.propertyValue);
                this.triggerDelayedUpdate();
                break;
            default:
                console.warn('Unable to handle data type: ' + data.type);
        }
    }

    public settings(): Settings {
        return Settings;
    }

}
