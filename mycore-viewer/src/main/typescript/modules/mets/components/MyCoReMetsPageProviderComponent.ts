/// <reference path="events/RequestAltoModelEvent.ts" />
/// <reference path="../widgets/XMLImageInformationProvider.ts" />
/// <reference path="../widgets/TileImagePage.ts" />
/// <reference path="../widgets/alto/AltoHTMLGenerator.ts" />
/// <reference path="MetsSettings.ts" />

namespace mycore.viewer.components {

    import RequestAltoModelEvent = mycore.viewer.components.events.RequestAltoModelEvent;
    import AltoHTMLGenerator = mycore.viewer.widgets.alto.AltoHTMLGenerator;

    export class MyCoReMetsPageProviderComponent extends ViewerComponent {

        constructor(private _settings:MetsSettings) {
            super();
        }

        public init() {
            if (this._settings.doctype == 'mets') {
                this.trigger(new events.WaitForEvent(this, events.RequestPageEvent.TYPE));
            }
        }

        private _imageInformationMap:MyCoReMap<string, widgets.image.XMLImageInformation> = new MyCoReMap<string, widgets.image.XMLImageInformation>();
        private _imagePageMap:MyCoReMap<string, widgets.canvas.TileImagePage> = new MyCoReMap<string, widgets.canvas.TileImagePage>();
        private _altoHTMLGenerator = new AltoHTMLGenerator();

        private getPage(image:string, resolve:(page:widgets.canvas.TileImagePage) => void) {
            if (this._imagePageMap.has(image)) {
                resolve(this.createPageFromMetadata(image, this._imageInformationMap.get(image)));
            } else {
                this.getPageMetadata(image, (metadata) => {
                    resolve(this.createPageFromMetadata(image, metadata));
                });
            }
        }

        private createPageFromMetadata(imageId:string, metadata:widgets.image.XMLImageInformation):widgets.canvas.TileImagePage {
            var tiles = this._settings.tileProviderPath.split(",");
            var paths = new Array<string>();

            tiles.forEach((path:string) => {
                paths.push(path + this._settings.derivate + metadata.path + "/{z}/{y}/{x}.jpg");
            });

            return new widgets.canvas.TileImagePage(imageId, metadata.width, metadata.height, paths);
        }

        private getPageMetadata(image:string, resolve:(metadata:widgets.image.XMLImageInformation) => void) {
            image = (image.charAt(0) == "/") ? image.substr(1) : image;

            if (this._imageInformationMap.has(image)) {
                resolve(this._imageInformationMap.get(image));
            } else {
                var path = "/" + image;
                mycore.viewer.widgets.image.XMLImageInformationProvider.getInformation(this._settings.imageXmlPath + this._settings.derivate, path,
                    (info:widgets.image.XMLImageInformation) => {
                        this._imageInformationMap.set(image, info);
                        resolve(info);
                    }, function (error) {
                        console.log("Error while loading ImageInformations", +error.toString());
                    });
            }
        }


        public get handlesEvents():string[] {
            if (this._settings.doctype == 'mets') {
                return [ events.RequestPageEvent.TYPE ];
            } else { 
                return [];
            }
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == events.RequestPageEvent.TYPE) {
                var rpe = <events.RequestPageEvent> e;

                var pageAltoSynchronize = Utils.synchronize<{
                    page:widgets.canvas.TileImagePage; 
                    altoModel:widgets.alto.AltoFile;
                }>(
                    [
                        (synchronizeObj)=>synchronizeObj.page != null,
                        (synchronizeObj)=>synchronizeObj.altoModel != null,
                    ],
                    (synchronizeObj)=> {
                        var htmlElement = this._altoHTMLGenerator.generateHtml(synchronizeObj.altoModel);
                        synchronizeObj.page.setAltoContent(htmlElement);
                    }
                );

                var synchronizeObj = {page : null, altoModel : null};

                this.getPage(rpe._pageId, (page:widgets.canvas.TileImagePage) => {
                    synchronizeObj.page = page;
                    pageAltoSynchronize(synchronizeObj);
                    rpe._onResolve(rpe._pageId, page);
                });

                this.trigger(new RequestAltoModelEvent(this, rpe._pageId, (page, altoHref,altoModel)=>{
                    synchronizeObj.altoModel = altoModel;
                    pageAltoSynchronize(synchronizeObj);
                } ));

            }


            return;
        }


    }

}

addViewerComponent(mycore.viewer.components.MyCoReMetsPageProviderComponent);