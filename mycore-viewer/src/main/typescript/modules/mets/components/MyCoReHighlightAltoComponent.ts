/// <reference path="MetsSettings.ts" />
/// <reference path="../widgets/canvas/HighlightAltoCanvasPageLayer.ts" />
/// <reference path="../widgets/MetsStructureModel.ts" />
/// <reference path="../widgets/alto/AltoFile.ts" />
/// <reference path="../widgets/alto/AltoElement.ts" />

namespace mycore.viewer.components {
    import RequestAltoModelEvent = events.RequestAltoModelEvent;
    import WaitForEvent = events.WaitForEvent;

    export class MyCoReHighlightAltoComponent extends ViewerComponent {
        private pageLayout:widgets.canvas.PageLayout = null;
        private highlightLayer:widgets.canvas.HighlightAltoCanvasPageLayer = new widgets.canvas.HighlightAltoCanvasPageLayer();
        private _model:model.StructureModel;
        private _chapterAreaContainer:ChapterAreaContainer = null;

        private selectedChapter:string = null;
        private highlightedChapter:string = null;

        constructor(private _settings:MetsSettings, private container:JQuery) {
            super();
        }

        public init():any {
            if (this._settings.doctype == 'mets') {
                this.trigger(new WaitForEvent(this, events.PageLayoutChangedEvent.TYPE));
                this.trigger(new WaitForEvent(this, events.RequestPageEvent.TYPE));
                this.trigger(new events.AddCanvasPageLayerEvent(this, 0, this.highlightLayer));
            }
        }

        public get handlesEvents():string[] {
            if (this._settings.doctype == 'mets') {
                return [ events.ChapterChangedEvent.TYPE,
                         events.PageLayoutChangedEvent.TYPE,
                         events.RequestPageEvent.TYPE,
                         events.MetsLoadedEvent.TYPE
                ];
            } else {
                return [];
            }
        }

        public getPageLayout():widgets.canvas.PageLayout {
            return this.pageLayout;
        }
 
        public getPageController():widgets.canvas.PageController {
            return this.pageLayout.getPageController();
        }

        public getChapterAreaContainer():ChapterAreaContainer {
            return this._chapterAreaContainer;
        }

        public setChapter(chapterId:string, triggerChapterChangeEvent:boolean = false, forceChange:boolean = false) {
            if(!forceChange && this.selectedChapter === chapterId) {
                return;
            }
            this.selectedChapter = chapterId;
            if(this._chapterAreaContainer === null) {
                return;
            }
            let chapterArea:ChapterArea = chapterId != null ? this._chapterAreaContainer.chapters.get(chapterId) : null;
            this.highlightLayer.selectedChapter = chapterArea;
            this.handleDarkenPageAnimation();
            this.trigger(new events.RedrawEvent(this));
            if(triggerChapterChangeEvent) {
                let chapter:model.StructureChapter = this._chapterAreaContainer.getChapter(chapterId);
                this.trigger(new events.ChapterChangedEvent(this, chapter));
            }
        }

        public setHighlightChapter(chapterId:string) {
            if(this._chapterAreaContainer === null || this.highlightedChapter === chapterId) {
                return;
            }
            let chapterArea:ChapterArea = chapterId != null ? this._chapterAreaContainer.chapters.get(chapterId) : null;
            this.highlightLayer.highlightedChapter = chapterArea;
            this.highlightedChapter = chapterId;
            if(this.selectedChapter == null) {
                this.handleDarkenPageAnimation();
            }
            this.trigger(new events.RedrawEvent(this));
        }

        public handleDarkenPageAnimation() {
            let selected:boolean = this.selectedChapter != null;
            let highlighted:boolean = this.highlightedChapter != null;
            let oldValue:number = 0;
            if(this.highlightLayer.fadeAnimation != null) {
                oldValue = this.highlightLayer.fadeAnimation.value;
                this.getPageController().removeAnimation(this.highlightLayer.fadeAnimation);
            }
            // fade out
            if(!selected && !highlighted) {
                if(oldValue == 0) {
                    return;
                }
                this.highlightLayer.fadeAnimation = new widgets.canvas.InterpolationAnimation(1000, oldValue, 0);
                this.getPageController().addAnimation(this.highlightLayer.fadeAnimation);
                return;
            }
            // fade in
            let alpha:number = selected ? 0.4 : 0.15;
            this.highlightLayer.fadeAnimation = new widgets.canvas.InterpolationAnimation(1000, oldValue, alpha);
            this.getPageController().addAnimation(this.highlightLayer.fadeAnimation);
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent) {
            if (e.type == events.MetsLoadedEvent.TYPE) {
                let mle = <events.MetsLoadedEvent> e;
                this._model = mle.mets.model;
                this._chapterAreaContainer = new ChapterAreaContainer(this._model);
                if(this.selectedChapter != null) {
                    this.setChapter(this.selectedChapter, false, true);
                }
            }
            if (e.type == events.RequestPageEvent.TYPE) {
                let rpe = <events.RequestPageEvent> e;
                this.trigger(new RequestAltoModelEvent(this, rpe._pageId, (page, altoHref, altoModel)=>{
                    this._chapterAreaContainer.addPage(rpe._pageId, altoHref, altoModel);
                } ));
            }
            if (e.type == events.ChapterChangedEvent.TYPE) {
                let cce = <events.ChapterChangedEvent>e;
                if (cce == null || cce.chapter == null) {
                    return;
                }
                this.setChapter(cce.chapter.id);
            }
            if( e.type == events.PageLayoutChangedEvent.TYPE ) {
                this.pageLayout = ( <events.PageLayoutChangedEvent>e ).pageLayout;
                this.trigger(new events.RequestDesktopInputEvent(this, new HighlightAltoInputListener(this)));
            }
        }

    }

