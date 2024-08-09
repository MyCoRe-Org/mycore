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


import {MyCoReMap} from "../../base/Utils";
import {StructureChapter} from "../../base/components/model/StructureChapter";
import {StructureImage} from "../../base/components/model/StructureImage";
import {Annotation, Canvas, IIIFResource, Manifest, Range} from "manifesto.js";
import {StructureModel} from "../../base/components/model/StructureModel";
import {IIIFStructureModel} from "./IIIFStructureModel";

export class IIIFStructureBuilder {

    private hrefResolverElement: any = document.createElement('a');

    private vSmLinkMap: MyCoReMap<string, string[]>;
    private vChapterIdMap: MyCoReMap<string, StructureChapter>;
    private vIdFileMap: MyCoReMap<string, Annotation>;
    private vIdPhysicalFileMap: MyCoReMap<string, Element>;

    private vChapterImageMap: MyCoReMap<string, StructureImage>;
    private vImageChapterMap: MyCoReMap<string, StructureChapter>;
    private vManifestChapter: StructureChapter;
    private vImageList: StructureImage[];
    private vStructureModel: IIIFStructureModel;
    private vIdImageMap: MyCoReMap<string, StructureImage>;
    private vImprovisationMap: MyCoReMap<string, boolean>;
    private vImageHrefImageMap: MyCoReMap<string, StructureImage>;

    constructor(private manifestDocument: Manifest,
                private tilePathBuilder: (href: string, width: number, height: number) => string,
                private imageAPIURL) {

    }

    public processManifest(): StructureModel {
        this.vIdFileMap = this.getIdFileMap();

        const useFilesMap = new MyCoReMap<string, Node[]>();

        this.vChapterIdMap = new MyCoReMap<string, StructureChapter>();
        this.vIdPhysicalFileMap = undefined;
        this.vSmLinkMap = new MyCoReMap<string, string[]>();
        this.vChapterImageMap = new MyCoReMap<string, StructureImage>();
        this.vImageChapterMap = new MyCoReMap<string, StructureChapter>();
        this.vImprovisationMap = new MyCoReMap<string, boolean>(); // see makeLink
        this.manifestDocument
        this.vManifestChapter = this.processChapter(null, this.manifestDocument.getTopRanges()[0],
            this.manifestDocument.getSequences()[0].getCanvases());
        this.vImageHrefImageMap = new MyCoReMap<string, StructureImage>();
        this.vImageList = [];

        this.vIdImageMap = new MyCoReMap<string, StructureImage>();
        this.processImages();

        this.vStructureModel = new IIIFStructureModel(
            this.vSmLinkMap,
            this.vManifestChapter,
            this.vImageList,
            this.vChapterImageMap,
            this.vImageChapterMap,
            this.vImageHrefImageMap);

        return this.vStructureModel;
    }

    private processChapter(parent: StructureChapter, chapter: Range, cans: Canvas[]): StructureChapter {
        // if (chapter.nodeName.toString() == "mets:mptr") {
        //     return;
        // }
        //TODO Chaptertype currently not in Manifest
        let chapterObject;
        if (chapter === undefined || (chapter.getCanvasIds().length === 0 && chapter.getRanges().length === 0)) {
            chapterObject = new StructureChapter(parent,
                '', 'LOG_0', this.manifestDocument.getDefaultLabel());
            this.vChapterIdMap.set(chapterObject.id, chapterObject);
            cans.forEach((can: Canvas) => {
                const childChap = new StructureChapter(chapterObject,
                    chapterObject, this.getIDFromURL(can.id), can.getDefaultLabel());
                chapterObject.chapter.push(childChap);
                this.vChapterIdMap.set(childChap.id, childChap);
            });
        } else {
            chapterObject = new StructureChapter(parent,
                '', this.getIDFromURL(chapter.id), chapter.getDefaultLabel());
            this.vChapterIdMap.set(chapterObject.id, chapterObject);
            chapter.getRanges().forEach((childChap: Range) => {
                chapterObject.chapter.push(this.processChapter(chapterObject, childChap, []));
            });
        }

        return chapterObject;
    }

    private getIdFileMap(): MyCoReMap<string, Annotation> {
        const map = new MyCoReMap<string, Annotation>();
        this.manifestDocument.getSequences()[0].getCanvases().forEach((canvas: Canvas) => {
            canvas.getImages().forEach((image: Annotation) => {
                if (image.id === undefined) {
                    map.set(image.getResource().id, image);
                } else {
                    map.set(image.id, image);
                }
            });
        });
        return map;
    }

