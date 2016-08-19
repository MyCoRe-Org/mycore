/// <reference path="TouchSession.ts" />
/// <reference path="TouchMove.ts" />

module mycore.viewer.widgets.canvas {

    export interface TouchEventHandler {
        touchStart(session:TouchSession):void;
        touchMove(session:TouchSession):void;
        touchEnd(session:TouchSession):void;
    }

}