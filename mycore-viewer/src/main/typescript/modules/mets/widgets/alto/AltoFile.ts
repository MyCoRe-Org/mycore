/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

/// <reference path="AltoStyle.ts" />
/// <reference path="AltoElement.ts" />


namespace mycore.viewer.widgets.alto {
    export class AltoFile {
        get allElements():Array<mycore.viewer.widgets.alto.AltoElement> {
            return this._allElements;
        }

        private _allStyles:{[id: string]: AltoStyle} = {};

        private _rootChilds:Array<AltoElement> = new Array<AltoElement>();
        private _allElements:Array<AltoElement> = new Array<AltoElement>();
        private _allTextBlocks:Array<AltoElement> = new Array<AltoElement>();
        private _allIllustrations:Array<AltoElement> = new Array<AltoElement>();
        private _allLines:Array<AltoElement> = new Array<AltoElement>();
        private _allComposedBlock:Array<AltoElement> = new Array<AltoElement>();
        private _allGraphicalElements:Array<AltoElement> = new Array<AltoElement>();

        private _pageWidth:number = -1;
        private _pageHeight:number = -1;

        constructor(styles:Element, layout:Element) {
            // set style
            var styleList:NodeListOf<Element> = styles.getElementsByTagName("TextStyle");
            for(var index = 0; index < styleList.length; index++) {
                var style:Element = styleList.item(index);
                var altoStyle:AltoStyle = this.createAltoStyle(style);
                this._allStyles[altoStyle.getId()] = altoStyle;
            }

            // set width/height
            var pages:NodeListOf<Element> = layout.getElementsByTagName("Page");
            var page:Element = pages.item(0);
            if(page == null) {
                return;
            }
            this._pageWidth = parseInt(page.getAttribute("WIDTH"));
            this._pageHeight = parseInt(page.getAttribute("HEIGHT"));

            // extract content
            var printSpaces:NodeListOf<Element> = page.getElementsByTagName("PrintSpace");
            var printSpace:Element = printSpaces.item(0);
            if(printSpace == null) {
                return;
            }
            this._rootChilds = this.extractElements(printSpace);
            this._allElements = this._allTextBlocks.concat(this._allIllustrations).concat(this._allComposedBlock).concat(this._allLines).concat(this._allGraphicalElements);
        }

        private createAltoStyle(src:Element):AltoStyle {
            var id:string         = src.getAttribute("ID");
            var fontFamily:string = src.getAttribute("FONTFAMILY");
            var fontSize:number   = parseFloat(src.getAttribute("FONTSIZE"));
            var fontStyle:string  = src.getAttribute("FONTSTYLE");
            return new AltoStyle(id, fontFamily, fontSize, fontStyle);
        }

        //erstellt ein neues Alto-Element mit Mindestanforderungen
        private createAltoElement(src:Element, type:AltoElementType, parent:AltoElement):AltoElement {
            let width:number  = parseFloat(src.getAttribute("WIDTH"));
            let height:number = parseFloat(src.getAttribute("HEIGHT"));
            let hpos:number   = parseFloat(src.getAttribute("HPOS"));
            let vpos:number   = parseFloat(src.getAttribute("VPOS"));
            let wc:number   = parseFloat(src.getAttribute("WC"));
            let id:string     = src.getAttribute("ID");
            let styleID:string  = src.getAttribute("STYLEREFS");
            let altoElement:AltoElement = new AltoElement(parent, type, id, width, height, hpos, vpos, wc);
            if(styleID != null) {
                let style:AltoStyle = this._allStyles[styleID];
                if(style != null) {
                    altoElement.setAltoStyle(style);
                }
            }
            return altoElement;
        }

