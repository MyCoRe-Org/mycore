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

/// <reference path="../widgets/canvas/DoublePageLayout.ts" />
/// <reference path="../widgets/canvas/DoublePageRelocatedLayout.ts" />

namespace mycore.viewer.components {

    export class MyCoRePageDesktopLayoutProviderComponent extends ViewerComponent {

        constructor(private _settings: MyCoReViewerSettings) {
            super();
        }

        public get handlesEvents(): string[] {
            return [];
        }

        public init() {
            const singlePageLayout = new widgets.canvas.SinglePageLayout();
            const doublePageLayout = new widgets.canvas.DoublePageLayout();
            const doublePageRelocatedLayout = new widgets.canvas.DoublePageRelocatedLayout();

            singlePageLayout.maximalPageScale = doublePageLayout.maximalPageScale =
                doublePageRelocatedLayout.maximalPageScale = parseInt(this._settings.maximalPageScale, 10) || 4;

            this.trigger(new events.ProvidePageLayoutEvent(this, singlePageLayout, true));
            this.trigger(new events.ProvidePageLayoutEvent(this, doublePageLayout));
            this.trigger(new events.ProvidePageLayoutEvent(this, doublePageRelocatedLayout));
        }
    }

}

addViewerComponent(mycore.viewer.components.MyCoRePageDesktopLayoutProviderComponent);
