/// <reference path="../../Utils.ts" />

namespace mycore.viewer.widgets.canvas {
    export interface CanvasPageLayer {

        draw(ctx:CanvasRenderingContext2D, id:string, pageSize:Size2D, drawOnHtml:boolean):void;

    }

}
