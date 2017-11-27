
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

export class Registry {

    collections: Array<Collection> = [];

    public getCollection( id: number ): Collection {
        return this.collections.find(( collection ) => {
            return collection.id == id;
        });
    }

    public getProcessable( id: number ): Processable {
        for ( var collection of this.collections ) {
            var processable: Processable = collection.getProcessable( id );
            if ( processable != null ) {
                return processable;
            }
        }
        return null;
    }

    public removeProcessable( id: number ): Processable {
        for ( var collection of this.collections ) {
            var oldProcessable: Processable = collection.removeProcessable( id );
            if ( oldProcessable != null ) {
                return oldProcessable;
            }
        }
        return null;
    }

}

export class Collection {

    id: number;

    name: string;

    processables: Array<Processable> = [];

    properties: {[name: string]: any} = {};

    /**
     * Double storage of property keys due performance reasons.
     */
    propertyKeys: Array<string> = [];

    /**
     * Returns the first processables of this collection
     *
     * @param {number} amount the amount to return
     * @returns {Array<Processable>} array of processables
     */
    public getProcessables(amount: number): Array<Processable> {
        if (amount >= this.processables.length) {
            return this.processables;
        }
        return this.processables.slice(0, amount);
    }

    public getProcessable( id: number ): Processable {
        for (let processable of this.processables) {
            if ( processable.id == id ) {
                return processable;
            }
        }
        return null;
    }

    public removeProcessable( id: number ): Processable {
        let oldProcessable: Processable = null;
        this.processables = this.processables.filter( p => {
            if ( p.id == id ) {
                oldProcessable = p;
            }
            return p.id !== id;
        });
        return oldProcessable;
    }

    public setProperty(name: string, value: any): void {
        let updateKeys: boolean = name in this.properties;
        this.properties[name] = value;
        if(updateKeys) {
            this.updatePropertyKeys();
        }
    }

    /**
     * Should be called every time the properties object
     * is updated.
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

}
