/// <reference path="ButtonPressedEvent.ts" />
/// <reference path="../model/ToolbarDropdownButton.ts" />

module mycore.viewer.widgets.toolbar.events {
    export class DropdownButtonPressedEvent extends ButtonPressedEvent {
        constructor(button:ToolbarDropdownButton, private _childId:string) {
            super(button, DropdownButtonPressedEvent.TYPE);
        }

        public get childId() {
            return this._childId;
        }

        public static TYPE:string = "DropdownButtonPressedEvent";


    }

}