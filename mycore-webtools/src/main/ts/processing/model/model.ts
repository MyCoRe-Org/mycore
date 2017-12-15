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

import {Util} from "../util";
import {Settings} from "../settings";

export class Registry {

    collections: Array<Collection> = [];

    public getCollection(id: number): Collection {
        return this.collections.find((collection) => {
            return collection.id == id;
        });
    }

    public addCollection(collectionData: any): void {
        let collection = new Collection();
        Util.mixin(collectionData, collection);
        collection.updatePropertyKeys();
        this.collections.push(collection);
    }

    public updateProcessable(processableData: any): void {
        let collection = this.getCollection(processableData.collectionId);
        if (collection == null) {
            console.log("Unable to find collection with id " + processableData.collectionId);
            return;
        }
        collection.updateProcessable(processableData);
    }

}

export class Collection {

    id: number;

    name: string;

    /**
     * Array for fast access.
     */
    processables: Array<Processable> = [];

    processingProcessables: Array<Processable> = [];

    createdProcessables: Array<Processable> = [];

    finishedProcessables: Array<Processable> = [];

    properties: { [name: string]: any } = {};

    /**
     * Double storage of property keys due performance reasons.
     */
    propertyKeys: Array<string> = [];

    /**
     * Returns the amount of processables with the status === 'created' of this collection.
     *
     * @returns {Array<Processable>} array of processables
     */
    public getCreatedProcessables(amount: number): Array<Processable> {
        if (amount >= this.createdProcessables.length) {
            return this.createdProcessables;
        }
        return this.createdProcessables.slice(0, amount);
    }

    public updateProcessable(processableData: any): void {
        let oldProcessable = this.processables[processableData.id];
        if (oldProcessable == null) {
            // new
            this.addProcessable(processableData);
        } else {
            // update
            if (oldProcessable.status === "processing" && processableData.status !== "processing") {
                Util.remove(this.processingProcessables, oldProcessable);
                this.addProcessable(processableData);
            } else if (oldProcessable.status === "created" && processableData.status !== "created") {
                Util.remove(this.createdProcessables, oldProcessable);
                this.addProcessable(processableData);
            } else {
                Util.mixin(processableData, oldProcessable);
                oldProcessable.updatePropertyKeys();
            }
        }
    }

    public addProcessable(processableData: any) {
        let processable = new Processable();
        Util.mixin(processableData, processable);
        this.processables[processable.id] = processable;
        if (processable.status === "processing") {
            this.processingProcessables.push(processable);
        } else if (processable.status === "created") {
            this.createdProcessables.push(processable);
        } else {
            let maxNumberOfFinishedProcess = Number.parseInt(Settings.get("maxNumberFinished", 50));
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
        let updateKeys: boolean = name in this.properties;
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

    id: number;

    collectionId: number;

    name: string;

    status: string;

    createTime: number;

    startTime: number;

    endTime: number;

    took: number;

    error: string;

    progress: number;

    progressText: string;

    properties: { [name: string]: any } = {};

    /**
     * Double storage of property keys due performance reasons.
     */
    propertyKeys: Array<string> = [];

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