    class HighlightAltoInputListener extends widgets.canvas.DesktopInputAdapter {

        constructor(protected component: MyCoReHighlightAltoComponent) {
            super();
        }

        public mouseClick(position:Position2D) {
            let chapterId:string = this.getChapterId(position);
            this.component.setChapter(chapterId, true);
        }

        public mouseMove(position:Position2D) {
            let chapterId:string = this.getChapterId(position);
            this.component.setHighlightChapter(chapterId);
        }

        private getChapterId(position:Position2D):string {
            let pageHitInfo:widgets.canvas.PageHitInfo = this.component.getPageLayout().getPageHitInfo(position);
            if(pageHitInfo.id == null) {
                return null;
            }
            let chapterAreaContainer:ChapterAreaContainer = this.component.getChapterAreaContainer();
            if(chapterAreaContainer === null) {
                return null;
            }
            let chapters:MyCoReMap<string, ChapterArea> = chapterAreaContainer.chapters;
            let pageChapterMap:MyCoReMap<string, Array<string>> = chapterAreaContainer.pageChapterMap;
    
            let chapterIdsOnPage:Array<string> = pageChapterMap.get(pageHitInfo.id);
            if(chapterIdsOnPage == null || chapterIdsOnPage.length <= 0) {
                return null;
            }
            for(let chapterId of chapterIdsOnPage) {
                let chapterArea:ChapterArea = chapters.get(chapterId);
                let rectsOfChapter:Array<Rect> = chapterArea.pages.get(pageHitInfo.id);
                if(rectsOfChapter == null) {
                    continue;
                }
                for(let rectOfChapter of rectsOfChapter) {
                    let rect:Rect = rectOfChapter.scale(pageHitInfo.pageAreaInformation.scale);
                    if(rect.intersects(pageHitInfo.hit)) {
                        return chapterId;
                    }
                }
            }
            return null;
        }

    }

    export class ChapterAreaContainer {

        public chapters: MyCoReMap<string, ChapterArea> = new MyCoReMap<string, ChapterArea>();
        public pageChapterMap:MyCoReMap<string, Array<string>> = new MyCoReMap<string, Array<string>>();

        private _loadedPages:any = {};

        constructor(private _model:model.StructureModel) {
            this._model.chapterToImageMap.keys.forEach(chapterId => {
                this.chapters.set(chapterId, new ChapterArea(chapterId));
            });
            let blocklistChapters:Array<model.StructureChapter> = this.getAllBlocklistChapters(this._model.rootChapter);
            this._model.imageList.forEach(image => {
                let chaptersOfPage:Array<string> = this.pageChapterMap.get(image.href)
                if(chaptersOfPage == null) {
                    chaptersOfPage = new Array<string>();
                    this.pageChapterMap.set(image.href, chaptersOfPage);
                }
                let altoHref:string = image.additionalHrefs.get("AltoHref");
                blocklistChapters.filter(chapter => {
                   let blocklist:Array<{fileId:string;fromId:string;toId:string}> = chapter.additional.get("blocklist");
                   for(let block of blocklist) {
                       if(block.fileId == altoHref) {
                           return true;
                       }
                   }
                   return false;
                }).forEach(chapter => {
                    chaptersOfPage.push(chapter.id);
                });
            });
        }

        getBlocklistOfChapter(chapterId:string):Array<{fileId:string;fromId:string;toId:string}> {
            let chapter:model.StructureChapter = this.getChapter(chapterId);
            if(chapter == null) {
                return;
            }
            return chapter.additional.get("blocklist");
        }

        getChapter(chapterId:string):model.StructureChapter {
            return this.findChapter(this._model.rootChapter, chapterId);
        }

