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

    export class ToolbarButton extends ToolbarComponent {
        constructor(id: string, label: string, tooltip: string = label, icon: string= null, buttonClass: string = "default", disabled: boolean = false, active: boolean = false) {
            super(id);
            this.addProperty(new ViewerProperty<string>(this, "label", label));
            this.addProperty(new ViewerProperty<string>(this, "tooltip", tooltip));
            this.addProperty(new ViewerProperty<string>(this, "icon", icon));
            this.addProperty(new ViewerProperty<string>(this, "buttonClass", buttonClass));
            this.addProperty(new ViewerProperty<boolean>(this, "disabled", false));
            this.addProperty(new ViewerProperty<boolean>(this, "active", false));
        }

        public get label(): string {
            return this.getProperty("label").value;
        }

        public set label(label: string) {
            this.getProperty("label").value = label;
        }

        public get tooltip(): string {
            return this.getProperty("tooltip").value;
        }

        public set tooltip(tooltip: string) {
            this.getProperty("tooltip").value = tooltip;
        }

        public get icon(): string {
            return this.getProperty("icon").value;
        }

        public set icon(icon: string) {
            this.getProperty("icon").value = icon;
        }

        public get buttonClass(): string {
            return this.getProperty("buttonClass").value;
        }

        public set buttonClass(buttonClass: string) {
            this.getProperty("buttonClass").value = buttonClass;
        }

        public get disabled(): boolean {
            return this.getProperty("disabled").value;
        }

        public set disabled(disabled: boolean) {
            this.getProperty("disabled").value = disabled;
        }

        public get active(): boolean {
            return this.getProperty("active").value;
        }

        public set active(active: boolean) {
            this.getProperty("active").value = active;
        }
    }
}
