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

/// <reference path="MetsSettings.ts" />
/// <reference path="../widgets/canvas/HighlightAltoChapterCanvasPageLayer.ts" />
/// <reference path="../widgets/MetsStructureModel.ts" />
/// <reference path="../widgets/alto/AltoFile.ts" />
/// <reference path="../widgets/alto/AltoElement.ts" />

namespace mycore.viewer.components {
    import RequestAltoModelEvent = events.RequestAltoModelEvent;
    import WaitForEvent = events.WaitForEvent;
    import AltoElement = mycore.viewer.widgets.alto.AltoElement;

    export class MyCoReHighlightAltoComponent extends ViewerComponent {
        private pageLayout: widgets.canvas.PageLayout = null;
        private highlightLayer: widgets.canvas.HighlightAltoChapterCanvasPageLayer =
            new widgets.canvas.HighlightAltoChapterCanvasPageLayer();
        private _model: widgets.mets.MetsStructureModel;
        private _altoChapterContainer: AltoChapterContainer = null;

        private selectedChapter: string = null;
        private highlightedChapter: string = null;

        constructor(private _settings: MetsSettings, private container: JQuery) {
            super();
        }

        public init(): any {
            if (this._settings.doctype === 'mets') {
                this.trigger(new WaitForEvent(this, events.PageLayoutChangedEvent.TYPE));
                this.trigger(new WaitForEvent(this, events.RequestPageEvent.TYPE));
                this.trigger(new events.AddCanvasPageLayerEvent(this, 0, this.highlightLayer));
            }
        }

        public get handlesEvents(): string[] {
            if (this._settings.doctype === 'mets') {
                return [events.ChapterChangedEvent.TYPE,
                    events.PageLayoutChangedEvent.TYPE,
                    events.RequestPageEvent.TYPE,
                    events.MetsLoadedEvent.TYPE,
                    events.TextEditEvent.TYPE
                ];
            } else {
                return [];
            }
        }

        public get isEnabled(): boolean {
            return this._model != null && this._model._textContentPresent;
        }

        public getPageLayout(): widgets.canvas.PageLayout {
            return this.pageLayout;
        }

        public getPageController(): widgets.canvas.PageController {
            return this.pageLayout.getPageController();
        }

        public getAltoChapterContainer(): AltoChapterContainer {
            return this._altoChapterContainer;
        }

        public setChapter(chapterId: string, triggerChapterChangeEvent: boolean = false, forceChange: boolean = false) {
            if (!forceChange && this.selectedChapter === chapterId) {
                return;
            }
            this.selectedChapter = chapterId;
            if (this._altoChapterContainer === null || !this._altoChapterContainer.hasLoadedPages()) {
                return;
            }
            this.highlightLayer.selectedChapter = chapterId != null ? this._altoChapterContainer.chapters.get(chapterId) : null;
            this.handleDarkenPageAnimation();
            this.trigger(new events.RedrawEvent(this));
            if (triggerChapterChangeEvent) {
                const chapter: model.StructureChapter = this._altoChapterContainer.getChapter(chapterId);
                this.trigger(new events.ChapterChangedEvent(this, chapter));
            }
        }

        public setHighlightChapter(chapterId: string) {
            if (this._altoChapterContainer === null ||
                !this._altoChapterContainer.hasLoadedPages() ||
                this.highlightedChapter === chapterId) {
                return;
            }
            this.highlightLayer.highlightedChapter = chapterId != null ? this._altoChapterContainer.chapters.get(chapterId) : null;
            this.highlightedChapter = chapterId;
            if (this.selectedChapter == null) {
                this.handleDarkenPageAnimation();
            }
            this.trigger(new events.RedrawEvent(this));
        }

