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

/// <reference path="../../../Utils.ts" />
/// <reference path="ToolbarComponent.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class ToolbarGroup {

        constructor(private _name: string, private _right: boolean = false) {
            this._idComponentMap = new MyCoReMap<string, ToolbarComponent>();
            this._observerArray = new Array<ContainerObserver<ToolbarGroup, ToolbarComponent>>();
        }

        private _idComponentMap: MyCoReMap<string, ToolbarComponent>;
        private _observerArray: Array<ContainerObserver<ToolbarGroup, ToolbarComponent>>;

        /**
         * Gets a IviewComponent from this Group.
         */
        public getComponentById(id: string): ToolbarComponent {
            return this._idComponentMap.get(id);
        }

        /**
         * Adds a IviewComponent to this Group and notify all registered observers.
         */
        public addComponent(component: ToolbarComponent) {
            var componentId = component.getProperty("id").value;
            if (this._idComponentMap.has(componentId)) {
                throw new Error(componentId + " already exist in " + this.name);
            }

            this._idComponentMap.set(componentId, component);
            this.notifyObserverChildAdded(this, component);
        }

        /**
         * Removes a IviewComponent from this Group and notify all registered observers.
         */
        public removeComponent(component: ToolbarComponent) {
            var componentId = component.getProperty("id").value;
            if (!this._idComponentMap.has(componentId)) {
                throw new Error(componentId + " doesnt exist in " + this.name);
            }

            this._idComponentMap.remove(componentId);
            this.notifyObserverChildRemoved(this, component);
        }

        /**
         * Gets all child ComponentIDs.
         */
        public getComponentIDs() {
            return this._idComponentMap.keys;
        }

        public getComponents() {
            return this._idComponentMap.values;
        }

        /**
        * Adds a Observer wich will be notified if a child was added or removed.
        */
        public addObserver(observer: ContainerObserver<ToolbarGroup, ToolbarComponent>) {
            this._observerArray.push(observer);
        }

        /**
         * Removes a previous added Observer.
         */
        public removeObserver(observer: ContainerObserver<ToolbarGroup, ToolbarComponent>) {
            var index = this._observerArray.indexOf(observer);
            this._observerArray.splice(index, 1);
        }

        /**
         * Gets all Registered Observers from this Group
         */
        public get observer() {
            return this._observerArray;
        }

        /**
         * Gets the name of this Group.
         */
        public get name() {
            return this._name;
        }

        public get align() {
            return (this._right) ? "right" : "left";
        }


        /**
        * [DO NOT CALL THIS] (Protected)
        */
        public notifyObserverChildAdded(group: ToolbarGroup, component: ToolbarComponent) {
            this._observerArray.forEach(function(elem) {
                elem.childAdded(group, component);
            });
        }

        /**
        * [DO NOT CALL THIS] (Protected)
        */
        public notifyObserverChildRemoved(group: ToolbarGroup, component: ToolbarComponent) {
            this._observerArray.forEach(function(elem) {
                elem.childRemoved(group, component);
            });
        }
    }
}
