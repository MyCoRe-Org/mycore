/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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


import {GivenViewerPromise, MyCoReMap, Size2D, Utils, ViewerPromise} from "../../base/Utils";
import {StructureImage} from "../../base/components/model/StructureImage";
import {StructureChapter} from "../../base/components/model/StructureChapter";
import {PDFStructureModel} from "./PDFStructureModel";
import {PDFDocumentProxy, PDFPageProxy} from "pdfjs-dist/legacy/build/pdf.min.mjs";
import type {RefProxy} from "pdfjs-dist/types/src/display/api";

export class PDFStructureBuilder {
    private _startPage: number = -1;

    constructor(private _document: PDFDocumentProxy, private _name: string) {
        this._pageCount = <any>(this._document.numPages);
    }

    private _structureModel: PDFStructureModel = null;
    private _chapterPageMap: MyCoReMap<string, StructureImage> = new MyCoReMap<string, StructureImage>();
    private _pages: Array<StructureImage> = new Array<StructureImage>();
    private _pageCount: number = 0;
    private _refPageMap: MyCoReMap<string, PDFPageProxy> = new MyCoReMap<string, PDFPageProxy>();
    private _idPageMap: MyCoReMap<number, StructureImage> = new MyCoReMap<number, StructureImage>();
    private _loadedPageCount: number;
    private _outline: Array<PDFTreeNode>;
    private _rootChapter: StructureChapter;
    private _promise: ViewerPromise<PDFStructureModel, any> = new ViewerPromise<PDFStructureModel, any>();
    private _outlineTodoCount = 0;
    private static PDF_TEXT_HREF = "pdfText";


    public resolve() {
        this._resolvePages();
        this._resolveOutline();
        this._resolveStartPage();
        return this._promise as GivenViewerPromise<PDFStructureModel, any>;
    }

    private _resolveStartPage() {
        try {
            (this._document as any).getOpenAction().then((openAction: { dest: Array<{ num?: number, gen: number }> }) => {

                if (openAction === null || openAction.dest === null) {
                    this._startPage = 1;
                    this.checkResolvable();
                    return;
                }
                this._document.getPageIndex(openAction.dest[0] as RefProxy).then((page) => {
                    this._startPage = page + 1;
                    this.checkResolvable();
                }, () => {
                    this._startPage = 1;
                    this.checkResolvable();
                });
            });
        } catch (e) {
            this._startPage = 1;
            this.checkResolvable();
        }
    }

    private _resolvePages() {
        this._loadedPageCount = 0;

        for (let i = 1; i <= this._pageCount; i++) {
            const callback = this._createThumbnailDrawer(i);
            const additionalHref = new MyCoReMap<string, string>();
            additionalHref.set(PDFStructureBuilder.PDF_TEXT_HREF, i + "");
            const structureImage = new StructureImage("pdfPage", i + "", i, null, i + "", "pdfPage", callback, additionalHref);
            this._pages.push(structureImage);
            this._idPageMap.set(i, structureImage);
        }
    }

    private _createThumbnailDrawer(i) {
        let imgData = null;
        const collectedCallbacks = new Array<(string) => void>();
        return (callback: (string) => void) => {
            if (imgData == null) {
                collectedCallbacks.push((url) => {
                    if (imgData == null) {
                        imgData = url;
                    }
                    callback(url);
                });
                if (collectedCallbacks.length == 1) {
                    this._document.getPage(i).then((page) => {
                        this._renderPage(collectedCallbacks, page);
                    });
                }
            } else {
                callback(imgData);
            }
        }
    }

    private _renderPage(callbacks: Array<(string) => void>, page) {
        const originalSize = new Size2D(page.view[2] - page.view[0], page.view[3] - page.view[1]);//IviewPDFCanvas.getPageSize(page);
        const largest = Math.max(originalSize.width, originalSize.height);
        const vpScale = 256 / largest;
        const vp = page.getViewport({scale: vpScale});
        let thumbnailDrawCanvas = document.createElement("canvas");
        let thumbnailCanvasCtx = thumbnailDrawCanvas.getContext("2d");
        thumbnailDrawCanvas.width = (originalSize.width) * vpScale;
        thumbnailDrawCanvas.height = (originalSize.height) * vpScale;

        const task = page.render({canvasContext: thumbnailCanvasCtx, viewport: vp})
        task.promise.then(() => {
            this._loadedPageCount++;
            let imgUrl = thumbnailDrawCanvas.toDataURL();
            thumbnailDrawCanvas = null;
            thumbnailCanvasCtx = null;
            for (const callback of callbacks) {
                callback(imgUrl);
            }
        });


    }

    private _resolveOutline() {
        this._document.getOutline().then((nodes: Array<PDFTreeNode>) => {
            this._outline = nodes;
            this.resolveStructure();
        });
    }

    public getPageNumberFromDestination(dest: String, callback: (number: number) => void) {
        let promise;

        if (typeof dest === 'string') {
            promise = this._document.getDestination(dest);
        } else {
            promise = (window as any).Promise.resolve(dest);
        }

        promise.then((destination) => {
            if (!(destination instanceof Array)) {
                console.error("Invalid destination " + destination);
                return;
            } else {
                this._document.getPageIndex(destination[0]).then((pageNumber) => {
                    if (typeof pageNumber != "undefined" && pageNumber != null) {
                        if (pageNumber > this._pageCount) {
                            console.error("Destination outside of Document! (" + pageNumber + ")");
                        } else {
                            callback(pageNumber + 1);
                        }
                    }
                });
            }
        });
    }

    private getChapterFromOutline(parent: StructureChapter, nodes: Array<PDFTreeNode>, currentCount: number): Array<StructureChapter> {
        let chapterArr = new Array<StructureChapter>();
        for (let nodeIndex in nodes) {
            let currentNode = nodes[nodeIndex];
            let destResolver = ((copyChapter) => (callback) => {
                this.getPageNumberFromDestination(copyChapter.dest as string, callback);
            })(currentNode);
            let chapter = new StructureChapter(parent, "pdfChapter", Utils.hash(currentNode.title + currentCount++).toString(), currentNode.title, null, null, destResolver);
            let children = this.getChapterFromOutline(chapter, currentNode.items, currentCount++);
            chapter.chapter = children;
            chapterArr.push(chapter);
        }

        return chapterArr;
    }

    private checkResolvable() {
        if (this._structureModel !== null && this._outlineTodoCount === 0 && this._startPage !== -1) {
            this._structureModel.startPage = this._startPage;
            this._promise.resolve(this._structureModel);
        }
    }

    /**
     * Checks if all needed data is resolved and the structure model can be build.
     * Executes the Callback.
     */
    private resolveStructure() {
        if (typeof this._outline != "undefined") {
            this._rootChapter = new StructureChapter(null, "pdf", "0", this._name, null, null, () => 1);
            this._rootChapter.chapter = this.getChapterFromOutline(this._rootChapter, this._outline, 1);
            this._structureModel = new PDFStructureModel(this._rootChapter,
                this._pages,
                this._chapterPageMap,
                new MyCoReMap<string, StructureChapter>(),
                new MyCoReMap<string, StructureImage>(),
                this._refPageMap);
            this.checkResolvable();
        }
    }

}

interface PDFTreeNode{
    title: string;
    bold: boolean;
    italic: boolean;
    /**
     * - The color in RGB format to use for
     * display purposes.
     */
    color: Uint8ClampedArray;
    dest: string | Array<any> | null;
    url: string | null;
    unsafeUrl: string | undefined;
    newWindow: boolean | undefined;
    count: number | undefined;
    items: any[];
}