        public handleDarkenPageAnimation() {
            const selected: boolean = this.selectedChapter != null;
            const highlighted: boolean = this.highlightedChapter != null;
            let oldValue: number = 0;
            if (this.highlightLayer.fadeAnimation != null) {
                oldValue = this.highlightLayer.fadeAnimation.value;
                this.getPageController().removeAnimation(this.highlightLayer.fadeAnimation);
            }
            // fade out
            if (!selected && !highlighted) {
                if (oldValue === 0) {
                    return;
                }
                this.highlightLayer.fadeAnimation = new widgets.canvas.InterpolationAnimation(1000, oldValue, 0);
                this.getPageController().addAnimation(this.highlightLayer.fadeAnimation);
                return;
            }
            // fade in
            const alpha: number = selected ? 0.4 : 0.15;
            this.highlightLayer.fadeAnimation = new widgets.canvas.InterpolationAnimation(1000, oldValue, alpha);
            this.getPageController().addAnimation(this.highlightLayer.fadeAnimation);
        }

        public handle(e: mycore.viewer.widgets.events.ViewerEvent) {
            if (e.type === events.MetsLoadedEvent.TYPE) {
                const mle = <events.MetsLoadedEvent> e;
                this._model = <widgets.mets.MetsStructureModel>mle.mets.model;
                if (!this.isEnabled) {
                    return;
                }
                this._altoChapterContainer = new AltoChapterContainer(this._model);
                if (this.selectedChapter != null) {
                    this.setChapter(this.selectedChapter, false, true);
                }
                this.trigger(new events.RequestDesktopInputEvent(this, new HighlightAltoInputListener(this)));
            }
            if (e.type === events.RequestPageEvent.TYPE) {
                const rpe = <events.RequestPageEvent> e;
                this.trigger(new RequestAltoModelEvent(this, rpe._pageId, (page, altoHref, altoModel) => {
                    this._altoChapterContainer.addPage(rpe._pageId, altoHref, altoModel);
                    if (this.selectedChapter != null) {
                        this.setChapter(this.selectedChapter, false, true);
                    }
                }));
            }
            if (e.type === events.ChapterChangedEvent.TYPE) {
                const cce = <events.ChapterChangedEvent>e;
                if (cce == null || cce.chapter == null) {
                    return;
                }
                this.setChapter(cce.chapter.id);
            }
            if (e.type === events.PageLayoutChangedEvent.TYPE) {
                this.pageLayout = (<events.PageLayoutChangedEvent>e).pageLayout;
            }
            if (e.type === events.TextEditEvent.TYPE) {
                const tee = <events.TextEditEvent>e;
                this.highlightLayer.setEnabled(!tee.edit);
            }
        }

    }

    class HighlightAltoInputListener extends widgets.canvas.DesktopInputAdapter {

        constructor(protected component: MyCoReHighlightAltoComponent) {
            super();
        }

        public mouseClick(position: Position2D) {
            const chapterId: string = this.getChapterId(position);
            this.component.setChapter(chapterId, true);
        }

        public mouseMove(position: Position2D) {
            const chapterId: string = this.getChapterId(position);
            this.component.setHighlightChapter(chapterId);
        }

        private getChapterId(position: Position2D): string {
            const pageLayout = this.component.getPageLayout();
            if (pageLayout == null) {
                return null;
            }

            const pageHitInfo: widgets.canvas.PageHitInfo = pageLayout.getPageHitInfo(position);

            if (pageHitInfo.id == null) {
                return null;
            }
            const altoChapterContainer: AltoChapterContainer = this.component.getAltoChapterContainer();
            if (altoChapterContainer === null) {
                return null;
            }
            const chapters: MyCoReMap<string, AltoChapter> = altoChapterContainer.chapters;
            const pageChapterMap: MyCoReMap<string, string[]> = altoChapterContainer.pageChapterMap;

            const chapterIdsOnPage: string[] = pageChapterMap.get(pageHitInfo.id);
            if (chapterIdsOnPage == null || chapterIdsOnPage.length <= 0) {
                return null;
            }
            for (const chapterId of chapterIdsOnPage) {
                const altoChapter: AltoChapter = chapters.get(chapterId);
                const rectsOfChapter: Rect[] = altoChapter.boundingBoxMap.get(pageHitInfo.id);
                if (rectsOfChapter == null) {
                    continue;
                }
                for (const rectOfChapter of rectsOfChapter) {
                    const rect: Rect = rectOfChapter.scale(pageHitInfo.pageAreaInformation.scale);
                    if (rect.intersects(pageHitInfo.hit)) {
                        return chapterId;
                    }
                }
            }
            return null;
        }

    }

