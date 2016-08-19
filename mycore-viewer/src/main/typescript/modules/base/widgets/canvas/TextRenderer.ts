/// <reference path="../../components/model/TextContent.ts" />
module mycore.viewer.widgets.canvas {
    export class TextRenderer {
        constructor(private _vp:Viewport, private _area:PageArea, private _view:PageView, private _textContentProvider:(page:model.AbstractPage, contentProvider:(textContent:model.TextContentModel)=>void)=>void) {
            this.textContainer = document.createElement("div");
            this.textContainer.style.cssText = "line-height: 1;" +
                "white-space: pre;" +
                "font-family: sans-serif";


            this.textContainer.setAttribute("class", "textContainer");
            var htmlElement = <HTMLElement>this._view.container[0];
            htmlElement.appendChild(this.textContainer);
        }

        private _contentCache:MyCoReMap<model.AbstractPage, model.TextContentModel> = new MyCoReMap<model.AbstractPage, model.TextContentModel>();
        private _callbackRunning:MyCoReMap<model.AbstractPage, boolean> = new MyCoReMap<model.AbstractPage, boolean>();
        private _addedPages:Array<model.AbstractPage> = new Array<model.AbstractPage>();
        public textContainer:HTMLElement;
        private _elementCache:MyCoReMap<String, HTMLElement> = new MyCoReMap<String, HTMLElement>();
        private _pageElementCache:MyCoReMap<model.AbstractPage, HTMLElement> = new MyCoReMap<model.AbstractPage, HTMLElement>();
        private _lineElementMap:MyCoReMap<model.TextElement, HTMLElement> = new MyCoReMap<model.TextElement, HTMLElement>()
        private _highlightedLines:MyCoReMap<string, model.TextElement> = null;
        private _highlightWordMap:MyCoReMap<model.TextElement, Array<string>> = null;
        private _idPageMap = new MyCoReMap<string, model.AbstractPage>();
        private _mesureCanvas = <CanvasRenderingContext2D>document.createElement("canvas").getContext("2d");
        public update() {
            var pagesInViewport = this._area.getPagesInViewport(this._vp);
            pagesInViewport.forEach((page:model.AbstractPage) => {
                if (!this._idPageMap.has(page.id)) {
                    this._idPageMap.set(page.id, page);
                }

                if (this._addedPages.indexOf(page) == -1) {
                    this.addPage(page);
                }

                if (!this._contentCache.has(page) && !this._callbackRunning.has(page)) {
                    var promise = this._textContentProvider(page, (content)=> {
                        this._contentCache.set(page, content);
                        this.addPageParts(page, content);
                    });
                    this._callbackRunning.set(page, true);
                } else if (this._contentCache.has(page)) {
                    this.updatePage(page);
                }
            });

            this._addedPages.forEach((p)=> {
                if (pagesInViewport.indexOf(p) == -1) {
                    this.removePage(p);
                }
            });
        }

        public highlightText(text:Array<model.TextElement>, wordMap:MyCoReMap<model.TextElement, Array<string>>) {

            var it = new MyCoReMap<model.TextElement, boolean>();
            var filteredText = text.filter((item, pos)=> {
                if (it.has(item)) {
                    return false;
                } else {
                    it.set(item, true);
                }
                return true;
            });

            var highlightedLines = this._highlightedLines;
            this._highlightedLines = new MyCoReMap<string, model.TextElement>();
            if (highlightedLines != null) {
                highlightedLines.forEach((k, v) => {
                    if (this._lineElementMap.has(v)) {
                        var parent = this._lineElementMap.get(v).parentElement;
                        this.removeContentPart(v);
                        var newContentPart = this.createContentPart(this._idPageMap.get(v.pageHref), v);
                        parent.appendChild(newContentPart);
                    }
                });
            }

            this._highlightWordMap = wordMap;

            this._highlightedLines = new MyCoReMap<string, model.TextElement>();
            filteredText.forEach((textLine) => {
                this._highlightedLines.set(textLine.toString(), textLine);
                var hasParentPage = this._idPageMap.get(textLine.pageHref);
                if (hasParentPage) {
                    if (this._pageElementCache.has(this._idPageMap.get(textLine.pageHref)) && this._lineElementMap.has(textLine)) {
                        var elem = this._lineElementMap.get(textLine);
                        elem.setAttribute("class", "line highlighted");
                        this.highlightWords(textLine, elem);
                    }
                }
            });


        }

