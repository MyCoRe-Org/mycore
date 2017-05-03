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