    export class AltoChapterContainer {

        public chapters: MyCoReMap<string, AltoChapter> = new MyCoReMap<string, AltoChapter>();
        public pageChapterMap: MyCoReMap<string, string[]> = new MyCoReMap<string, string[]>();

        private loadedPages: any = {};

        constructor(private _model: widgets.mets.MetsStructureModel) {
            this._model.smLinkMap.forEach((chapterId, linkedImages) => {
                this.chapters.set(chapterId, new AltoChapter(chapterId));
                for (const imageHref of linkedImages) {
                    if (!this.pageChapterMap.has(imageHref)) {
                        this.pageChapterMap.set(imageHref, []);
                    }
                    this.pageChapterMap.get(imageHref).push(chapterId);
                }
            });
        }

        hasLoadedPages(): boolean {
            return Object.keys(this.loadedPages).length > 0;
        }

        getAreaListOfChapter(chapter: model.StructureChapter): MetsArea[] {
            const blocklist = chapter.additional.get('blocklist');
            if (blocklist == null) {
                return [];
            }
            return blocklist.map((block: { fileId: string, fromId: string, toId: string }) => {
                return new MetsArea(block.fileId, block.fromId, block.toId);
            });
        }

        getChapter(chapterId: string): model.StructureChapter {
            return this.findChapter(this._model.rootChapter, chapterId);
        }

        findChapter(from: model.StructureChapter, chapterId: string): model.StructureChapter {
            if (from.id === chapterId) {
                return from;
            }
            for (const childChapter of from.chapter) {
                const foundChapter = this.findChapter(childChapter, chapterId);
                if (foundChapter != null) {
                    return foundChapter;
                }
            }
            return null;
        }

        getBlocklistOfChapterAndAltoHref(chapterId: string, altoHref: string): MetsArea[] {
            const chapter: model.StructureChapter = this.getChapter(chapterId);
            if (chapter == null) {
                return [];
            }
            return this.getAreaListOfChapter(chapter).filter(area => {
                return altoHref === area.altoRef;
            });
        }

        getAllBlocklistChapters(from: model.StructureChapter): model.StructureChapter[] {
            let chapters: model.StructureChapter[] = [];
            if (from.additional.get('blocklist') != null) {
                chapters.push(from);
            }
            from.chapter.forEach(childChapter => {
                chapters = chapters.concat(this.getAllBlocklistChapters(childChapter));
            });
            return chapters;
        }

        addPage(pageId: string, altoHref: string, alto: widgets.alto.AltoFile) {
            if (this.loadedPages[pageId] != null) {
                return;
            }
            this.loadedPages[pageId] = true;
            this.pageChapterMap.hasThen(pageId, (chapterIds: string[]) => {
                // calculate areas for each chapter
                chapterIds.map(chapterId => this.chapters.get(chapterId)).forEach((chapter) => {
                    chapter.addPage(pageId, alto, this.getBlocklistOfChapterAndAltoHref(chapter.chapterId, altoHref));
                });
                // fix stuff, needs to be done after all chapters are added
                chapterIds.map(chapterId => this.chapters.get(chapterId)).forEach((chapter, i, chapters) => {
                    // maximize
                    const maximizedRect = chapter.maximize(pageId);
                    if (maximizedRect == null) {
                        return;
                    }
                    chapter.boundingBoxMap.set(pageId, [maximizedRect]);

                    // intersect with other chapters
                    for (let j = 0; j < chapters.length; j++) {
                        if (i === j) {
                            continue;
                        }
                        // try to remove bounding box
                        const otherBoundingBox = chapters[j].maximize(pageId);
                        chapter.fixBoundingBox(pageId, otherBoundingBox);
                        // fix area intersections of chapters on the same page
                        chapter.fixIntersections(pageId, chapters[j]);
                    }

                    // cut start and end
                    const altoRects = chapter.altoRectMap.get(pageId);
                    chapter.cutVerticalBoundingBox(pageId, altoRects[0].getY());
                    chapter.cutVerticalBoundingBox(pageId,
                        altoRects[altoRects.length - 1].getY() + altoRects[altoRects.length - 1].getHeight());

                    // remove areas which does not contain any content
                    chapter.fixEmptyAreas(pageId, alto);
                });
            });
        }
    }

