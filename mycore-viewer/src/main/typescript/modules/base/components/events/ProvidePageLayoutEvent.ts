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

/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../../widgets/canvas/PageLayout.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    /**
     * Used to provide a PageLayout wich can be used by the MyCoReImageScrollComponent.
     */
    export class ProvidePageLayoutEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public pageLayout:widgets.canvas.PageLayout, public isDefault=false) {
            super(component, ProvidePageLayoutEvent.TYPE);
        }

        public static TYPE = "ProvidePageLayoutEvent";

    }
}