        findChapter(from:model.StructureChapter, chapterId:string):model.StructureChapter {
            if(from.id == chapterId) {
                return from;
            }
            for(let childChapter of from.chapter) {
                let foundChapter = this.findChapter(childChapter, chapterId);
                if(foundChapter != null) {
                    return foundChapter;
                }
            }
            return null;
        }

        getBlocklistOfChapterAndAltoHref(chapterId:string, altoHref:string):Array<{fileId:string;fromId:string;toId:string}> {
            return this.getBlocklistOfChapter(chapterId).filter(({fileId, fromId, toId}) => {
                return fileId == altoHref;
            });
        }

        getAllBlocklistChapters(from:model.StructureChapter):Array<model.StructureChapter> {
            let chapters:Array<model.StructureChapter> = [];
            if(from.additional.get("blocklist") != null) {
                chapters.push(from);
            }
            from.chapter.forEach(childChapter => {
                chapters = chapters.concat(this.getAllBlocklistChapters(childChapter));
            });
            return chapters;
        }

        addPage(pageId:string, altoHref:string, alto:widgets.alto.AltoFile) {
            if(this._loadedPages[pageId] != null) {
                return;
            }
            this._loadedPages[pageId] = true;
            this.pageChapterMap.hasThen(pageId, (chapterIds:Array<string>) => {
                chapterIds.map(chapterId => this.chapters.get(chapterId)).forEach(chapter => {
                    chapter.addPage(pageId, alto, this.getBlocklistOfChapterAndAltoHref(chapter.chapterId, altoHref));
                });
            });
        }
    }

    export class ChapterArea {

        public pages: MyCoReMap<string, Array<Rect>> = new MyCoReMap<string, Array<Rect>>();

        constructor(public chapterId:string) {
        }

        public addPage(pageId:string, altoFile:widgets.alto.AltoFile,
                metsBlocklist:Array<{fileId:string;fromId:string;toId:string}>) {
            let altoBlocks:Array<widgets.alto.AltoElement> = this.getAltoBlocks(altoFile, metsBlocklist);
            let areas:Array<Rect> = this.getAreas(altoFile, altoBlocks);
            this.pages.set(pageId, areas);
        }

        private getAltoBlocks( altoFile: widgets.alto.AltoFile,
            metsBlocklist: Array<{ fileId: string; fromId: string; toId: string }> ): Array<widgets.alto.AltoElement> {
            let allBlocks = altoFile.allElements;
            let ids = allBlocks.map( block => block.getId() );
            let blocks: Array<widgets.alto.AltoElement> = [];
            metsBlocklist.map( blockFromTo => [ids.indexOf( blockFromTo.fromId ), ids.indexOf( blockFromTo.toId )] )
                .forEach(( [fromIndex, toIndex] ) => {
                    for ( let i = fromIndex; i <= toIndex; i++ ) {
                        let blockToHighlight = allBlocks[i];
                        if ( blockToHighlight == null ) {
                            continue;
                        }
                        blocks.push( blockToHighlight );
                    }
                }
                );
            return blocks;
        }

        private getAreas(altoFile: widgets.alto.AltoFile, blocks:Array<widgets.alto.AltoElement>):Array<Rect> {
            var areas:Array<Rect> = [];
            var area:Rect = null;
            var maxBottom:number = null;
            var maxRight:number = null;
            // added at the end to create nicer wider areas
            var padding:number = Math.ceil(altoFile.pageHeight * 0.004);
            // sometimes the blocks are not perfectly placed, use this
            // to be a bit more generous
            var blockFaultiness:number = Math.ceil(altoFile.pageHeight * 0.005);

            blocks.forEach(block => {
                var blockX:number = block.getBlockHPos();
                var blockY:number = block.getBlockVPos();
                var blockW:number = block.getWidth();
                var blockH:number = block.getHeight();
                // new area
                if(area == null) {
                    newArea()
                    return;
                }
                // check if next block should be assigned to the current area
                if(isAssignable()) {
                    area = area.maximize(blockX, blockY, blockW, blockH);
                    return;
                }
                // add a small padding
                area = area.increase(padding);
                // push to array
                areas.push(area);
                // new area with block
                newArea();
    
                function newArea() {
                    area = Rect.fromXYWH(blockX, blockY, blockW, blockH);
                    maxRight = area.getX() + area.getWidth();
                    maxBottom = area.getY() + area.getHeight();
                }
    
                function isAssignable() {
                    return (blockY >= maxBottom - blockFaultiness) && (blockX <= maxRight);
                }
    
            });
            if(area != null) {
                area = area.increase(padding); // add a small padding
                areas.push(area);
            }
            return areas;
        }
    }

}
addViewerComponent(mycore.viewer.components.MyCoReHighlightAltoComponent);
