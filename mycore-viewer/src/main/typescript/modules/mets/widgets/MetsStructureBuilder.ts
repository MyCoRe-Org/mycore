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

/// <reference path="MetsStructureModel.ts" />


namespace mycore.viewer.widgets.mets {


    export class MetsStructureBuilder {

        private static METS_NAMESPACE_URI = "http://www.loc.gov/METS/";
        private static XLINK_NAMESPACE_URI = "http://www.w3.org/1999/xlink";
        private static ALTO_TEXT = "AltoHref";
        private static TEI_TRANSCRIPTION = "TeiTranscriptionHref";
        private static TEI_TRANSLATION = "TeiTranslationHref";

        private hrefResolverElement = document.createElement("a");

        private _smLinkMap: MyCoReMap<string, Array<string>>;
        private _chapterIdMap: MyCoReMap<string, model.StructureChapter>;
        private _idFileMap: MyCoReMap<string, Element>;
        private _idPhysicalFileMap: MyCoReMap<string, Element>;

        private _chapterImageMap: MyCoReMap<string, model.StructureImage>;
        private _imageChapterMap: MyCoReMap<string, model.StructureChapter>;
        private _metsChapter: model.StructureChapter;
        private _imageList: Array<model.StructureImage>;
        private _structureModel: MetsStructureModel;
        private _idImageMap: MyCoReMap<string, model.StructureImage>;
        private _improvisationMap: MyCoReMap<string, boolean>;
        private _imageHrefImageMap: MyCoReMap<string, model.StructureImage>;

        private static NS_RESOLVER = {
            lookupNamespaceURI: (nsPrefix: String) => {
                if (nsPrefix == "mets") {
                    return MetsStructureBuilder.METS_NAMESPACE_URI;
                }
                return null;
            }
        };

        private static NS_MAP = (() => {
            let nsMap = new MyCoReMap<string, string>();
            nsMap.set("mets", MetsStructureBuilder.METS_NAMESPACE_URI);
            nsMap.set("xlink", MetsStructureBuilder.XLINK_NAMESPACE_URI);
            return nsMap;
        })();

        constructor(private metsDocument: Document, private tilePathBuilder: (href: string) => string) {

        }

        public processMets(): model.StructureModel {
            let files = this.getFiles("IVIEW2");

            if (files.length == 0) {
                files = this.getFiles("MASTER");
            }

            let altoFiles = this.getFiles("ALTO");
            let teiTranscriptionFiles = this.getFiles("TRANSCRIPTION");
            let teiTranslationFiles = this.getFiles("TRANSLATION");

            this._chapterIdMap = new MyCoReMap<string, model.StructureChapter>();
            this._idFileMap = this.getIdFileMap(files);
            this._idPhysicalFileMap = this.getIdPhysicalFileMap();

            if (altoFiles != null) {
                this._idFileMap.mergeIn(this.getIdFileMap(altoFiles));
            }

            if (teiTranscriptionFiles != null) {
                this._idFileMap.mergeIn(this.getIdFileMap(teiTranscriptionFiles))
            }

            if (teiTranslationFiles != null) {
                this._idFileMap.mergeIn(this.getIdFileMap(teiTranslationFiles))
            }

            this._smLinkMap = new MyCoReMap<string, Array<string>>();
            this._chapterImageMap = new MyCoReMap<string, model.StructureImage>();
            this._imageChapterMap = new MyCoReMap<string, model.StructureChapter>();
            this._improvisationMap = new MyCoReMap<string, boolean>(); // see makeLink
            this._metsChapter = this.processChapter(null,
                this.getFirstElementChild(<Element>this.getStructMap("LOGICAL")));
            this._imageHrefImageMap = new MyCoReMap<string, model.StructureImage>();
            this._imageList = [];

            this._idImageMap = new MyCoReMap<string, model.StructureImage>();
            this.processImages();

            this._structureModel = new widgets.mets.MetsStructureModel(
                this._smLinkMap,
                this._metsChapter,
                this._imageList,
                this._chapterImageMap,
                this._imageChapterMap,
                this._imageHrefImageMap,
                altoFiles != null && altoFiles.length > 0);

            return this._structureModel;
        }

        public getStructMap(type: string): Node {
            let logicalStructMapPath = "//mets:structMap[@TYPE='" + type + "']";
            return singleSelectShim(this.metsDocument, logicalStructMapPath, MetsStructureBuilder.NS_MAP);
        }

