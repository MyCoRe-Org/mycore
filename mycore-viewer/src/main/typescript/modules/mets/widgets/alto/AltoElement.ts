/// <reference path="AltoStyle.ts" />

enum AltoElementType {ComposedBlock, Illustration, GraphicalElement,TextBlock, TextLine, String, SP, HYP}

namespace mycore.viewer.widgets.alto {
    export class AltoElement {
        //diese Klasse stellt ein Grundelement von ALTO dar mit den Minimalanforderungen

        //Attribute der Elemente (die hier genannten MÜSSEN vorliegen)
        private _children:Array<AltoElement> = new Array<AltoElement>();
        private _content:string = null;
        private _style:AltoStyle = null;

        constructor(private _parent:AltoElement,
                    private _type: AltoElementType,
                    private _id: string,
                    private _width: number,
                    private _height: number,
                    private _hpos: number,
                    private _vpos: number
        ){
        }

        public getHeight(): number {
            return this._height;
        }

        public getHPos(): number {
            return this._hpos;
        }

        public getId(): string {
            return this._id;
        }

        public getType(): AltoElementType {
            return this._type;
        }

        public getVPos(): number {
            return this._vpos;
        }

        public getWidth(): number {
            return this._width;
        }

        public getChildren(): Array<AltoElement> {
            return this._children;
        }

        public setChildren(childs: Array<AltoElement>): void {
            this._children = childs;
        }

        public getContent(): string {
            return this._content;
        }

        public setContent(content: string): void {
            this._content = content;
        }

        public getStyle(): AltoStyle {
            return this._style;
        }

        public setAltoStyle( style: AltoStyle ): void {
            this._style = style;
        }

        public getParent(): AltoElement {
            return this._parent;
        }
        
        public getBlockHPos():number {

            return this._hpos;
        }

        public getBlockVPos():number {
      
            return this._vpos;
        }
    }
}