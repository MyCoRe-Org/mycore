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

import {Util} from '../util';
import {Settings} from '../settings';

export class Registry {

    protected collections: Collection[] = [];

    public getCollection(id: number): Collection {
        return this.collections.find((collection) => {
            return collection.id === id;
        });
    }

    public addCollection(collectionData: any): void {
        const collection = new Collection();
        Util.mixin(collectionData, collection);
        collection.updatePropertyKeys();
        this.collections.push(collection);
    }

    public updateProcessable(processableData: any): void {
        const collection = this.getCollection(processableData.collectionId);
        if (collection == null) {
            console.warn('Unable to find collection with id ' + processableData.collectionId);
            return;
        }
        collection.updateProcessable(processableData);
    }

}

export class Collection {

    public id: number;

    public name: string;

    /**
     * Array for fast access.
     */
    public processables: Processable[] = [];

    public processingProcessables: Processable[] = [];

    public createdProcessables: Processable[] = [];

    public finishedProcessables: Processable[] = [];

    public properties: { [name: string]: any } = {};

    /**
     * Double storage of property keys due performance reasons.
     */
    public propertyKeys: string[] = [];

    /**
     * Returns the amount of processables with the status === 'created' of this collection.
     *
     * @returns array of processables
     */
    public getCreatedProcessables(amount: number): Processable[] {
        if (amount >= this.createdProcessables.length) {
            return this.createdProcessables;
        }
        return this.createdProcessables.slice(0, amount);
    }

    public updateProcessable(processableData: any): void {
        const oldProcessable = this.processables[processableData.id];
        if (oldProcessable == null) {
            // new
            this.addProcessable(processableData);
        } else {
            // update
            if (oldProcessable.status === 'processing' && processableData.status !== 'processing') {
                Util.remove(this.processingProcessables, oldProcessable);
                this.addProcessable(processableData);
            } else if (oldProcessable.status === 'created' && processableData.status !== 'created') {
                Util.remove(this.createdProcessables, oldProcessable);
                this.addProcessable(processableData);
            } else {
                Util.mixin(processableData, oldProcessable);
                oldProcessable.updatePropertyKeys();
            }
        }
    }

    public addProcessable(processableData: any) {
        const processable = new Processable();
        Util.mixin(processableData, processable);
        this.processables[processable.id] = processable;
        if (processable.status === 'processing') {
            this.processingProcessables.push(processable);
        } else if (processable.status === 'created') {
            this.createdProcessables.push(processable);
        } else {
            const maxNumberOfFinishedProcess = Number.parseInt(Settings.get('maxNumberFinished', 50));
            if (maxNumberOfFinishedProcess !== -1) {
                while (this.finishedProcessables.length >= maxNumberOfFinishedProcess) {
                    this.finishedProcessables.shift();
                }
            }
            this.finishedProcessables.push(processable);
        }
        processable.updatePropertyKeys();
    }

    public setProperty(name: string, value: any): void {
        const updateKeys: boolean = name in this.properties;
        this.properties[name] = value;
        if (updateKeys) {
            this.updatePropertyKeys();
        }
    }

    /**
     * Should be called every time the properties object is updated.
     */
    public updatePropertyKeys(): void {
        this.propertyKeys = Object.keys(this.properties);
    }

}

export class Processable {

    public id: number;

    public collectionId: number;

    public name: string;

    public status: string;

    public createTime: number;

    public startTime: number;

    public endTime: number;

    public took: number;

    public error: string;

    public progress: number;

    public progressText: string;

    public properties: { [name: string]: any } = {};

    /**
     * Double storage of property keys due performance reasons.
     */
    public propertyKeys: string[] = [];

    public hasProperties() {
        return Object.keys(this.properties).length > 0;
    }

    /**
     * Should be called every time the properties object is updated.
     */
    public updatePropertyKeys(): void {
        this.propertyKeys = Object.keys(this.properties);
    }

}
