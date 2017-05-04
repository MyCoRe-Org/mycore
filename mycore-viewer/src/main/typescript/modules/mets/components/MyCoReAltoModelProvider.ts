/// <reference path="../widgets/XMLImageInformationProvider.ts" />
/// <reference path="../widgets/TileImagePage.ts" />
/// <reference path="MetsSettings.ts" />
/// <reference path="../widgets/alto/AltoFile.ts" />
/// <reference path="events/RequestAltoModelEvent.ts" />

namespace mycore.viewer.components {

    export class MyCoReAltoModelProvider extends ViewerComponent {

        constructor(private _settings:MetsSettings) {
            super();
        }

        private structureModel:model.StructureModel = null;
        private pageHrefAltoHrefMap = new MyCoReMap<string, string>();
        private altoHrefPageHrefMap = new MyCoReMap<string, string>();

        private altoModelRequestTempStore = new Array<events.RequestAltoModelEvent>();
        private static altoHrefModelMap = new MyCoReMap<string, widgets.alto.AltoFile>();
        private static TEXT_HREF = "AltoHref";

        public init() {
            if (this._settings.doctype == "mets") {
                this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
                this.trigger(new events.WaitForEvent(this, events.RequestAltoModelEvent.TYPE));
            }
        }


        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == events.RequestAltoModelEvent.TYPE) {
                if (this.structureModel == null || this.structureModel._textContentPresent) {
                    var rtce = <events.RequestAltoModelEvent>e;
                    let {altoHref, imgHref} = this.detectHrefs(rtce._href);
                    if (this.pageHrefAltoHrefMap.has(imgHref)) {
                        this.resolveAltoModel(imgHref, (mdl)=> {
                            rtce._onResolve(imgHref, altoHref, mdl);
                        });
                    } else if(this.structureModel == null) {
                        this.altoModelRequestTempStore.push(rtce);
                    }
                }
            }

            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var smle = <events.StructureModelLoadedEvent>e;
                this.structureModel = smle.structureModel;
                if (smle.structureModel._textContentPresent) {
                    this.fillAltoHrefMap();
                    for(var rtceIndex in this.altoModelRequestTempStore) {
                        var rtce = this.altoModelRequestTempStore[rtceIndex];
                        var {altoHref, imgHref} = this.detectHrefs(rtce._href);
                        ((altoHref, imgHref, cb)=>{
                            if (this.pageHrefAltoHrefMap.has(imgHref)) {
                                this.resolveAltoModel(imgHref, (mdl)=> {
                                    cb(imgHref, altoHref, mdl);
                                });
                            } else {
                                console.warn("RPE : altoHref not found!");
                            }
                        })(altoHref, imgHref, rtce._onResolve)
                    }
                    this.altoModelRequestTempStore = [];
                    this.trigger(new events.WaitForEvent(this, events.RequestTextContentEvent.TYPE));
                }
            }

            return;
        }

        private detectHrefs(href:string) {
            let altoHref, imgHref;
            if (this.altoHrefPageHrefMap.has(href)) {
                altoHref = href;
                imgHref = this.altoHrefPageHrefMap.get(altoHref);
            } else {
                imgHref = href;
                altoHref = this.pageHrefAltoHrefMap.get(imgHref);
            }
            return {altoHref : altoHref, imgHref : imgHref};
        }

        private fillAltoHrefMap() {
            this.structureModel.imageList.forEach((image)=> {
                var hasTextHref = image.additionalHrefs.has(MyCoReAltoModelProvider.TEXT_HREF);
                if (hasTextHref) {
                    var altoHref = image.additionalHrefs.get(MyCoReAltoModelProvider.TEXT_HREF);
                    this.pageHrefAltoHrefMap.set(image.href, altoHref);
                    this.altoHrefPageHrefMap.set(altoHref, image.href);
                }
            });
        }


        public get handlesEvents():string[] {
            if (this._settings.doctype == "mets") {
                return [ events.RequestAltoModelEvent.TYPE, events.StructureModelLoadedEvent.TYPE ];
            } else {
                return [];
            }
        }

        private resolveAltoModel(pageId:string, callback:(content:widgets.alto.AltoFile)=>void):void {
            var altoHref = this.pageHrefAltoHrefMap.get(pageId);
            if (MyCoReAltoModelProvider.altoHrefModelMap.has(altoHref)) {
                callback(MyCoReAltoModelProvider.altoHrefModelMap.get(altoHref));
            } else {
                this.loadAltoXML(this._settings.derivateURL + altoHref, (result) => {
                    this.loadedAltoModel(pageId, altoHref, result, callback);
                }, (e) => {
                    console.error("Failed to receive alto from server... ", e);
                });
            }
        }

        public loadAltoXML(altoPath:string, successCallback:any, errorCallback:any):void {
            var requestObj:any = {
                url : altoPath,
                type : "GET",
                dataType : "xml",
                async : true,
                success : successCallback,
                error : errorCallback
            };
            jQuery.ajax(requestObj);
        }

        public loadedAltoModel(parentId:string,
                               altoHref:string,
                               xml:any,
                               callback:(altoContainer:widgets.alto.AltoFile)=>void):void {

            var pageStyles:NodeListOf<HTMLAreaElement> = xml.getElementsByTagName("Styles");
            var styles:Element = pageStyles.item(0);

            var layouts:NodeListOf<HTMLAreaElement> = xml.getElementsByTagName("Layout");
            var layout:Element = layouts.item(0);

            if (styles != null && layout != null) {
                var altoContainer = new widgets.alto.AltoFile(styles, layout);
                MyCoReAltoModelProvider.altoHrefModelMap.set(altoHref, altoContainer);
                callback(altoContainer);
            }
        }

    }
}

addViewerComponent(mycore.viewer.components.MyCoReAltoModelProvider);