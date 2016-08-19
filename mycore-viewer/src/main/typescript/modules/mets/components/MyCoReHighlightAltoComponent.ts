/// <reference path="MetsSettings.ts" />

module mycore.viewer.components {
    import RequestAltoModelEvent = mycore.viewer.components.events.RequestAltoModelEvent;
    import AreaInPage = mycore.viewer.widgets.canvas.AreaInPage;
    import WaitForEvent = mycore.viewer.components.events.WaitForEvent;
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
                this.marker.clearAll();
                if (cce.chapter.additional.has("blocklist")) {
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

                    fileIdsBlockListMap.keys.forEach(fileId=> {
                        this.trigger(new RequestAltoModelEvent(this, fileId, (imageHref, altoHref, altoFile:widgets.alto.AltoFile)=> {
                            var list = fileIdsBlockListMap.get(fileId);
                            var blocks = altoFile.allElements;
                            var ids = blocks.map(block=>block.getId());
                            list.map(blockFromTo=> [ ids.indexOf(blockFromTo.fromId), ids.indexOf(blockFromTo.toId) ])
                                .forEach(([fromIndex,toIndex])=> {
                                        for (var i = fromIndex; i <= toIndex; i++) {
                                            let blockToHighlight = blocks[ i ];
                                            if(blockToHighlight == null) {
                                                continue;
                                            }
                                            this.marker.markArea(new AreaInPage(imageHref, blockToHighlight.getBlockHPos(), blockToHighlight.getBlockVPos(), blockToHighlight.getWidth(), blockToHighlight.getHeight()));
                                        }
                                    }
                                );

                        }));
                    });


                }
            }


            if(e.type == events.ShowContentEvent.TYPE){
                let sce = (<events.ShowContentEvent>e);
                if(sce.containerDirection==events.ShowContentEvent.DIRECTION_WEST){
                    this.marker.clearAll();
                }

            }

            if (e.type == events.MarkerInitializedEvent.TYPE) {
                this.marker = (<events.MarkerInitializedEvent>e).marker;
            }
        }
    }
}
addViewerComponent(mycore.viewer.components.MyCoReHighlightAltoComponent);