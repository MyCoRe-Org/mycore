/// <reference path="../../Utils.ts" />

namespace mycore.viewer.model {

    export interface AbstractPage {
        id:string;
        size: Size2D;
        draw(ctx:CanvasRenderingContext2D, rect:Rect, sourceScale, preview?:boolean, infoScale?:number): void;
        registerHTMLPage?(elem:HTMLElement);
        refreshCallback: () => void;
        clear(): void;
        toString(): string;
    }

}
