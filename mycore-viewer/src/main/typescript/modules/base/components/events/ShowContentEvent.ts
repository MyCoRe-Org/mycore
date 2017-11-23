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

/// <reference path="../../definitions/jquery.d.ts" />
/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    export class ShowContentEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public content:JQuery, public containerDirection:number, public size = 300, public text:JQuery = null) {
            super(component, ShowContentEvent.TYPE);
        }

        public static DIRECTION_CENTER = 0;
        public static DIRECTION_EAST = 1;
        public static DIRECTION_SOUTH = 2;
        public static DIRECTION_WEST = 3;
        public static DIRECTION_NORTH = 4;

        public static TYPE = "ShowContentEvent";
    }
}
