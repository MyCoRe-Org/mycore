/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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


import {ViewerComponent} from "../../base/components/ViewerComponent";
import {IIIFSettings} from "./IIIFSettings";
import {MyCoReMap} from "../../base/Utils";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {RequestPageEvent} from "../../base/components/events/RequestPageEvent";
import {IIIFImageInformation, IIIFImageInformationProvider} from "../widgets/IIIFImageInformationProvider";
import {TileImagePageIIIF} from "../widgets/TileImagePageIIIF";
import {PageLoadedEvent} from "../../base/components/events/PageLoadedEvent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";

export class MyCoReIIIFPageProviderComponent extends ViewerComponent {

    constructor(private settings: IIIFSettings) {
        super();
    }

    public init() {
        if (this.settings.doctype === 'manifest') {
            this.trigger(new WaitForEvent(this, RequestPageEvent.TYPE));
        }
    }

    private vImageInformationMap: MyCoReMap<string, IIIFImageInformation>
        = new MyCoReMap<string, IIIFImageInformation>();
    private vImagePageMap: MyCoReMap<string, TileImagePageIIIF>
        = new MyCoReMap<string, TileImagePageIIIF>();
    private vImageHTMLMap: MyCoReMap<string, HTMLElement> = new MyCoReMap<string, HTMLElement>();
    private vImageCallbackMap: MyCoReMap<string, ((page: TileImagePageIIIF) => void)[]>
        = new MyCoReMap<string, ((page: TileImagePageIIIF) => void)[]>();

    private getPage(image: string, resolve: (page: TileImagePageIIIF) => void) {
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
                                   metadata: IIIFImageInformation): TileImagePageIIIF {
        const paths: string[] = [];
        paths.push(metadata.path + '/{x},{y},{w},{h}/!{tx},{ty}/0/default.jpg');

        return new TileImagePageIIIF(imageId, metadata.width, metadata.height, paths);
    }

    private getPageMetadata(image: string, resolve: (metadata: IIIFImageInformation) => void) {
        image = (image.charAt(0) === '/') ? image.substr(1) : image;

        if (this.vImageInformationMap.has(image)) {
            resolve(this.vImageInformationMap.get(image));
        } else {
            let path = this.settings.imageAPIURL + image;
            IIIFImageInformationProvider.getInformation(path,
                (info: IIIFImageInformation) => {
                    this.vImageInformationMap.set(image, info);
                    resolve(info);
                }, (error: any) => {
                    console.log('Error while loading ImageInformations', +error.toString());
                });
        }
    }

    public get handlesEvents(): string[] {
        if (this.settings.doctype === 'manifest') {
            return [RequestPageEvent.TYPE];
        } else {
            return [];
        }
    }

    public handle(e: ViewerEvent): void {
        if (e.type === RequestPageEvent.TYPE) {
            const rpe = e as RequestPageEvent;

            this.getPage(rpe._pageId, (page: TileImagePageIIIF) => {
                rpe._onResolve(rpe._pageId, page);
            });

        }

        return;
    }

}


