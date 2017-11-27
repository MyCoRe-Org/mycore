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
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../../widgets/events/ViewerEvent.ts" />
/// <reference path="../model/StructureImage.ts" />

namespace mycore.viewer.components.events {
    export class ImageChangedEvent extends MyCoReImageViewerEvent{
        constructor(component: ViewerComponent,private _image:model.StructureImage) {
            super(component, ImageChangedEvent.TYPE);
        }

        public get image() {
            return this._image;
        }

        public static TYPE:string = "ImageChangedEvent";

    }
}
