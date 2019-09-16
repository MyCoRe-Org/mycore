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

/// <reference path="IIIFStructureModel.ts" />


namespace mycore.viewer.widgets.iiif {


    // import IIIFCanvas = mycore.viewer.components.manifest.IIIFCanvas;
    // import IIIFImage = mycore.viewer.components.manifest.IIIFImage;
    import Manifest = Manifesto.Manifest;
    import IRange = Manifesto.IRange;
    import ICanvas = Manifesto.ICanvas;
    import IAnnotation = Manifesto.IAnnotation;

    export class IIIFStructureBuilder {

        private static METS_NAMESPACE_URI = "http://www.loc.gov/METS/";
        private static XLINK_NAMESPACE_URI = "http://www.w3.org/1999/xlink";

        private hrefResolverElement = document.createElement("a");

        private _smLinkMap: MyCoReMap<string, Array<string>>;
        private _chapterIdMap: MyCoReMap<string, model.StructureChapter>;
        private _idFileMap: MyCoReMap<string, IAnnotation>;
        private _idPhysicalFileMap: MyCoReMap<string, Element>;

        private _chapterImageMap: MyCoReMap<string, model.StructureImage>;
        private _imageChapterMap: MyCoReMap<string, model.StructureChapter>;
        private _manifestChapter: model.StructureChapter;
        private _imageList: Array<model.StructureImage>;
        private _structureModel: IIIFStructureModel;
        private _idImageMap: MyCoReMap<string, model.StructureImage>;
        private _improvisationMap: MyCoReMap<string, boolean>;
        private _imageHrefImageMap: MyCoReMap<string, model.StructureImage>;

        private static NS_RESOLVER = {
            lookupNamespaceURI: (nsPrefix: String) => {
                if (nsPrefix == "manifest") {
                    return IIIFStructureBuilder.METS_NAMESPACE_URI;
                }
                return null;
            }
        };

        private static NS_MAP = (() => {
            let nsMap = new MyCoReMap<string, string>();
            nsMap.set("manifest", IIIFStructureBuilder.METS_NAMESPACE_URI);
            nsMap.set("xlink", IIIFStructureBuilder.XLINK_NAMESPACE_URI);
            return nsMap;
        })();

        constructor(private manifestDocument: Manifest, private tilePathBuilder: (href: string,width:number,height:number) => string) {

        }

        public processManifest(): model.StructureModel {
            this._idFileMap = this.getIdFileMap();


            const useFilesMap = new MyCoReMap<string, Array<Node>>();
            // this.getGroups().map(node => {
            //     return (<Element>node).getAttribute("USE");
            // })
            //     .map(s=>s.toUpperCase())
            //     .filter(s => s != "MASTER")
            //     .forEach(s => {
            //         let files = this.getFiles(s);
            //         useFilesMap.set(s, files);
            //         this._idFileMap.mergeIn(this.getIdFileMap(files));
            //     });


            this._chapterIdMap = new MyCoReMap<string, model.StructureChapter>();
            // this._idPhysicalFileMap = this.getIdPhysicalFileMap();
            this._idPhysicalFileMap = undefined;
            this._smLinkMap = new MyCoReMap<string, Array<string>>();
            this._chapterImageMap = new MyCoReMap<string, model.StructureImage>();
            this._imageChapterMap = new MyCoReMap<string, model.StructureChapter>();
            this._improvisationMap = new MyCoReMap<string, boolean>(); // see makeLink
            this._manifestChapter = this.processChapter(null,this.manifestDocument.getTopRanges()[0]);
            this._imageHrefImageMap = new MyCoReMap<string, model.StructureImage>();
            this._imageList = [];

            this._idImageMap = new MyCoReMap<string, model.StructureImage>();
            this.processImages();

            this._structureModel = new widgets.iiif.IIIFStructureModel(
                this._smLinkMap,
                this._manifestChapter,
                this._imageList,
                this._chapterImageMap,
                this._imageChapterMap,
                this._imageHrefImageMap);

            return this._structureModel;
        }

        // public getStructMap(type: string): Node {
        //     let logicalStructMapPath = "//mets:structMap[@TYPE='" + type + "']";
        //     return singleSelectShim(this.metsDocument, logicalStructMapPath, MetsStructureBuilder.NS_MAP);
        // }

        // public getGroups() {
        //     const fileGroupPath: string = '//mets:fileSec//mets:fileGrp';
        //     return getNodesShim(this.metsDocument, fileGroupPath,
        //         this.metsDocument.documentElement, MetsStructureBuilder.NS_MAP,
        //         /* XPathResult.UNORDERED_NODE_ITERATOR_TYPE */ 4, null);
        // }

        /**
         * Reads all files from a specific group
         * @param group {string} the group from wich the files should be selected
         * return the files a Array of nodes
         */
        // public getFiles(group: string): Array<Node> {
        //     let fileGroupPath = "//mets:fileSec//mets:fileGrp[@USE='" + group + "']";
        //     let fileSectionResult = singleSelectShim(this.metsDocument, fileGroupPath, MetsStructureBuilder.NS_MAP);
        //     let nodeArray: Array<Node> = [];
        //     if (fileSectionResult != null) {
        //         nodeArray = XMLUtil.nodeListToNodeArray(fileSectionResult.childNodes);
        //     }
        //     return nodeArray;
        // }

