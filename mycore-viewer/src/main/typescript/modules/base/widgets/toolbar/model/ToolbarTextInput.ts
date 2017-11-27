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

    export class ToolbarTextInput extends ToolbarComponent {
        constructor(id: string, value:string, placeHolder: string) {
            super(id);
            this.addProperty(new ViewerProperty<string>(this, "value", value));
            this.addProperty(new ViewerProperty<string>(this, "placeHolder", placeHolder))
        }

        public get value(): string {
            return this.getProperty("value").value;
        }

        public set value(value: string) {
            this.getProperty("value").value = value;
        }

        public get placeHolder():string{
            return this.getProperty("placeHolder").value;
        }

        public set placeHolder(prefillText:string) {
            this.getProperty("placeHolder").value = prefillText;
        }

    }

}
