/// <reference path="MetsStructureModel.ts" />


namespace mycore.viewer.widgets.mets {


    export class MetsStructureBuilder {
        constructor(private metsDocument: Document, private tilePathBuilder: (href: string) => string) {

        }



        private static METS_NAMESPACE_URI = "http://www.loc.gov/METS/";
        private static XLINK_NAMESPACE_URI = "http://www.w3.org/1999/xlink";
        private static ALTO_TEXT = "AltoHref";
        private static TEI_TRANSCRIPTION = "TeiTranscriptionHref";
        private static TEI_TRANSLATION = "TeiTranslationHref";

        private hrefResolverElement = document.createElement("a");

        private static NS_RESOLVER = {
            lookupNamespaceURI: (nsPrefix: String) => {
                if (nsPrefix == "mets") {
                    return MetsStructureBuilder.METS_NAMESPACE_URI;
                }
                return null;
            }
        };

        private static NS_MAP = (() => {
            var nsMap = new MyCoReMap<string, string>();
            nsMap.set("mets", MetsStructureBuilder.METS_NAMESPACE_URI);
            nsMap.set("xlink", MetsStructureBuilder.XLINK_NAMESPACE_URI);
            return nsMap;
        })();

        public processMets(): model.StructureModel {
            var logicalStructMap = this.getStructMap("LOGICAL");
            var physicalStructMap = this.getStructMap("PHYSICAL");
            var files = this.getFiles("IVIEW2");

            if (files.length == 0) {
                files = this.getFiles("MASTER");
            }

            var altoFiles = this.getFiles("ALTO");
            var teiTranscriptionFiles = this.getFiles("TRANSCRIPTION");
            var teiTranslationFiles = this.getFiles("TRANSLATION");


            this._chapterIdMap = new MyCoReMap<string, model.StructureChapter>();
            this._idFileMap = this.getIdFileMap(files);
            this._idPhysicalFileMap = this.getIdPhysicalFileMap();

            if (altoFiles != null) {
                this._idFileMap.mergeIn(this.getIdFileMap(altoFiles));
            }

            if(teiTranscriptionFiles != null){
                this._idFileMap.mergeIn(this.getIdFileMap(teiTranscriptionFiles))
            }

            if(teiTranslationFiles != null){
                this._idFileMap.mergeIn(this.getIdFileMap(teiTranslationFiles))
            }

            this._chapterImageMap = new MyCoReMap<string, model.StructureImage>();
            this._imageChapterMap = new MyCoReMap<string, model.StructureChapter>();
            this._improvisationMap = new MyCoReMap<string, boolean>(); // see makeLink
            this._metsChapter = this.processChapter(null, this.getFirstElementChild(<Element>this.getStructMap("LOGICAL")), 1);
            this._imageHrefImageMap = new MyCoReMap<string, model.StructureImage>();
            this._imageList = new Array<model.StructureImage>();

            this._idImageMap = new MyCoReMap<string, model.StructureImage>();
            this.processImages();

            this._structureModel = new widgets.mets.MetsStructureModel( this._metsChapter,
                    this._imageList,
                    this._chapterImageMap,
                    this._imageChapterMap,
                    this._imageHrefImageMap,
                    altoFiles != null && altoFiles.length > 0 );

            return this._structureModel;
        }

        public getStructMap(type: string): Node {
            var logicalStructMapPath = "//mets:structMap[@TYPE='" + type + "']";
            return singleSelectShim(this.metsDocument, logicalStructMapPath, MetsStructureBuilder.NS_MAP);
        }

        /**
         * Reads all files from a specific group
         * @param group {string} the group from wich the files should be selected
         * return the files a Array of nodes
         */
        public getFiles(group: string): Array<Node> {
            var fileGroupPath = "//mets:fileSec//mets:fileGrp[@USE='" + group + "']";
            var fileSectionResult = singleSelectShim(this.metsDocument, fileGroupPath, MetsStructureBuilder.NS_MAP);
            if (fileSectionResult != null) {
                var nodeArray:Array<Node> = XMLUtil.nodeListToNodeArray(fileSectionResult.childNodes);
            } else {
                nodeArray = new Array<Node>();
            }

            return nodeArray;
        }

        public getStructLinks(): Array<Element> {
            var structLinkPath = "//mets:structLink";
            var structLinkResult: Node = singleSelectShim(this.metsDocument, structLinkPath, MetsStructureBuilder.NS_MAP);
            var nodeArray: Array<Element> = new Array();

            XMLUtil.iterateChildNodes(structLinkResult, (currentChild: Node) => {
                if (currentChild instanceof Element || "getAttribute" in currentChild) {
                    nodeArray.push(<Element>currentChild);
                }
            });

            return nodeArray;
        }

        private _chapterIdMap: MyCoReMap<string, model.StructureChapter>;
        private _idFileMap: MyCoReMap<string, Element>;
        private _idPhysicalFileMap: MyCoReMap<string, Element>;

        private _chapterImageMap: MyCoReMap<string, model.StructureImage>;
        private _imageChapterMap: MyCoReMap<string, model.StructureChapter>;
        private _metsChapter: model.StructureChapter;
        private _imageList: Array<model.StructureImage>;
        private _structureModel: MetsStructureModel;
        private _idImageMap: MyCoReMap<string, model.StructureImage>;
        private _improvisationMap:MyCoReMap<string, boolean>;
        private _imageHrefImageMap:MyCoReMap<string, model.StructureImage>;

