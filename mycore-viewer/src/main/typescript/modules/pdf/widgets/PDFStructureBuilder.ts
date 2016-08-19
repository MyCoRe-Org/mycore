/// <reference path="../definitions/pdf.d.ts" />
/// <reference path="PDFStructureModel.ts" />

module mycore.viewer.widgets.pdf {
    export class PDFStructureBuilder {

        constructor(private _document:PDFDocumentProxy, private _name:string) {
            this._pageCount = <any>(this._document.numPages);
        }

        private _structureModel:PDFStructureModel;
        private _pageChapterMap:MyCoReMap<string, model.StructureChapter> = new MyCoReMap<String, mycore.viewer.model.StructureChapter>();
        private _chapterPageMap:MyCoReMap<string, model.StructureImage> = new MyCoReMap<String, mycore.viewer.model.StructureImage>();
        private _pages:Array<model.StructureImage> = new Array<model.StructureImage>();
        private _pageCount:number = 0;
        private _destinations:Array<any>;
        private _refPageMap:MyCoReMap<string, PDFPageProxy> = new MyCoReMap<String, PDFPageProxy>();
        private _idPdfPageMap:MyCoReMap<string, PDFPageProxy> = new MyCoReMap<String, PDFPageProxy>();
        private _idPageMap:MyCoReMap<number, model.StructureImage> = new MyCoReMap<Number, mycore.viewer.model.StructureImage>();
        private _loadedPageCount:number;
        private _outline:Array<PDFTreeNode>;
        private _rootChapter:model.StructureChapter;
        private _promise:ViewerPromise<PDFStructureModel, any> = new ViewerPromise<PDFStructureModel, any>();
        private static PDF_TEXT_HREF = "pdfText";

        public resolve() {
            this._resolveDestinations();
            this._resolvePages();
            this._resolveOutline();
            return <GivenViewerPromise<PDFStructureModel, any>>this._promise;
        }

        private _resolvePages() {
            var that = this;
            this._loadedPageCount = 0;

            for (var i = 1; i <= that._pageCount; i++) {
                var callback = this._createThumbnailDrawer(i);
                var additionalHref = new MyCoReMap<string,string>();
                additionalHref.set(PDFStructureBuilder.PDF_TEXT_HREF, i + "");
                var img = new model.StructureImage("pdfPage", i + "", i, null, i + "", "pdfPage", callback, additionalHref);
                that._pages.push(img);
                that._idPageMap.set(i, img);
                var promise = that._document.getPage(i);
                promise.then(this._createPageLoadCallback(i));
            }
        }

        private _createThumbnailDrawer(i) {
            var that = this;
            var imgData = null;
            var collectedCallbacks = new Array<(string)=>void>();
            return (callback:(string)=>void)=> {
                if (imgData == null) {
                    if (collectedCallbacks.length == 0) {
                        collectedCallbacks.push((imgDataToSet)=> {
                            imgData = imgDataToSet;
                        });
                        that._renderPage(collectedCallbacks, that._idPdfPageMap.get(i));
                    }
                    collectedCallbacks.push(callback);
                } else {
                    callback(imgData);
                }
            }
        }

        private _createPageLoadCallback(i) {
            var that = this;
            return function (page:PDFPageProxy) {
                try {
                    var ref:PDFRef = <any>page.ref;
                    var strRef = PDFStructureBuilder.destToString(ref);
                    that._refPageMap.set(strRef, page);
                    that._idPdfPageMap.set(i + "", page);
                    var sImage = that._idPageMap.get(i);

                    // if thumbnail panel or Imagebar need preview page

                    that._loadedPageCount++;
                    that.resolveStructure();
                } catch (e) {
                    console.log(e);
                    that._promise.reject(e);
                }
            }
        }

