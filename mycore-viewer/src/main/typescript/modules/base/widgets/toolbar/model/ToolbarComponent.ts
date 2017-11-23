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

namespace mycore.viewer.widgets.toolbar {

    export class ToolbarComponent {
        constructor(id: string) {
            this._properties = new MyCoReMap<string, ViewerProperty<any>>();
            this.addProperty(new ViewerProperty(this, "id", id))
        }

        private _properties: MyCoReMap<string, ViewerProperty<any>>;

        public get id() {
            return this.getProperty("id").value;
        }

        public get PropertyNames() {
            return this._properties.keys;
        }

        public addProperty(property: ViewerProperty<any>) {
            this._properties.set(property.name, property);
        }

        public getProperty(name: string) {
            return this._properties.get(name);
        }

        public hasProperty(name: string) {
            return this._properties.has(name);
        }


    }

}