        private updatePage(page:model.AbstractPage) {
            var pai = this._area.getPageInformation(page);
            var size = page.size.scale(pai.scale);
            var halfSize = size.scale(0.5);
            var pageRect = new Rect(new Position2D(pai.position.x - halfSize.width, pai.position.y - halfSize.height), size);
            var vpRect = this._vp.asRectInArea();
            var dpr = (window.devicePixelRatio||1);

            var pagePos = new Position2D(pageRect.pos.x - vpRect.pos.x, pageRect.pos.y - vpRect.pos.y);
            var scaledPagePos = pagePos.scale(this._vp.scale / dpr);
            var pe = this._pageElementCache.get(page);

            var realSize = size.scale(1 / pai.scale);

            pe.style.cssText =
                "transform-origin : 0% 0%;" +
                "position : absolute;" +
                "left : " + scaledPagePos.x + "px;" +
                "top : " + scaledPagePos.y + "px;" +
                "width : " + realSize.width + "px;" +
                "height : " + realSize.height + "px;" +
                "transform : " + "scale(" + (pai.scale * this._vp.scale / dpr) + ");" +
                "z-index: 5;";

            var childrenElement = <HTMLElement>pe.children[0];
            childrenElement.style.cssText = "transform : rotate(" + pai.rotation + "deg);" +
                "width: " + realSize.width + "px;" +
                "height: " + realSize.height + "px;"


        }

        private addPage(page:model.AbstractPage) {
            if (!this._pageElementCache.has(page)) {
                this.createPageElement(page);
            }

            var pageElement = this._pageElementCache.get(page);
            this._addedPages.push(page);
            this.textContainer.appendChild(pageElement);
        }

        createPageElement(page) {
            var pageElement = document.createElement("div");
            var childPageElement = <HTMLDivElement>(pageElement.cloneNode());
            pageElement.appendChild(childPageElement);
            this._pageElementCache.set(page, pageElement);
            pageElement.style.display = "none";
        }

        private removePage(page:model.AbstractPage) {
            this._addedPages.splice(this._addedPages.indexOf(page), 1);
            var textContent = this._pageElementCache.get(page);
            textContent.parentElement.removeChild(textContent);
        }

        private addPageParts(page:model.AbstractPage, textContent:model.TextContentModel) {
            var pageHtml = document.createDocumentFragment();
            textContent.content.forEach((e)=> {
                var cacheKey = e.pos.toString() + e.text;
                if (!this._elementCache.has(cacheKey)) {
                    var contentPart = this.createContentPart(page, e);
                    pageHtml.appendChild(contentPart);
                }
            });
            var pageElement = <HTMLElement>this._pageElementCache.get(page).children[0];
            pageElement.appendChild(pageHtml);
            if (pageElement.style.display == "none") {
                pageElement.style.display == "block";
            }
        }

        private removeContentPart(cp:model.TextElement) {
            var cpElement = this._lineElementMap.get(cp);
            var parent = cpElement.parentElement;

            parent.removeChild(cpElement);
            this._lineElementMap.remove(cp);
        }

        private createContentPart(page:model.AbstractPage, cp:model.TextElement):HTMLElement {
            var htmlElement = window.document.createElement("div");
            htmlElement.textContent = cp.text;
            htmlElement.setAttribute("class", "line");

            // need to stop propagation, because the text should be selectable and one parent calls preventDefault
            // (which prevents the selection)
            var stopPropagation = (e)=>{
                e.stopPropagation();
            };

            htmlElement.addEventListener("mousedown", stopPropagation);
            htmlElement.addEventListener("mouseup", stopPropagation);
            htmlElement.addEventListener("mousemove", stopPropagation);

            htmlElement.addEventListener("mouseenter", ()=> {
                if ("mouseenter" in cp && typeof cp.mouseenter == "function") {
                    cp.mouseenter();
                }
            });

            htmlElement.addEventListener("mouseleave", ()=> {
                if ("mouseleave" in cp && typeof cp.mouseleave == "function") {
                    cp.mouseleave();
                }
            });



            var size = page.size;
            var cacheKey = cp.pos.toString() + cp.text;

            this._elementCache.set(cacheKey, htmlElement);

            this._mesureCanvas.save();
            this._mesureCanvas.font = cp.fontSize + 'px ' + cp.fontFamily;

            var drawnWidth = this._mesureCanvas.measureText(cp.text).width;
            this._mesureCanvas.restore();

            var xScaling = cp.size.width / drawnWidth;
            this._lineElementMap.set(cp, htmlElement);

            if (this._highlightedLines != null && this._highlightedLines.has(cp.toString())) {
                htmlElement.setAttribute("class", "line highlighted");
                this.highlightWords(cp, htmlElement);
            }


            var topPosition = ("fromBottomLeft" in cp && !cp.fromBottomLeft) ? cp.pos.y : size.height - cp.pos.y;

            htmlElement.style.cssText = "left : " + cp.pos.x + "px;" +
                "top : " + topPosition + "px;" +
                "font-family : " + cp.fontFamily + ";" +
                "font-size : " + cp.fontSize + "px;" +
                "transform-origin : 0% 0%;" +
                "transform : scalex(" + xScaling + ");";

            return htmlElement;
        }

        private highlightWords(cp, elem) {
            if (this._highlightWordMap.has(cp)) {
                var words = this._highlightWordMap.get(cp);

                words.forEach((w)=> {
                    var elemHtml = elem.innerHTML;
                    elemHtml = elemHtml.replace(new RegExp(w, "gim"), "<span class='highlighted'>$&</span>")
                    elem.innerHTML = elemHtml;
                });
            }
        }


    }
}