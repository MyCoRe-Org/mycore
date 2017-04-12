/// <reference path="MetsSettings.ts" />

module mycore.viewer.components {
    import RequestAltoModelEvent = mycore.viewer.components.events.RequestAltoModelEvent;
    import AreaInPage = mycore.viewer.widgets.canvas.AreaInPage;
    import WaitForEvent = mycore.viewer.components.events.WaitForEvent;
    import CanvasMarkerType = mycore.viewer.widgets.canvas.CanvasMarkerType;

    export class MyCoReHighlightAltoComponent extends ViewerComponent {
        private marker:widgets.canvas.CanvasMarker = null;

        constructor(private _settings:MetsSettings, private container:JQuery) {
            super();
        }


        public init():any {
            this.trigger(new WaitForEvent(this, events.MarkerInitializedEvent.TYPE));
        }

        public get handlesEvents():string[] {
            if (this._settings.doctype == 'mets') {
                return [ events.ChapterChangedEvent.TYPE, events.MarkerInitializedEvent.TYPE, events.ShowContentEvent.TYPE ];
            } else {
                return [];
            }
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent) {
            if (e.type == events.ChapterChangedEvent.TYPE) {
                let cce = <events.ChapterChangedEvent>e;
                this.marker.clearAll(CanvasMarkerType.AREA);
                if (cce && cce.chapter && cce.chapter.additional.has("blocklist")) {
                    let blocklist:Array<{
                        fileId:string,
                        fromId:string,
                        toId:string,
                    }> = cce.chapter.additional.get("blocklist");

                    let fileIdsBlockListMap = new MyCoReMap<string, Array<{
                        fileId:string,
                        fromId:string,
                        toId:string,
                    }>>();
                    blocklist.forEach(entry=> {
                        let blockList;
                        if (!fileIdsBlockListMap.has(entry.fileId)) {
                            fileIdsBlockListMap.set(entry.fileId, blockList = new Array());
                        } else {
                            blockList = fileIdsBlockListMap.get(entry.fileId);
                        }

                        blockList.push(entry);
                    });

                    fileIdsBlockListMap.keys.forEach( fileId => {
                        this.trigger( new RequestAltoModelEvent( this, fileId, ( imageHref, altoHref, altoFile: widgets.alto.AltoFile ) => {
                            var list = fileIdsBlockListMap.get( fileId );
                            var allBlocks = altoFile.allElements;
                            var ids = allBlocks.map( block => block.getId() );
                            // add all blocks which are required to highlight
                            var blocks = [];
                            list.map( blockFromTo => [ids.indexOf( blockFromTo.fromId ), ids.indexOf( blockFromTo.toId )] )
                                .forEach(( [fromIndex, toIndex] ) => {
                                    for ( var i = fromIndex; i <= toIndex; i++ ) {
                                        let blockToHighlight = allBlocks[i];
                                        if ( blockToHighlight == null ) {
                                            continue;
                                        }
                                        blocks.push(blockToHighlight);
                                    }
                                }
                                );
                            // bundle blocks in areas for better highlight quality
                            var areas:Array<any> = [];
                            var area:AreaInPage = null;
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
                                    area.maximize(blockX, blockY, blockW, blockH);
                                    return;
                                }
                                // mark area
                                area.increase(padding); // add a small padding
                                this.marker.markArea( jQuery.extend(false, {}, area) );
                                // new area with block
                                newArea();

                                function newArea() {
                                    area = new AreaInPage(imageHref, blockX, blockY, blockW, blockH, CanvasMarkerType.AREA);
                                    maxRight = area.x + area.width;
                                    maxBottom = area.y + area.height;
                                }

                                function isAssignable() {
                                    return (blockY >= maxBottom - blockFaultiness) && (blockX <= maxRight);
                                }

                            });
                            if(area != null) {
                                area.increase(padding); // add a small padding
                                this.marker.markArea( jQuery.extend(false, {}, area) );
                            }
                        }) );
                    });


                }
            }


            if(e.type == events.ShowContentEvent.TYPE){
                let sce = (<events.ShowContentEvent>e);
                if(sce.containerDirection==events.ShowContentEvent.DIRECTION_WEST){
                    this.marker.clearAll(CanvasMarkerType.AREA);
                }

            }

            if (e.type == events.MarkerInitializedEvent.TYPE) {
                this.marker = (<events.MarkerInitializedEvent>e).marker;
            }
        }
    }
}
addViewerComponent(mycore.viewer.components.MyCoReHighlightAltoComponent);
