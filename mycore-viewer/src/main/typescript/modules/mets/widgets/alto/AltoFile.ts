/// <reference path="AltoElement.ts" />


module mycore.viewer.widgets.alto {
    export class AltoFile {
        get allElements():Array<mycore.viewer.widgets.alto.AltoElement> {
            return this._allElements;
        }

        private _rootChilds:Array<AltoElement> = new Array<AltoElement>();
        private _allElements:Array<AltoElement> = new Array<AltoElement>();
        private _allTextBlocks:Array<AltoElement> = new Array<AltoElement>();
        private _allIllustrations:Array<AltoElement> = new Array<AltoElement>();
        private _allLines:Array<AltoElement> = new Array<AltoElement>();
        private _allComposedBlock:Array<AltoElement> = new Array<AltoElement>();
        private _allGraphicalElements:Array<AltoElement> = new Array<AltoElement>();

        constructor(elem:Element){
            this._rootChilds = this.extractElements(elem);
            this._allElements = this._allTextBlocks.concat(this._allIllustrations).concat(this._allComposedBlock).concat(this._allLines).concat(this._allGraphicalElements);
        }

        //erstellt ein neues Alto-Element mit Mindestanforderungen
        private createAltoElement(src:Element, type:AltoElementType, parent:AltoElement):AltoElement {
            var width:number  = parseFloat(src.getAttribute("WIDTH"));
            var height:number = parseFloat(src.getAttribute("HEIGHT"));
            var hpos:number   = parseFloat(src.getAttribute("HPOS"));
            var vpos:number   = parseFloat(src.getAttribute("VPOS"));
            var id:string     = src.getAttribute("ID");
            return new AltoElement(parent, type, id, width, height, hpos, vpos);
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