    private processImages() {
        let count = 1;
        this.manifestDocument.getSequences()[0].getCanvases().forEach((canvas: Canvas) => {
            const image = this.parseFile(canvas, count++);
            if (image !== null) {
                this.vImageList.push(image);
                this.vIdImageMap.set(this.getIDFromURL(canvas.id), image);
            }
        });

        this.vImageList = this.vImageList.sort((x, y) => x.order - y.order);

        if (this.manifestDocument.getTopRanges().length > 0) {
            this.makeLinks(this.manifestDocument.getTopRanges()[0]);
        } else {
            this.makeLinksWithoutStructures(this.manifestDocument.getSequences()[0].getCanvases());
        }

        this.vImageList = this.vImageList.filter((el) => this.vImageChapterMap.has(el.id));
        this.vImageList.forEach((image, i) => {
            // fix order
            image.order = i + 1;
            // build href map
            this.vImageHrefImageMap.set(image.href, image);
        });
    }

    private makeLinks(elem: Range) {
        const chapter = elem;
        if (elem.getCanvasIds().length === 0 && elem.getRanges().length === 0) {
            this.makeLinksWithoutStructures(this.manifestDocument.getSequences()[0].getCanvases());
        }
        elem.getCanvasIds().forEach((can: string) => {
            this.makeLink(this.vChapterIdMap.get(this.getIDFromURL(chapter.id)),
                this.vIdImageMap.get(this.getIDFromURL(can)));
        });
        elem.getRanges().forEach((range: Range) => {
            this.makeLinks(range);
        });
    }

    private makeLinksWithoutStructures(cans: Canvas[]) {
        cans.forEach((can: Canvas) => {
            this.makeLink(this.vChapterIdMap.get(this.getIDFromURL(can.id)),
                this.vIdImageMap.get(this.getIDFromURL(can.id)));
        });
    }

    private makeLink(chapter: StructureChapter, image: StructureImage) {
        if (chapter.parent !== null && !this.vChapterImageMap.has(chapter.parent.id)) {
            this.vImprovisationMap.set(chapter.parent.id, true); // we flag this link as improvisation
            this.vChapterImageMap.set(chapter.parent.id, image);
        }

        if (!this.vChapterImageMap.has(chapter.id)
            || this.vImageList.indexOf(this.vChapterImageMap.get(chapter.id)) > this.vImageList.indexOf(image)
            || (this.vImprovisationMap.has(chapter.id) && this.vImprovisationMap.get(chapter.id))) {
            this.vChapterImageMap.set(chapter.id, image);
            this.vImprovisationMap.set(chapter.id, false);
        }

        if (!this.vImageChapterMap.has(image.id)) {
            this.vImageChapterMap.set(image.id, chapter);
        }

        if (!this.vSmLinkMap.has(chapter.id)) {
            this.vSmLinkMap.set(chapter.id, []);
        }
        this.vSmLinkMap.get(chapter.id).push(image.href);
    }

    private parseFile(canvas: Canvas, defaultOrder: number): StructureImage {
        const type: string = 'page'; //TODO set real type, not in use currently
        const id: string = this.getHrefFromID(this.getIDFromURL(canvas.id));
        const order: number = parseInt('' + defaultOrder, 10); //TODO wo bekommen wir die Order sonst her?
        const orderLabel: string = canvas.getDefaultLabel();
        const contentIds: string = ''; //TODO was ist das? wo kommt es her?
        const additionalHrefs = new MyCoReMap<string, string>();

        let imgHref: string = null;
        let width: number = null;
        let height: number = null;
        let imgMimeType: string = null;

        canvas.getImages().forEach((image: Annotation) => {
            let href: string = image.getResource().getServices()[0].id;
            const mimetype: string = image.getResource().getFormat() ? image.getResource().getFormat().toString() : null;
            width = image.getResource().getWidth();
            height = image.getResource().getHeight();

            imgHref = href.substr(href.indexOf(this.imageAPIURL) + this.imageAPIURL.length);
            imgMimeType = mimetype; //TODO multiple Images?

        });

        if (imgHref === null) {
            console.warn('Unable to find MASTER|IVIEW2 file for ' + id);
            return null;
        }

        return new StructureImage(type, id, order, orderLabel, imgHref, imgMimeType, (cb) => {
            cb(this.tilePathBuilder(imgHref, width, height));
        }, additionalHrefs, contentIds, width, height);
    }

    private getIDFromURL(url: string) {
        return url.substr(url.lastIndexOf('/') + 1);
    }

    private getHrefFromID(url: string) {
        if (url.indexOf('%2F') > -1) {
            return url.substr(url.indexOf('%2F') + 3);
        }
        return url;
    }
}

