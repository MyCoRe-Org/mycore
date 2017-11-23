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

/// <reference path="events/RequestAltoModelEvent.ts" />
/// <reference path="../widgets/XMLImageInformationProvider.ts" />
/// <reference path="../widgets/TileImagePage.ts" />
/// <reference path="../widgets/alto/AltoHTMLGenerator.ts" />
/// <reference path="MetsSettings.ts" />

namespace mycore.viewer.components {

    import RequestAltoModelEvent = mycore.viewer.components.events.RequestAltoModelEvent;
    import AltoHTMLGenerator = mycore.viewer.widgets.alto.AltoHTMLGenerator;
    import PageLoadedEvent = mycore.viewer.components.events.PageLoadedEvent;

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
        private _imageHTMLMap:MyCoReMap<string,HTMLElement> = new MyCoReMap<string, HTMLElement>();
        private _imageCallbackMap = new MyCoReMap<string, Array<(page: widgets.canvas.TileImagePage) => void>>();

        private getPage(image:string, resolve:(page:widgets.canvas.TileImagePage) => void) {
            if (this._imagePageMap.has(image)) {
                resolve(this._imagePageMap.get(image));
            } else {
                if (this._imageCallbackMap.has(image)) {
                    this._imageCallbackMap.get(image).push(resolve);
                } else {
                    let initialArray = new Array();
                    initialArray.push(resolve);
                    this._imageCallbackMap.set(image, initialArray);
                    this.getPageMetadata(image, (metadata) => {
                        let imagePage = this.createPageFromMetadata(image, metadata);
                        if (!this._imageHTMLMap.has(image)) {
                            this.trigger(new RequestAltoModelEvent(this, image, (page, altoHref, altoModel) => {
                                if (!this._imageHTMLMap.has(image)) {
                                    let htmlElement = this._altoHTMLGenerator.generateHtml(altoModel, altoHref);
                                    imagePage.getHTMLContent().value = htmlElement;
                                    this._imageHTMLMap.set(image, htmlElement);
                                }
                            }));
                        }
                        let resolveList = this._imageCallbackMap.get(image);
                        let pop;
                        while (pop = resolveList.pop()) {
                            pop(imagePage);
                        }
                        this._imagePageMap.set(image, imagePage);
                        this.trigger(new PageLoadedEvent(this,image,imagePage));
                    });
                }
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
                let rpe = <events.RequestPageEvent> e;

                this.getPage(rpe._pageId, (page:widgets.canvas.TileImagePage) => {
                    rpe._onResolve(rpe._pageId, page);
                });


            }


            return;
        }


    }

}

addViewerComponent(mycore.viewer.components.MyCoReMetsPageProviderComponent);
