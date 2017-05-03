/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../../widgets/canvas/input/DesktopInputListener.ts" />

namespace mycore.viewer.components.events {
    export class RequestDesktopInputEvent extends MyCoReImageViewerEvent {
        constructor(component:ViewerComponent, public listener:widgets.canvas.DesktopInputListener) {
            super(component, RequestDesktopInputEvent.TYPE);
        }

        public static TYPE:string = "RequestDesktopInputEvent";
    }
}
