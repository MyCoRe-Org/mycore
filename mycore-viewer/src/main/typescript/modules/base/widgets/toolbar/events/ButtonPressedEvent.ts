/// <reference path="../../events/ViewerEvent.ts" />
/// <reference path="../model/ToolbarButton.ts" />

namespace mycore.viewer.widgets.toolbar.events {
    export class ButtonPressedEvent extends mycore.viewer.widgets.events.DefaultViewerEvent {
        constructor(private _button:ToolbarButton, type = ButtonPressedEvent.TYPE) {
            super(type);
        }

        public static TYPE:string = "ButtonPressedEvent";

        public get button() {
            return this._button;
        }
    }

}