    /**
     * Contains the bounding box paragraph information for a chapter.
     */
    export class AltoChapter {

        /**
         * Map of the original mets:area's read from the mets.xml for this chapter.
         *
         * key: image (e.g. ThULB_129489832_1941_Perthes_0034.tif)
         * value: array of MetsArea's which are assigned to this chapter on the specific page (image)
         */
        public metsAreas: MyCoReMap<string, MetsArea[]>;

        /**
         * Map of rectangles's for a given page. Those rectangles's are calculated using the original mets:area's when
         * adding a page. Each area has a BEGIN and END attribute containing a range of combined paragraphs e.g.
         * Paragraph_3 to Paragraph_6 (so paragraph 3,4,5 and 6). Each paragraph is converted to a rectangle and those
         * rectangles are stored in this map (a small padding is added too).
         *
         * Important note: Those ALTO paragraph bounding boxes are created when adding a page and never changed after.
         * They will be used to calculate the bounding box of the whole chapter and for intersection tests.
         *
         * key: image (e.g. ThULB_129489832_1941_Perthes_0034.tif)
         * value: array of rectangles where each rectangle is the bounding box of a ALTO paragraph (+ small padding)
         */
        public altoRectMap: MyCoReMap<string, Rect[]>;

        /**
         * Map of rectangles's for a given page. Those rectangles define the bounding box of an AltoChapter for one
         * page. This map is calculated in the AltoChapterContainer#addPage method using multiple steps (including
         * maximize(), fixBoundingBox(), fixIntersections(), cutVerticalBoundingBox() and fixEmptyAreas()).
         *
         * key: image (e.g. ThULB_129489832_1941_Perthes_0034.tif)
         * value: array of rectangles
         */
        public boundingBoxMap: MyCoReMap<string, Rect[]>;

        constructor(public chapterId: string) {
            this.boundingBoxMap = new MyCoReMap<string, Rect[]>();
            this.altoRectMap = new MyCoReMap<string, Rect[]>();
            this.metsAreas = new MyCoReMap<string, MetsArea[]>();
        }

        public addPage(pageId: string, altoFile: widgets.alto.AltoFile, metsAreas: MetsArea[]) {
            const altoBlocks: widgets.alto.AltoElement[] = this.getAltoBlocks(altoFile, metsAreas);
            const areaRects = this.getAreaRects(altoFile, altoBlocks);
            this.altoRectMap.set(pageId, areaRects);
            this.boundingBoxMap.set(pageId, areaRects);
            this.metsAreas.set(pageId, metsAreas);
        }

        public maximize(pageId: string): Rect {
            const boundingBox = this.boundingBoxMap.get(pageId);
            if (boundingBox == null || boundingBox.length === 0) {
                return null;
            }
            return boundingBox.reduce((a, b) => {
                return a.maximizeRect(b);
            });
        }

        public fixBoundingBox(pageId: string, rect: Rect) {
            if (rect == null) {
                return;
            }
            if (this.intersectsText(pageId, rect)) {
                return;
            }
            let thisBoundingBox = this.boundingBoxMap.get(pageId);
            for (const thisBBRect of thisBoundingBox) {
                if (!thisBBRect.intersectsArea(rect)) {
                    continue;
                }
                thisBoundingBox = thisBoundingBox.filter(r => r !== thisBBRect);
                thisBBRect.difference(rect).forEach(r => thisBoundingBox.push(r));
                this.boundingBoxMap.set(pageId, thisBoundingBox);
                this.fixBoundingBox(pageId, rect);
                return;
            }
        }

