/// <reference path="../../widgets/events/ViewerEvent.ts" />

namespace mycore.viewer.components.events {
    export class MyCoReImageViewerEvent extends mycore.viewer.widgets.events.DefaultViewerEvent {

        constructor(public component: any, type: string) {
            super(type);
        }


    }

}