        /**
         * Reads all files from a specific group
         * @param group {string} the group from wich the files should be selected
         * return the files a Array of nodes
         */
        public getFiles(group: string): Array<Node> {
            let fileGroupPath = "//mets:fileSec//mets:fileGrp[@USE='" + group + "']";
            let fileSectionResult = singleSelectShim(this.metsDocument, fileGroupPath, MetsStructureBuilder.NS_MAP);
            let nodeArray: Array<Node> = [];
            if (fileSectionResult != null) {
                nodeArray = XMLUtil.nodeListToNodeArray(fileSectionResult.childNodes);
            }
            return nodeArray;
        }

        public getStructLinks(): Array<Element> {
            let structLinkPath = "//mets:structLink";
            let structLinkResult: Node = singleSelectShim(this.metsDocument, structLinkPath, MetsStructureBuilder.NS_MAP);
            let nodeArray: Array<Element> = [];

            XMLUtil.iterateChildNodes(structLinkResult, (currentChild: Node) => {
                if (currentChild instanceof Element || "getAttribute" in currentChild) {
                    nodeArray.push(<Element>currentChild);
                }
            });

            return nodeArray;
        }

        private processChapter(parent: model.StructureChapter, chapter: Element): model.StructureChapter {
            if (chapter.nodeName.toString() == "mets:mptr") {
                return;
            }

            let chapterObject = new model.StructureChapter(parent, chapter.getAttribute("TYPE"), chapter.getAttribute("ID"), chapter.getAttribute("LABEL"));
            let chapterChildren = chapter.childNodes;

            this._chapterIdMap.set(chapterObject.id, chapterObject);

            for (let i = 0; i < chapterChildren.length; i++) {
                let elem = chapterChildren[i];
                if ((elem instanceof Element || "getAttribute" in elem)) {
                    if (elem.nodeName.indexOf("fptr") != -1) {
                        this.processFPTR(chapterObject, <Element>elem);
                    } else if (elem.nodeName.indexOf("div")) {
                        chapterObject.chapter.push(this.processChapter(chapterObject, <Element>elem));
                    }
                }

            }

            return chapterObject;
        }

        private processFPTR(parent: model.StructureChapter, fptrElem: Element) {
            let elem = this.getFirstElementChild(fptrElem);

            if (elem.nodeName.indexOf("seq")) {
                XMLUtil.iterateChildNodes(elem, (child: Node) => {
                    if ((child instanceof Element || "getAttribute" in child)) {
                        this.parseArea(parent, <Element>child);
                    }
                });
            } else if (elem.nodeName.indexOf("area")) {
                this.parseArea(parent, elem);
            }
        }

        private parseArea(parent: model.StructureChapter, area: Element) {
            // create blocklist if not exist
            let blockList: Array<{ fileId: string; fromId: string; toId: string }>;
            if (!parent.additional.has("blocklist")) {
                blockList = [];
                parent.additional.set("blocklist", blockList);
            } else {
                blockList = parent.additional.get("blocklist");
            }
            let fileID = area.getAttribute("FILEID");
            if (fileID == null) {
                throw `@FILEID of mets:area is required but not set!`;
            }
            let href: string = this.getAttributeNs(this.getFirstElementChild(this._idFileMap.get(fileID)), "xlink", "href");
            if (href == null) {
                throw `couldn't find href of @FILEID in mets:area! ${fileID}`;
            }
            let blockEntry: any = {
                fileId: href
            };
            let beType = area.getAttribute("BETYPE");
            if (beType == "IDREF") {
                blockEntry.fromId = area.getAttribute("BEGIN");
                blockEntry.toId = area.getAttribute("END");
            } else {
                console.warn("mets:area/@FILEID='" + href + "' has no BETYPE attribute");
            }
            blockList.push(blockEntry);
        }

        private getIdFileMap(fileGrpChildren: Array<Node>): MyCoReMap<string, Element> {
            let map = new MyCoReMap<string, Element>();
            fileGrpChildren.forEach((node: Node, childrenIndex: Number) => {
                if (node instanceof Element || "getAttribute" in node) {
                    let element: Element = <Element> node;
                    map.set(element.getAttribute("ID"), element);
                }
            });
            return map;
        }

        private getIdPhysicalFileMap(): MyCoReMap<string, Element> {
            let map = new MyCoReMap<string, Element>();
            let physicalStructMap = <Element>this.getStructMap("PHYSICAL");

            let metsDivs = this.getFirstElementChild(physicalStructMap).childNodes;

            for (let i = 0; i < metsDivs.length; i++) {
                let child = <Element>metsDivs[i];
                if ("getAttribute" in child) {
                    map.set(child.getAttribute("ID"), child);
                }
            }

            return map;
        }

