/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../../widgets/canvas/input/TouchInputListener.ts" />

namespace mycore.viewer.components.events {
    export class RequestTouchInputEvent extends MyCoReImageViewerEvent {
        constructor(component:ViewerComponent, public listener:widgets.canvas.TouchInputListener) {
            super(component, RequestTouchInputEvent.TYPE);
        }

        public static TYPE:string = "RequestTouchInputEvent";
    }
}
