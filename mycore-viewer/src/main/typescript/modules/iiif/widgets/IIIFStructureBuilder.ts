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

    import Manifest = Manifesto.Manifest;
    import IRange = Manifesto.IRange;
    import ICanvas = Manifesto.ICanvas;
    import IAnnotation = Manifesto.IAnnotation;

    export class IIIFStructureBuilder {

        private hrefResolverElement: any = document.createElement('a');

        private vSmLinkMap: MyCoReMap<string, string[]>;
        private vChapterIdMap: MyCoReMap<string, model.StructureChapter>;
        private vIdFileMap: MyCoReMap<string, IAnnotation>;
        private vIdPhysicalFileMap: MyCoReMap<string, Element>;

        private vChapterImageMap: MyCoReMap<string, model.StructureImage>;
        private vImageChapterMap: MyCoReMap<string, model.StructureChapter>;
        private vManifestChapter: model.StructureChapter;
        private vImageList: model.StructureImage[];
        private vStructureModel: IIIFStructureModel;
        private vIdImageMap: MyCoReMap<string, model.StructureImage>;
        private vImprovisationMap: MyCoReMap<string, boolean>;
        private vImageHrefImageMap: MyCoReMap<string, model.StructureImage>;

        constructor(private manifestDocument: Manifest,
                    private tilePathBuilder: (href: string, width: number, height: number) => string) {

        }

        public processManifest(): model.StructureModel {
            this.vIdFileMap = this.getIdFileMap();

            const useFilesMap = new MyCoReMap<string, Node[]>();

            this.vChapterIdMap = new MyCoReMap<string, model.StructureChapter>();
            this.vIdPhysicalFileMap = undefined;
            this.vSmLinkMap = new MyCoReMap<string, string[]>();
            this.vChapterImageMap = new MyCoReMap<string, model.StructureImage>();
            this.vImageChapterMap = new MyCoReMap<string, model.StructureChapter>();
            this.vImprovisationMap = new MyCoReMap<string, boolean>(); // see makeLink
            this.vManifestChapter = this.processChapter(null, this.manifestDocument.getTopRanges()[0]);
            this.vImageHrefImageMap = new MyCoReMap<string, model.StructureImage>();
            this.vImageList = [];

            this.vIdImageMap = new MyCoReMap<string, model.StructureImage>();
            this.processImages();

            this.vStructureModel = new widgets.iiif.IIIFStructureModel(
                this.vSmLinkMap,
                this.vManifestChapter,
                this.vImageList,
                this.vChapterImageMap,
                this.vImageChapterMap,
                this.vImageHrefImageMap);

            return this.vStructureModel;
        }

        private processChapter(parent: model.StructureChapter, chapter: IRange): model.StructureChapter {
            // if (chapter.nodeName.toString() == "mets:mptr") {
            //     return;
            // }
            //TODO Chaptertype currently not in Manifest
            const chapterObject = new model.StructureChapter(parent,
                '', this.getIDFromURL(chapter.id), chapter.getDefaultLabel());
            // let chapterChildren = chapter.getRanges();

            this.vChapterIdMap.set(chapterObject.id, chapterObject);

            chapter.getRanges().forEach((childChap: IRange) => {
                chapterObject.chapter.push(this.processChapter(chapterObject, childChap));
            });
            return chapterObject;
        }

        private getIdFileMap(): MyCoReMap<string, IAnnotation> {
            const map = new MyCoReMap<string, IAnnotation>();
            this.manifestDocument.getSequences()[0].getCanvases().forEach((canvas: ICanvas) => {
                canvas.getImages().forEach((image: IAnnotation) => {
                        map.set(image.id, image);
                });
            });
            return map;
        }

        private processImages() {
            let count = 1;
            this.manifestDocument.getSequences()[0].getCanvases().forEach((canvas: ICanvas) => {
                const image = this.parseFile(canvas, count++);
                if (image !== null) {
                    this.vImageList.push(image);
                    this.vIdImageMap.set(this.getIDFromURL(canvas.id), image);
                }
            });

            this.vImageList = this.vImageList.sort((x, y) => x.order - y.order);

            this.makeLinks(this.manifestDocument.getTopRanges()[0]);

            this.vImageList = this.vImageList.filter((el) => this.vImageChapterMap.has(el.id));
            this.vImageList.forEach((image, i) => {
                // fix order
                image.order = i + 1;
                // build href map
                this.vImageHrefImageMap.set(image.href, image);
            });
        }

        private makeLinks(elem: IRange) {
            const chapter = elem;
            elem.getCanvasIds().forEach((can: string) => {
                this.makeLink(this.vChapterIdMap.get(this.getIDFromURL(chapter.id)),
                    this.vIdImageMap.get(this.getIDFromURL(can)));
            });
            elem.getRanges().forEach((range: IRange) => {
                this.makeLinks(range);
            });
        }

        private makeLink(chapter: model.StructureChapter, image: model.StructureImage) {
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

        private parseFile(canvas: ICanvas, defaultOrder: number): model.StructureImage {
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

            canvas.getImages().forEach((image: IAnnotation) => {
                let href: string = image.getResource().id;
                const mimetype: string = image.getResource().getFormat() ? image.getResource().getFormat().toString() : null;
                width = image.getResource().getWidth();
                height = image.getResource().getHeight();

                imgHref = this.getHrefFromID(this.getIDFromURL(href));
                imgMimeType = mimetype; //TODO multiple Images?

            });

            if (imgHref === null) {
                console.warn('Unable to find MASTER|IVIEW2 file for ' + id);
                return null;
            }

            return new model.StructureImage(type, id, order, orderLabel, imgHref, imgMimeType, (cb) => {
                cb(this.tilePathBuilder(imgHref, width, height));
            }, additionalHrefs, contentIds, width, height);
        }

        private getIDFromURL(url: String) {
            return url.substr(url.lastIndexOf('/') + 1);
        }

        private getHrefFromID(url: String) {
            return url.substr(url.indexOf('%2F') + 3);
        }
    }
}