        private getFirstElementChild(node: Node): Element {
            if ("firstElementChild" in node) {
                return (<any>node).firstElementChild;
            } else {
                return <Element>node.firstChild;
            }
        }

        private getAttributeNs(element: Element, namespaceKey: string, attribute: string) {
            if ("getAttributeNS" in element) {
                return element.getAttributeNS(MetsStructureBuilder.NS_MAP.get(namespaceKey), attribute);
            } else {
                return element.getAttribute(namespaceKey + ":" + attribute);
            }
        }

        private processImages() {
            let count = 1;
            this._idPhysicalFileMap.forEach((k: string, v: Element) => {
                let physFileDiv = this._idPhysicalFileMap.get(k);
                let image = this.parseFile(physFileDiv, count++);
                this._imageList.push(image);
                this._idImageMap.set(k, image);
            });

            this._imageList = this._imageList.sort((x, y) => x.order - y.order);

            this.makeLinks();

            this._imageList = this._imageList.filter((el => this._imageChapterMap.has(el.id)));
            this._imageList.forEach((image, i) => {
                // fix order
                image.order = i + 1;
                // build href map
                this._imageHrefImageMap.set(image.href, image);
            });
        }

        private makeLinks() {
            this.getStructLinks().forEach((elem: Element) => {
                let chapterId = this.getAttributeNs(elem, "xlink", "from");
                let physFileId = this.getAttributeNs(elem, "xlink", "to");
                this.makeLink(this._chapterIdMap.get(chapterId), this._idImageMap.get(physFileId));
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
        private extractTranslationLanguage(href: string): string {
            return href.split("/")[1].split(".")[1];
        }

        private parseFile(physFileDiv: Element, defaultOrder: number): model.StructureImage {
            let img: model.StructureImage;
            let type: string = physFileDiv.getAttribute("TYPE");
            let id: string = physFileDiv.getAttribute("ID");
            let order: number = parseInt(physFileDiv.getAttribute("ORDER") || "" + defaultOrder, 10);
            let orderLabel: string = physFileDiv.getAttribute("ORDERLABEL");
            let contentIds: string = physFileDiv.getAttribute("CONTENTIDS");
            let additionalHrefs = new MyCoReMap<string, string>();


            let imgHref: string = null;
            let imgMimeType: string = null;
            this.hrefResolverElement.href = "./";
            let base = this.hrefResolverElement.href;

            XMLUtil.iterateChildNodes(physFileDiv, (child) => {
                if (child instanceof Element || "getAttribute" in child) {
                    let childElement = <Element>child;
                    let fileId = childElement.getAttribute("FILEID");
                    let file = this._idFileMap.get(fileId);
                    let href: string = this.getAttributeNs(this.getFirstElementChild(file), "xlink", "href");
                    let mimetype: string = file.getAttribute("MIMETYPE");

                    this.hrefResolverElement.href = href;
                    href = this.hrefResolverElement.href.substr(base.length);

                    let use = (<Element>file.parentNode).getAttribute("USE");
                    if (use == "MASTER" || use == "IVIEW2") {
                        imgHref = href;
                        imgMimeType = mimetype;
                    } else if (use == "ALTO") {
                        additionalHrefs.set(MetsStructureBuilder.ALTO_TEXT, href);
                    } else if (use == "TRANSCRIPTION") {
                        additionalHrefs.set(MetsStructureBuilder.TEI_TRANSCRIPTION, href);
                    } else if (use == "TRANSLATION") {
                        additionalHrefs.set(MetsStructureBuilder.TEI_TRANSLATION + "." + this.extractTranslationLanguage(href), href);
                    } else {
                        console.log("Unknown File Group : " + use)
                    }
                }
            });


            // TODO: Fix in mycore (we need a valid URL)
            if (imgHref.indexOf("http:") + imgHref.indexOf("file:") + imgHref.indexOf("urn:") != -3) {
                let parser = document.createElement('a');
                parser.href = imgHref;
                imgHref = parser.pathname;
            }

            return new model.StructureImage(type, id, order, orderLabel, imgHref, imgMimeType, (cb) => {
                cb(this.tilePathBuilder(imgHref));
            }, additionalHrefs, contentIds);
        }
    }
}
