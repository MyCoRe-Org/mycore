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

import {ViewerComponent} from "../../base/components/ViewerComponent";
import {MetsSettings} from "./MetsSettings";
import {RequestPageEvent} from "../../base/components/events/RequestPageEvent";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {MyCoReMap} from "../../base/Utils";
import {XMLImageInformation, XMLImageInformationProvider} from "../widgets/XMLImageInformationProvider";
import {TileImagePage} from "../../base/widgets/canvas/TileImagePage";
import {AltoHTMLGenerator} from "../widgets/alto/AltoHTMLGenerator";
import {RequestAltoModelEvent} from "./events/RequestAltoModelEvent";
import {PageLoadedEvent} from "../../base/components/events/PageLoadedEvent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";

export class MyCoReMetsPageProviderComponent extends ViewerComponent {

    constructor(private settings: MetsSettings) {
        super();
    }

    public init() {
        if (this.settings.doctype === 'mets') {
            this.trigger(new WaitForEvent(this, RequestPageEvent.TYPE));
        }
    }

    private vImageInformationMap: MyCoReMap<string, XMLImageInformation>
        = new MyCoReMap<string, XMLImageInformation>();
    private vImagePageMap: MyCoReMap<string, TileImagePage>
        = new MyCoReMap<string, TileImagePage>();
    private vAltoHTMLGenerator: AltoHTMLGenerator = new AltoHTMLGenerator();
    private vImageHTMLMap: MyCoReMap<string, HTMLElement> = new MyCoReMap<string, HTMLElement>();
    private vImageCallbackMap: MyCoReMap<string, ((page: TileImagePage) => void)[]>
        = new MyCoReMap<string, ((page: TileImagePage) => void)[]>();

    private getPage(image: string, resolve: (page: TileImagePage) => void) {
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
                    if (!this.vImageHTMLMap.has(image)) {
                        this.trigger(new RequestAltoModelEvent(this, image, (page, altoHref, altoModel) => {
                            if (!this.vImageHTMLMap.has(image)) {
                                let htmlElement = this.vAltoHTMLGenerator.generateHtml(altoModel, altoHref);
                                imagePage.getHTMLContent().value = htmlElement;
                                this.vImageHTMLMap.set(image, htmlElement);
                            }
                        }));
                    }
                    const resolveList = this.vImageCallbackMap.get(image);
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
                                   metadata: XMLImageInformation): TileImagePage {
        const tiles = this.settings.tileProviderPath.split(',');
        const paths = new Array<string>();

        tiles.forEach((path: string) => {
            paths.push(path + this.settings.derivate + metadata.path + '/{z}/{y}/{x}.jpg');
        });

        return new TileImagePage(imageId, metadata.width, metadata.height, paths);
    }

    private getPageMetadata(image: string, resolve: (metadata: XMLImageInformation) => void) {
        image = (image.charAt(0) === '/') ? image.substr(1) : image;

        if (this.vImageInformationMap.has(image)) {
            resolve(this.vImageInformationMap.get(image));
        } else {
            const path = '/' + image;
            XMLImageInformationProvider.getInformation(this.settings.imageXmlPath
                + this.settings.derivate, path,
                (info: XMLImageInformation) => {
                    this.vImageInformationMap.set(image, info);
                    resolve(info);
                }, (error: any) => {
                    console.log('Error while loading ImageInformations', +error.toString());
                });
        }
    }

    public get handlesEvents(): string[] {
        if (this.settings.doctype === 'mets') {
            return [RequestPageEvent.TYPE];
        } else {
            return [];
        }
    }

    public handle(e: ViewerEvent): void {
        if (e.type === RequestPageEvent.TYPE) {
            const rpe = e as RequestPageEvent;

            this.getPage(rpe._pageId, (page: TileImagePage) => {
                rpe._onResolve(rpe._pageId, page);
            });

        }

        return;
    }

}



