/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import { Util } from '../common/util';
import { Settings } from '../common/settings';
import type {AddCollectionMessage, UpdateProcessableMessage} from "./messages.ts";

export class Registry {

  public collections: Collection[] = [];

  public getCollection(id: number): Collection | undefined {
    return this.collections.find((collection) => {
      return collection.id === id;
    });
  }

  public addCollection(collectionData: AddCollectionMessage): void {
    const collection = new Collection();
    Util.mixin(collectionData, collection);
    collection.updatePropertyKeys();
    this.collections.push(collection);
  }

  public updateProcessable(processableMessage: UpdateProcessableMessage): void {
    const collection = this.getCollection(processableMessage.collectionId);
    if (collection == null) {
      console.warn('Unable to find collection with id ' + processableMessage.collectionId);
      return;
    }
    collection.updateProcessable(processableMessage);
  }

}

export class Collection {

  public id: number | undefined;

  public name: string | undefined;

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

  public updateProcessable(processableMessage: UpdateProcessableMessage): void {
    const oldProcessable = this.processables[processableMessage.id];
    if (oldProcessable == null) {
      // new
      this.addProcessable(processableMessage);
    } else {
      // update
      if (oldProcessable.status === 'processing' && processableMessage.status !== 'processing') {
        Util.remove(this.processingProcessables, oldProcessable);
        this.addProcessable(processableMessage);
      } else if (oldProcessable.status === 'created' && processableMessage.status !== 'created') {
        Util.remove(this.createdProcessables, oldProcessable);
        this.addProcessable(processableMessage);
      } else {
        Util.mixin(processableMessage, oldProcessable);
        oldProcessable.updatePropertyKeys();
      }
    }
  }

  public addProcessable(processableMessage: UpdateProcessableMessage) {
    const processable = new Processable();
    Util.mixin(processableMessage, processable);
    this.processables[processableMessage.id] = processable;
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

  public id: number | undefined;

  public collectionId: number | undefined;

  public name: string | undefined;

  public status: string | undefined;

  public user: string | undefined;

  public createTime: number | undefined;

  public startTime: number | undefined;

  public endTime: number | undefined;

  public took: number | undefined;

  public error: string | undefined;

  public progress: number | undefined;

  public progressText: string | undefined;

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