        //Aufruf: PrintSpace->ermittle alle Kinder des Typs Textblock -> ermittle davon alle Kinder (TextLines) -> hole die Kinder der Textlines
        private extractElements(elem:Element, parent:AltoElement = null):Array<AltoElement> {
            var childrenOfElement:Array<AltoElement> = new Array<AltoElement>();
            //da enums mit reverse mapping erstellt werden kann zu der passende Nummer der entsprechende String ausgegeben werden
            var childList:NodeList = elem.childNodes;

            //durchlaufe die Kinder
            for(var index = 0; index < childList.length; index++) {
                var currentElement = <Element>childList.item(index);
                if (currentElement instanceof Element) {
                    var elementType = this.detectElementType(currentElement);
                    if(elementType!=null){
                        var currentAltoElement = this.createAltoElement(currentElement, elementType, parent);

                        switch(elementType) {
                            case AltoElementType.ComposedBlock:
                            case AltoElementType.TextBlock:
                                var blockChildren = this.extractElements(currentElement, currentAltoElement);
                                currentAltoElement.setChildren(blockChildren);
                                childrenOfElement.push(currentAltoElement);
                                if(elementType==AltoElementType.TextBlock){
                                    this._allTextBlocks.push(currentAltoElement);
                                }
                                if(elementType==AltoElementType.ComposedBlock){
                                    this._allComposedBlock.push(currentAltoElement);
                                }

                                break;
                            case AltoElementType.Illustration:
                            case AltoElementType.GraphicalElement:
                                if(elementType==AltoElementType.Illustration){
                                    this._allIllustrations.push(currentAltoElement);
                                }
                                if(elementType==AltoElementType.GraphicalElement){
                                    this._allGraphicalElements.push(currentAltoElement);
                                }
                                break;
                            case AltoElementType.TextLine:
                                var listChildrens = this.extractElements(currentElement,  currentAltoElement);
                                currentAltoElement.setChildren(listChildrens);
                                childrenOfElement.push(currentAltoElement);
                                this._allLines.push(currentAltoElement);
                                break;
                            case AltoElementType.String:
                            case AltoElementType.SP:
                            case AltoElementType.HYP:
                                currentAltoElement.setContent(currentElement.getAttribute("CONTENT"));
                                childrenOfElement.push(currentAltoElement);
                                break;
                        }
                    }
                }
            }
            return childrenOfElement;
        }

        public getBlocks():Array<AltoElement> {
            return this._allTextBlocks;
        }
        
        public getBlockContent(id:string): string {
            var content:string = "";
            for (var index = 0; index < this._allTextBlocks.length; index++) {
                if (this._allTextBlocks[ index ].getId() == id) {
                    var lines:Array<AltoElement> = this._allTextBlocks[ index ].getChildren();
                    for(var i = 0; i < lines.length; i++) {
                        content += this.getContainerContent(lines[ i ].getId(), this._allLines);
                    }
                    break;
                }
            }
            return content;
        }

        public getContainerContent(id:string, container:Array<AltoElement>):string {
            var content: string = "";

            for(var index = 0; index < container.length; index++) {
                if(container[index].getId() == id) {
                    var elements:Array<AltoElement> = container[ index ].getChildren();
                    for(var i = 0; i < elements.length; i++) {
                        var childs:Array<AltoElement> = elements[ i ].getChildren();
                        content += "<span data-id=" + elements[i].getId() + ">";
                        for(var j = 0; j < childs.length; j++) {
                            content += childs[j].getContent() + " ";
                        }
                        content += "</span>";
                    }
                    content.trim();
                    break;
                }
            }
            return content;
        }

        public getLines():Array<AltoElement> {
            return this._allLines;
        }

        public get pageWidth():number {
            return this._pageWidth;
        }

        public get pageHeight():number {
            return this._pageHeight;
        }

        private detectElementType(currentElement:Element):AltoElementType {
            if(currentElement.nodeName.toLowerCase() == "string"){
                return AltoElementType.String;
            }
            if(currentElement.nodeName.toLowerCase() == "sp"){
                return AltoElementType.SP;
            }
            if(currentElement.nodeName.toLowerCase() == "hyp"){
                return AltoElementType.HYP;
            }
            if(currentElement.nodeName.toLowerCase() == "textline"){
                return AltoElementType.TextLine;
            }
            if(currentElement.nodeName.toLowerCase() == "textblock"){
                return AltoElementType.TextBlock;
            }
            if(currentElement.nodeName.toLowerCase() == "composedblock"){
                return AltoElementType.ComposedBlock;
            }
            if(currentElement.nodeName.toLowerCase() == "illustration"){
                return AltoElementType.Illustration;
            }
            if(currentElement.nodeName.toLowerCase() == "graphicalelement"){
                return AltoElementType.GraphicalElement;
            }

            return null;
        }
    }
}