        private processChapter(parent: model.StructureChapter, chapter: Element, defaultOrder:number): model.StructureChapter {
            if(chapter.nodeName.toString() == "mets:mptr"){
                    return;
            }

            var chapterObject = new model.StructureChapter(parent, chapter.getAttribute("TYPE"), chapter.getAttribute("ID"), chapter.getAttribute("LABEL"));
            var chapterChildren = chapter.childNodes;

            this._chapterIdMap.set(chapterObject.id, chapterObject);

            var that = this;
            for (var i = 0; i < chapterChildren.length; i++) {
                var elem = chapterChildren[i];
                if ((elem instanceof Element || "getAttribute" in elem)) {

                    if (elem.nodeName.indexOf("fptr") != -1) {
                        this.processFPTR(chapterObject, <Element>elem);
                    } else if (elem.nodeName.indexOf("div")) {
                        chapterObject.chapter.push(that.processChapter(chapterObject, <Element>elem, i+1));
                    }
                }

            }

            return chapterObject;
        }

        private processFPTR(parent:model.StructureChapter, fptrElem:Element) {
            var elem = this.getFirstElementChild(fptrElem);

            if (elem.nodeName.indexOf("seq")) {
                XMLUtil.iterateChildNodes(elem, (child:Node)=> {
                    if ((child instanceof Element || "getAttribute" in child)) {
                        var elem = <Element>child;
                        this.parseArea(parent, elem);
                    }
                });
            } else if (elem.nodeName.indexOf("area")) {
                this.parseArea(parent, elem);
            }
        }

        private parseArea(parent:model.StructureChapter, area:Element) {
            // create blocklist if not exist
            var blockList:Array<{fileId:string;fromId:string;toId:string}>;
            if (!parent.additional.has("blocklist")) {
                blockList = new Array<{fileId:string;fromId:string;toId:string}>();
                parent.additional.set("blocklist", blockList);
            } else {
                blockList = parent.additional.get("blocklist");
            }

            var beType = area.getAttribute("BETYPE");
            if (beType == "IDREF") {
                var href = this.getAttributeNs(this.getFirstElementChild(this._idFileMap.get(area.getAttribute("FILEID"))), "xlink", "href");
                blockList.push({
                    fileId : href,
                    fromId : area.getAttribute("BEGIN"),
                    toId : area.getAttribute("END")
                });
            } else {
                throw `unknown beType found! ${beType}`;
            }


        }

        private getIdFileMap(fileGrpChildren: Array<Node>): MyCoReMap<string, Element> {
            var map = new MyCoReMap<string, Element>();
            fileGrpChildren.forEach((node: Node, childrenIndex: Number) => {
                if (node instanceof Element || "getAttribute" in node) {
                    var element: Element = <Element> node;
                    map.set(element.getAttribute("ID"), element);
                }
            });
            return map;
        }

        private getIdPhysicalFileMap(): MyCoReMap<string, Element> {
            var map = new MyCoReMap<string, Element>();
            var physicalStructMap = <Element>this.getStructMap("PHYSICAL");

            var metsDivs = this.getFirstElementChild(physicalStructMap).childNodes;

            for (var i = 0; i < metsDivs.length; i++) {
                var child = <Element>metsDivs[i];
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
                var physFileDiv = this._idPhysicalFileMap.get(k);
                var image = this.parseFile(physFileDiv, count++);
                this._imageList.push(image);
                this._idImageMap.set(k, image);
            });

            this._imageList = this._imageList.sort((x, y)=>x.order - y.order);

            this.makeLinks();

            this._imageList = this._imageList.filter((el => this._imageChapterMap.has(el.id)));
            this._imageList.forEach((image, i)=> {
                // fix order
                image.order = i + 1
                // build href map
                this._imageHrefImageMap.set(image.href, image);
            });
        }

        private makeLinks() {
            var structLinkChildren = this.getStructLinks();
            structLinkChildren.forEach((elem: Element) => {
                var chapterId = this.getAttributeNs(elem, "xlink", "from");
                var physFileId = this.getAttributeNs(elem, "xlink", "to");
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
        }

        // tei/translation.de/THULB_129846422_1801_1802_LLZ_001_18010701_001.xml -> de
        private extractTranslationLanguage(href:string):string {
            return href.split("/")[1].split(".")[1];
        }

        private parseFile(physFileDiv:Element, defaultOrder:number):model.StructureImage {
            var img: model.StructureImage;
            var type: string = physFileDiv.getAttribute("TYPE");
            var id: string = physFileDiv.getAttribute("ID");
            var order: number = parseInt(physFileDiv.getAttribute("ORDER") || ""+defaultOrder, 10);
            var orderLabel: string = physFileDiv.getAttribute("ORDERLABEL");
            var contentIds:string = physFileDiv.getAttribute("CONTENTIDS");
            var additionalHrefs = new MyCoReMap<string, string>();


            var imgHref:string = null;
            var imgMimeType:string = null;
            this.hrefResolverElement.href ="./";
            var base = this.hrefResolverElement.href;

            XMLUtil.iterateChildNodes(physFileDiv, (child)=> {
                if (child instanceof Element || "getAttribute" in child) {
                    var childElement = <Element>child;
                    var fileId = childElement.getAttribute("FILEID");
                    var file = this._idFileMap.get(fileId);
                    var href:string = this.getAttributeNs(this.getFirstElementChild(file), "xlink", "href");
                    var mimetype:string = file.getAttribute("MIMETYPE");

                    this.hrefResolverElement.href = href;
                    href = this.hrefResolverElement.href.substr(base.length);

                    var use = (<Element>file.parentNode).getAttribute("USE");
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
                var parser = document.createElement('a');
                parser.href = imgHref;
                imgHref = parser.pathname;
            }

            var that = this;
            return new model.StructureImage(type, id, order, orderLabel, imgHref, imgMimeType, (cb) => {
                cb(that.tilePathBuilder(imgHref));
            }, additionalHrefs, contentIds);
        }
    }
}