        private _renderPage(callbacks:Array<(string)=>void>, page) {
            var originalSize =  new Size2D(page.view[2] - page.view[0], page.view[3] - page.view[1]);//IviewPDFCanvas.getPageSize(page);
            var largest = Math.max(originalSize.width, originalSize.height);
            var vpScale = 256 / largest;
            var vp = page.getViewport(vpScale);
            var thumbnailDrawCanvas = document.createElement("canvas");
            var thumbnailCanvasCtx = thumbnailDrawCanvas.getContext("2d");
            thumbnailDrawCanvas.width = (originalSize.width) * vpScale;
            thumbnailDrawCanvas.height = (originalSize.height) * vpScale;

            var task = <any> page.render({canvasContext: thumbnailCanvasCtx, viewport: vp})
            var that = this;
            task.internalRenderTask.callback = function okay(err) {
                that._loadedPageCount++;
                var imgUrl = thumbnailDrawCanvas.toDataURL();
                thumbnailDrawCanvas = null;
                thumbnailCanvasCtx = null;
                for (var callbackIndex in callbacks) {
                    var callback = callbacks[callbackIndex];
                    callback(imgUrl);
                }
            };

        }


        private _resolveOutline() {
            var that = this;
            this._document.getOutline().then(function (nodes:Array<PDFTreeNode>) {
                that._outline = nodes;
                that.resolveStructure();
            });
        }

        private _resolveDestinations() {
            var that = this;
            this._document.getDestinations().then(function (dest:Array<any>) {
                that._destinations = dest;
                that.resolveStructure()
            });
        }

        private getChapterFromOutline(parent:model.StructureChapter, nodes:Array<PDFTreeNode>):Array<model.StructureChapter> {
            var o = 0;
            var chapterArr = new Array<model.StructureChapter>();
            for (var nodeIndex in nodes) {
                var currentNode = nodes[nodeIndex];
                var chapter = new model.StructureChapter(parent, "pdfChapter", Utils.hash(currentNode.title).toString(), ++o, currentNode.title, null);
                var children = this.getChapterFromOutline(chapter, currentNode.items);
                chapter.chapter = children;
                chapterArr.push(chapter);
                var dest:PDFRef = <any>this.getDestOfNode(currentNode);
                if(dest !== null && this._refPageMap.has(<any>dest)) {
                    var pageDest = this._refPageMap.get(<any>dest);
                    this._chapterPageMap.set(chapter.id, this._idPageMap.get(<any>pageDest.pageNumber));
                } else {
                    console.log("Could not find dest for " + chapter.id);
                }
            }

            return chapterArr;
        }

        /**
         * Checks if all needed data is resolved and the structure model can be build.
         * Executes the Callback.
         */
        private resolveStructure() {
            if (this._loadedPageCount == this._pageCount && typeof this._outline != "undefined" && typeof this._destinations != "undefined") {
                var that = this;
                this._rootChapter = new model.StructureChapter(null, "pdf", "0", 0, this._name, null);
                this._rootChapter.chapter = this.getChapterFromOutline(this._rootChapter, this._outline);
                this._chapterPageMap.set(this._rootChapter.id, this._idPageMap.get(1));
                this._structureModel = new PDFStructureModel(this._rootChapter, this._pages, this._chapterPageMap, new MyCoReMap<string, mycore.viewer.model.StructureChapter>(), this._refPageMap, this._idPdfPageMap);
                this._promise.resolve(this._structureModel);
            }
        }

        /**
         * Resolves the destination of a node. A destination can be a
         * @param node
         * @returns {string}
         */
        private getDestOfNode(node:PDFTreeNode):string {
            if(node == null ) {
                return null;
            }

            if(node.dest == null) {
                return null;
            }

            var chapterDestination = (typeof node.dest == "object") ? node.dest : this._destinations[node.dest];
            if (chapterDestination[ 0 ] == null) {
                return null;
            }

            return PDFStructureBuilder.destToString(chapterDestination[0]);
        }

        /**
         * Converts a destination to a String.
         * @param ref the PDFRef wich should be converted.
         * @returns {string}
         */
        private static destToString(ref:PDFRef):string {
            return   ref.gen + " " + ref.num;

        }

    }
}