        /**
         * Checks if the alto blocks are intersected by the given rect.
         *
         * @param pageId id of the page
         * @param rect the rect
         */
        public intersectsText(pageId: string, rect: Rect): boolean {
            const rects = this.altoRectMap.get(pageId);
            for (const altoRect of rects) {
                if (altoRect.intersectsArea(rect)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Fix area intersections of chapters on the same page.
         *
         * @param pageId the page to fix
         * @param other the other chapter area
         */
        public fixIntersections(pageId: string, other: AltoChapter) {
            let thisAreas = this.boundingBoxMap.get(pageId);
            const otherAreas = other.boundingBoxMap.get(pageId);
            for (const thisArea of thisAreas) {
                for (const otherArea of otherAreas) {
                    if (!thisArea.intersectsArea(otherArea)) {
                        continue;
                    }
                    thisAreas = thisAreas.filter(rect => rect !== thisArea);
                    thisArea.difference(otherArea).forEach(rect => thisAreas.push(rect));
                    this.boundingBoxMap.set(pageId, thisAreas);
                    this.fixIntersections(pageId, other);
                    return;
                }
            }
        }

        /**
         * Cuts the bounding box on y.
         *
         * @param pageId page to cut
         * @param y the vertical position to cut
         */
        public cutVerticalBoundingBox(pageId: string, y: number) {
            let thisAreas = this.boundingBoxMap.get(pageId);
            for (const thisArea of thisAreas) {
                if (!thisArea.intersectsVertical(y)) {
                    continue;
                }
                thisAreas = thisAreas.filter(rect => rect !== thisArea);
                const cutY = y - thisArea.getY();
                thisAreas.push(Rect.fromXYWH(thisArea.getX(), thisArea.getY(), thisArea.getWidth(), cutY));
                thisAreas.push(Rect.fromXYWH(thisArea.getX(), thisArea.getY() + cutY + 1, thisArea.getWidth(),
                    thisArea.getHeight() - (cutY + 1)));
                this.boundingBoxMap.set(pageId, thisAreas);
            }
        }

        /**
         * Removes areas which does not contain any ALTO content.
         *
         * @param pageId the page to fix
         * @param alto the alto file
         */
        public fixEmptyAreas(pageId: string, alto: widgets.alto.AltoFile) {
            let thisAreas = this.boundingBoxMap.get(pageId);
            const thisMetsAreas = this.metsAreas.get(pageId);
            const textBlockIds = alto.allElements.map(block => block.getId());
            const textAreas: Rect[] = alto.allElements
                .filter(block => {
                    return thisMetsAreas.some(metsArea => metsArea.contains(textBlockIds, block.getId()));
                })
                .map(block => block.asRect());
            thisAreas = thisAreas.filter(area => {
                for (const textArea of textAreas) {
                    if (area.intersectsArea(textArea)) {
                        return true;
                    }
                }
                return false;
            });
            this.boundingBoxMap.set(pageId, thisAreas);
        }

        private getAltoBlocks(altoFile: widgets.alto.AltoFile,
                              metsAreas: MetsArea[]): widgets.alto.AltoElement[] {
            const allBlocks = altoFile.allElements;
            const ids = allBlocks.map(block => block.getId());
            const blocks: widgets.alto.AltoElement[] = [];
            metsAreas.map(metsArea => [ids.indexOf(metsArea.begin), ids.indexOf(metsArea.end)])
                .forEach(([fromIndex, toIndex]) => {
                        for (let i = fromIndex; i <= toIndex; i++) {
                            const blockToHighlight = allBlocks[i];
                            if (blockToHighlight == null) {
                                continue;
                            }
                            blocks.push(blockToHighlight);
                        }
                    }
                );
            return blocks;
        }

        private getAreaRects(altoFile: widgets.alto.AltoFile, blocks: widgets.alto.AltoElement[]): Rect[] {
            const padding: number = Math.ceil(altoFile.pageHeight * 0.004);
            return blocks.map(block => {
                return Rect
                    .fromXYWH(block.getBlockHPos(), block.getBlockVPos(), block.getWidth(), block.getHeight())
                    .increase(padding);
            });
        }

    }

    export class MetsArea {

        constructor(public altoRef: string, public begin: string, public end: string) {
        }

        public contains(blockIds: string[], paragraph: string): boolean {
            const index = blockIds.indexOf(paragraph);
            return index >= blockIds.indexOf(this.begin) && index <= blockIds.indexOf(this.end);
        }

    }

}
addViewerComponent(mycore.viewer.components.MyCoReHighlightAltoComponent);
