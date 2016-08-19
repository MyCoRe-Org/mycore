/// <reference path="../../Utils.ts" />

module mycore.viewer.model {

    export interface AbstractPage {
        id:string;
        size: Size2D;
        draw(ctx:CanvasRenderingContext2D, rect:Rect, sourceScale, preview?:boolean): void;
        registerHTMLPage?(elem:HTMLElement);
        refreshCallback: () => void;
        clear(): void;
        toString(): string;
    }

}