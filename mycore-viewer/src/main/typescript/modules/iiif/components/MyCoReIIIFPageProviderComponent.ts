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

/// <reference path="../widgets/IIIFImageInformationProvider.ts" />
/// <reference path="../widgets/TileImagePageIIIF.ts" />
/// <reference path="IIIFSettings.ts" />

namespace mycore.viewer.components {

    import PageLoadedEvent = mycore.viewer.components.events.PageLoadedEvent;

    export class MyCoReIIIFPageProviderComponent extends ViewerComponent {

        constructor(private _settings:IIIFSettings) {
            super();
        }

        public init() {
            if (this._settings.doctype == 'manifest') {
                this.trigger(new events.WaitForEvent(this, events.RequestPageEvent.TYPE));
            }
        }

        private _imageInformationMap:MyCoReMap<string, widgets.image.IIIFImageInformation> = new MyCoReMap<string, widgets.image.IIIFImageInformation>();
        private _imagePageMap:MyCoReMap<string, widgets.canvas.TileImagePageIIIF> = new MyCoReMap<string, widgets.canvas.TileImagePageIIIF>();
        private _imageHTMLMap:MyCoReMap<string,HTMLElement> = new MyCoReMap<string, HTMLElement>();
        private _imageCallbackMap = new MyCoReMap<string, Array<(page: widgets.canvas.TileImagePageIIIF) => void>>();

        private getPage(image:string, resolve:(page:widgets.canvas.TileImagePageIIIF) => void) {
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

        private createPageFromMetadata(imageId:string, metadata:widgets.image.IIIFImageInformation):widgets.canvas.TileImagePageIIIF {
            var paths = new Array<string>();
            paths.push(metadata.path + "/{x},{y},{w},{h}/!{tx},{ty}/0/default.jpg");

            return new widgets.canvas.TileImagePageIIIF(imageId, metadata.width, metadata.height, paths);
        }

        private getPageMetadata(image:string, resolve:(metadata:widgets.image.IIIFImageInformation) => void) {
            image = (image.charAt(0) == "/") ? image.substr(1) : image;

            if (this._imageInformationMap.has(image)) {
                resolve(this._imageInformationMap.get(image));
            } else {
                var path = image;
                if (path.indexOf(this._settings.derivate) == -1) {
                    path = this._settings.imageAPIURL + this._settings.derivate + "%2F" + path;
                }
                mycore.viewer.widgets.image.IIIFImageInformationProvider.getInformation(path,
                    (info:widgets.image.IIIFImageInformation) => {
                        this._imageInformationMap.set(image, info);
                        resolve(info);
                    }, function (error) {
                        console.log("Error while loading ImageInformations", +error.toString());
                    });
            }
        }


        public get handlesEvents():string[] {
            if (this._settings.doctype == 'manifest') {
                return [ events.RequestPageEvent.TYPE ];
            } else { 
                return [];
            }
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == events.RequestPageEvent.TYPE) {
                let rpe = <events.RequestPageEvent> e;

                this.getPage(rpe._pageId, (page:widgets.canvas.TileImagePageIIIF) => {
                    rpe._onResolve(rpe._pageId, page);
                });


            }


            return;
        }


    }

}

addViewerComponent(mycore.viewer.components.MyCoReIIIFPageProviderComponent);
