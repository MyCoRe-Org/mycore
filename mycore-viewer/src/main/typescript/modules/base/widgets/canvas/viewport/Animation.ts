/// <reference path="../../../Utils.ts" />
/// <reference path="Viewport.ts" />

module mycore.viewer.widgets.canvas {

    export interface Animation {
        updateAnimation(elapsedTime:number):boolean;
    }

}

