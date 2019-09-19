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

        constructor(private settings: IIIFSettings) {
            super();
        }

        public init() {
            if (this.settings.doctype === 'manifest') {
                this.trigger(new events.WaitForEvent(this, events.RequestPageEvent.TYPE));
            }
        }

        private vImageInformationMap: MyCoReMap<string, widgets.image.IIIFImageInformation>
            = new MyCoReMap<string, widgets.image.IIIFImageInformation>();
        private vImagePageMap: MyCoReMap<string, widgets.canvas.TileImagePageIIIF>
            = new MyCoReMap<string, widgets.canvas.TileImagePageIIIF>();
        private vImageHTMLMap: MyCoReMap<string, HTMLElement> = new MyCoReMap<string, HTMLElement>();
        private vImageCallbackMap: MyCoReMap<string, ((page: widgets.canvas.TileImagePageIIIF) => void)[]>
            = new MyCoReMap<string, ((page: widgets.canvas.TileImagePageIIIF) => void)[]>();

        private getPage(image: string, resolve: (page: widgets.canvas.TileImagePageIIIF) => void) {
            if (this.vImagePageMap.has(image)) {
                resolve(this.vImagePageMap.get(image));
            } else {
                if (this.vImageCallbackMap.has(image)) {
                    this.vImageCallbackMap.get(image).push(resolve);
                } else {
                    const initialArray = [];
                    initialArray.push(resolve);
                    this.vImageCallbackMap.set(image, initialArray);
                    this.getPageMetadata(image, (metadata) => {
                        const imagePage = this.createPageFromMetadata(image, metadata);
                        let resolveList = this.vImageCallbackMap.get(image);
                        let pop;
                        while (pop = resolveList.pop()) {
                            pop(imagePage);
                        }
                        this.vImagePageMap.set(image, imagePage);
                        this.trigger(new PageLoadedEvent(this, image, imagePage));
                    });
                }
            }
        }

        private createPageFromMetadata(imageId: string,
                                       metadata: widgets.image.IIIFImageInformation): widgets.canvas.TileImagePageIIIF {
            const paths: string[] = [];
            paths.push(metadata.path + '/{x},{y},{w},{h}/!{tx},{ty}/0/default.jpg');

            return new widgets.canvas.TileImagePageIIIF(imageId, metadata.width, metadata.height, paths);
        }

        private getPageMetadata(image: string, resolve: (metadata: widgets.image.IIIFImageInformation) => void) {
            image = (image.charAt(0) === '/') ? image.substr(1) : image;

            if (this.vImageInformationMap.has(image)) {
                resolve(this.vImageInformationMap.get(image));
            } else {
                let path = image;
                if (path.indexOf(this.settings.derivate) === -1) {
                    path = this.settings.imageAPIURL + this.settings.derivate + '%2F' + path;
                }
                mycore.viewer.widgets.image.IIIFImageInformationProvider.getInformation(path,
                    (info: widgets.image.IIIFImageInformation) => {
                        this.vImageInformationMap.set(image, info);
                        resolve(info);
                    }, (error: any) => {
                        console.log('Error while loading ImageInformations', + error.toString());
                    });
            }
        }

        public get handlesEvents(): string[] {
            if (this.settings.doctype === 'manifest') {
                return [ events.RequestPageEvent.TYPE ];
            }else {
                return [];
            }
        }

        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (e.type === events.RequestPageEvent.TYPE) {
                const rpe = <events.RequestPageEvent> e;

                this.getPage(rpe._pageId, (page: widgets.canvas.TileImagePageIIIF) => {
                    rpe._onResolve(rpe._pageId, page);
                });

            }

            return;
        }

    }

}

addViewerComponent(mycore.viewer.components.MyCoReIIIFPageProviderComponent);