        // public getStructLinks(): Array<Element> {
        //     let structLinkPath = "//mets:structLink";
        //     let structLinkResult: Node = singleSelectShim(this.metsDocument, structLinkPath, MetsStructureBuilder.NS_MAP);
        //     let nodeArray: Array<Element> = [];
        //
        //     XMLUtil.iterateChildNodes(structLinkResult, (currentChild: Node) => {
        //         if (currentChild instanceof Element || "getAttribute" in currentChild) {
        //             nodeArray.push(<Element>currentChild);
        //         }
        //     });
        //
        //     return nodeArray;
        // }

        private processChapter(parent: model.StructureChapter, chapter: IRange): model.StructureChapter {
            // if (chapter.nodeName.toString() == "mets:mptr") {
            //     return;
            // }
            //TODO Chaptertype currently not in Manifest
            let chapterObject = new model.StructureChapter(parent, "", this.getIDFromURL(chapter.id), chapter.getDefaultLabel());
            // let chapterChildren = chapter.getRanges();

            this._chapterIdMap.set(chapterObject.id, chapterObject);

            chapter.getRanges().forEach((childChap: IRange) => {
                chapterObject.chapter.push(this.processChapter(chapterObject, childChap));
            });
            return chapterObject;
        }

        // private processFPTR(parent: model.StructureChapter, fptrElem: Element) {
        //     let elem = this.getFirstElementChild(fptrElem);
        //
        //     if (elem.nodeName.indexOf("seq")) {
        //         XMLUtil.iterateChildNodes(elem, (child: Node) => {
        //             if ((child instanceof Element || "getAttribute" in child)) {
        //                 this.parseArea(parent, <Element>child);
        //             }
        //         });
        //     } else if (elem.nodeName.indexOf("area")) {
        //         this.parseArea(parent, elem);
        //     }
        // }

        // private parseArea(parent: model.StructureChapter, area: Element) {
        //     // create blocklist if not exist
        //     let blockList: Array<{ fileId: string; fromId: string; toId: string }>;
        //     if (!parent.additional.has("blocklist")) {
        //         blockList = [];
        //         parent.additional.set("blocklist", blockList);
        //     } else {
        //         blockList = parent.additional.get("blocklist");
        //     }
        //     let fileID = area.getAttribute("FILEID");
        //     if (fileID == null) {
        //         throw `@FILEID of mets:area is required but not set!`;
        //     }
        //     let href: string = this.getAttributeNs(this.getFirstElementChild(this._idFileMap.get(fileID)), "xlink", "href");
        //     if (href == null) {
        //         throw `couldn't find href of @FILEID in mets:area! ${fileID}`;
        //     }
        //     let blockEntry: any = {
        //         fileId: href
        //     };
        //     let beType = area.getAttribute("BETYPE");
        //     if (beType == "IDREF") {
        //         blockEntry.fromId = area.getAttribute("BEGIN");
        //         blockEntry.toId = area.getAttribute("END");
        //     } else {
        //         console.warn("mets:area/@FILEID='" + href + "' has no BETYPE attribute");
        //     }
        //     blockList.push(blockEntry);
        // }

        private getIdFileMap(): MyCoReMap<string, IAnnotation> {
            let map = new MyCoReMap<string, IAnnotation>();
            this.manifestDocument.getSequences()[0].getCanvases().forEach((canvas: ICanvas) => {
                canvas.getImages().forEach((image: IAnnotation) => {
                        map.set(image.id, image);
                });
            });
            return map;
        }

        // private getIdPhysicalFileMap(): MyCoReMap<string, Element> {
        //     let map = new MyCoReMap<string, Element>();
        //     let physicalStructMap = <Element>this.getStructMap("PHYSICAL");
        //
        //     let metsDivs = this.getFirstElementChild(physicalStructMap).childNodes;
        //
        //     for (let i = 0; i < metsDivs.length; i++) {
        //         let child = <Element>metsDivs[i];
        //         if ("getAttribute" in child) {
        //             map.set(child.getAttribute("ID"), child);
        //         }
        //     }
        //
        //     return map;
        // }

        // private getFirstElementChild(node: Node): Element {
        //     if ("firstElementChild" in node) {
        //         return (<any>node).firstElementChild;
        //     } else {
        //         return <Element>node.firstChild;
        //     }
        // }

        // private getAttributeNs(element: any, namespaceKey: string, attribute: string) {
        //     if ("getAttributeNS" in element) {
        //         return element.getAttributeNS(MetsStructureBuilder.NS_MAP.get(namespaceKey), attribute);
        //     } else {
        //         return element.getAttribute(namespaceKey + ":" + attribute);
        //     }
        // }

