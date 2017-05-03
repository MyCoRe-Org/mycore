/// <reference path="../../Utils.ts" />

namespace mycore.viewer.model {
    export interface TextContentModel {
        content : Array<TextElement>;
    }

    export interface TextElement {
        fromBottomLeft?:boolean;
        angle?: number;
        size: Size2D;
        text: string;
        pos: Position2D;
        fontFamily?: string;
        fontSize?: number;
        pageHref: string;
        mouseenter?: ()=>void;
        mouseleave?: ()=>void;
        toString():string;
    }
}