/// <reference path="MouseSession.ts" />

module mycore.viewer.widgets.canvas {

    export interface DesktopInputEventHandler {
        mouseDown(session:MouseSession):void;
        mouseMove(session:MouseSession):void;
        mouseUp(session:MouseSession):void;
        scroll(e:{ deltaX:number; deltaY:number; orig:any
            ; pos:Position2D; altKey?:boolean});

        keydown(e:JQueryKeyEventObject):void;
        keypress(e:JQueryKeyEventObject):void;
        keyup(e:JQueryKeyEventObject):void;
    }

}