        private processImages() {
            let count = 1;
            this.manifestDocument.getSequences()[0].getCanvases().forEach((canvas: ICanvas) => {
                const image = this.parseFile(canvas, count++);
                if (image != null) {
                    this._imageList.push(image);
                    this._idImageMap.set(this.getIDFromURL(canvas.id), image);
                }
            });

            this._imageList = this._imageList.sort((x, y) => x.order - y.order);

            this.makeLinks(this.manifestDocument.getTopRanges()[0]);

            this._imageList = this._imageList.filter((el => this._imageChapterMap.has(el.id)));
            this._imageList.forEach((image, i) => {
                // fix order
                image.order = i + 1;
                // build href map
                this._imageHrefImageMap.set(image.href, image);
            });
        }

        private makeLinks(elem: IRange) {
            let chapter = elem;
            elem.getCanvasIds().forEach((can: string) => {
                this.makeLink(this._chapterIdMap.get(this.getIDFromURL(chapter.id)), this._idImageMap.get(this.getIDFromURL(can)));
            });
            elem.getRanges().forEach((range: IRange) => {
                this.makeLinks(range)
            });
        }

        private makeLink(chapter: model.StructureChapter, image: model.StructureImage) {
            if (chapter.parent != null && !this._chapterImageMap.has(chapter.parent.id)) {
                this._improvisationMap.set(chapter.parent.id, true); // we flag this link as improvisation
                this._chapterImageMap.set(chapter.parent.id, image);
            }

            if (!this._chapterImageMap.has(chapter.id) || this._imageList.indexOf(this._chapterImageMap.get(chapter.id)) > this._imageList.indexOf(image) || (this._improvisationMap.has(chapter.id) && this._improvisationMap.get(chapter.id))) {
                this._chapterImageMap.set(chapter.id, image);
                this._improvisationMap.set(chapter.id, false);
            }

            if (!this._imageChapterMap.has(image.id)) {
                this._imageChapterMap.set(image.id, chapter);
            }

            if (!this._smLinkMap.has(chapter.id)) {
                this._smLinkMap.set(chapter.id, []);
            }
            this._smLinkMap.get(chapter.id).push(image.href);
        }

        // tei/translation.de/THULB_129846422_1801_1802_LLZ_001_18010701_001.xml -> de
        // private extractTranslationLanguage(href: string): string {
        //     return href.split("/")[1].split(".")[1];
        // }

        private parseFile(canvas: ICanvas, defaultOrder: number): model.StructureImage {
            const type: string = "page"; //TODO set real type, not in use currently
            const id: string = this.getHrefFromID(this.getIDFromURL(canvas.id));
            const order: number = parseInt('' + defaultOrder, 10); //TODO wo bekommen wir die Order sonst her?
            const orderLabel: string = canvas.getDefaultLabel();
            const contentIds: string = ""; //TODO was ist das? wo kommt es her?
            const additionalHrefs = new MyCoReMap<string, string>();

            let imgHref: string = null;
            let width: number = null;
            let height: number = null;
            let imgMimeType: string = null;
            // this.hrefResolverElement.href = './';
            // const base = this.hrefResolverElement.href;

            canvas.getImages().forEach((image: IAnnotation) => {
                let href: string = image.getResource().id;
                const mimetype: string = image.getResource().getFormat() ? image.getResource().getFormat().toString() : null;
                width = image.getResource().getWidth();
                height = image.getResource().getHeight();

                // this.hrefResolverElement.href = href;
                // href = this.hrefResolverElement.href.substr(base.length);

                imgHref = this.getHrefFromID(this.getIDFromURL(href));
                imgMimeType = mimetype; //TODO multiple Images?

                // const use = (<Element>file.parentNode).getAttribute('USE');
                // if (use === 'MASTER' || use === 'IVIEW2') {
                //     imgHref = href;
                //     imgMimeType = mimetype;
                // } else if (use === 'ALTO') {
                //     additionalHrefs.set(MetsStructureBuilder.ALTO_TEXT, href);
                // } else if (use.indexOf("TEI.")==0) {
                //     additionalHrefs.set(use, href);
                // } else {
                //     console.warn('Unknown File Group : ' + use);
                // }
            });

            if (imgHref === null) {
                console.warn('Unable to find MASTER|IVIEW2 file for ' + id);
                return null;
            }

            // TODO: Fix in mycore (we need a valid URL)
            // if (imgHref.indexOf('http:') + imgHref.indexOf('file:') + imgHref.indexOf('urn:') !== -3) {
            //     const parser = document.createElement('a');
            //     parser.href = imgHref;
            //     imgHref = parser.pathname;
            // }
            return new model.StructureImage(type, id, order, orderLabel, imgHref, imgMimeType, (cb) => {
                cb(this.tilePathBuilder(imgHref, width, height ));
            }, additionalHrefs, contentIds, width, height);
        }

        private getIDFromURL(url: String) {
            return url.substr(url.lastIndexOf("/") + 1);
        }

        private getHrefFromID(url: String) {
            return url.substr(url.indexOf("%2F") + 3);
        }
    }
}
