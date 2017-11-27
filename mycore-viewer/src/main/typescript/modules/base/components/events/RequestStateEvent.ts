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

namespace mycore.viewer.components.events {
    export class RequestStateEvent extends MyCoReImageViewerEvent {
        /**
         * ImageViewer States Versions:
         *
         *  Legacy : (not supported)
         *      receive/{objId}?page={pageHref}?derivate={derivateId}&zoom={zoomLevel}&x={vpX}&y={vpY}&tosize={mode}&maximized={maximized}&rotation={rotation}
         *
         *      zoomLevel = The tile zoom level (no scaling)
         *      vpX = The x coordinate of the left upper corner, but not scaled to the original resolution.
         *      vpY = The y coordinate of the left upper corner, but not scaled to the original resolution.
         *      mode = The display mode. none = normal ,screen = fit to screen, width = fit to width,
         *      maximized = display metadata page or the maximized viewer
         *      rotation = rotation of the image in deg (0|90|180|270)
         *
         * v0.2 :
         *      /rsc/iview/client/{derivate}/{file}/x1={rect.x}&x2={rect.x+rect.width}&y1={rect.y}&y2={rect.y+rect.height}&rotation={rotation}&layout={layoutId}[&page={pdfPageNumber}]
         *      rect = The rectangle representing the viewport
         *      layoutId = singlePageLayout |  doublePageLayout | doublePageRelocatedLayout
         *
         *
         */ 
        constructor(component:ViewerComponent, public stateMap:ViewerParameterMap, public deepState:boolean = true) {
            super(component, RequestStateEvent.TYPE);
        }

        public static TYPE:string = "RequestStateEvent";

    }
}
