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
import {MyCoReMap} from "../../base/Utils";
import {StructureModel} from "../../base/components/model/StructureModel";
import {RequestAltoModelEvent} from "./events/RequestAltoModelEvent";
import {AltoFile} from "../widgets/alto/AltoFile";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {StructureModelLoadedEvent} from "../../base/components/events/StructureModelLoadedEvent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";
import {RequestTextContentEvent} from "../../base/components/events/RequestTextContentEvent";


export class MyCoReAltoModelProvider extends ViewerComponent {

    constructor(private _settings: MetsSettings) {
        super();
    }

    private structureModel: StructureModel = null;
    private pageHrefAltoHrefMap = new MyCoReMap<string, string>();
    private altoHrefPageHrefMap = new MyCoReMap<string, string>();

    private altoModelRequestTempStore = new Array<RequestAltoModelEvent>();
    private static altoHrefModelMap = new MyCoReMap<string, AltoFile>();
    private static TEXT_HREF = "AltoHref";

    public init() {
        if (this._settings.doctype == "mets") {
            this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));
            this.trigger(new WaitForEvent(this, RequestAltoModelEvent.TYPE));
        }
    }


    public handle(e: ViewerEvent): void {
        if (e.type == RequestAltoModelEvent.TYPE) {
            if (this.structureModel == null || this.structureModel._textContentPresent) {
                const rtce = e as RequestAltoModelEvent;
                let {altoHref, imgHref} = this.detectHrefs(rtce._href);
                if (this.pageHrefAltoHrefMap.has(imgHref)) {
                    this.resolveAltoModel(imgHref, (mdl) => {
                        rtce._onResolve(imgHref, altoHref, mdl);
                    });
                } else if (this.structureModel == null) {
                    this.altoModelRequestTempStore.push(rtce);
                }
            }
            return;
        }

        if (e.type == StructureModelLoadedEvent.TYPE) {
            const smle = e as StructureModelLoadedEvent;
            this.structureModel = smle.structureModel;
            if (smle.structureModel._textContentPresent) {
                this.fillAltoHrefMap();
                for (const rtce of this.altoModelRequestTempStore) {
                    const {altoHref, imgHref} = this.detectHrefs(rtce._href);
                    ((altoHref, imgHref, cb) => {
                        if (this.pageHrefAltoHrefMap.has(imgHref)) {
                            this.resolveAltoModel(imgHref, (mdl) => {
                                cb(imgHref, altoHref, mdl);
                            });
                        } else {
                            console.warn("RPE : altoHref not found!");
                        }
                    })(altoHref, imgHref, rtce._onResolve)
                }
                this.altoModelRequestTempStore = [];
                this.trigger(new WaitForEvent(this, RequestTextContentEvent.TYPE));
            }
            return;
        }

        return;
    }

    private detectHrefs(href: string) {
        let altoHref, imgHref;
        if (this.altoHrefPageHrefMap.has(href)) {
            altoHref = href;
            imgHref = this.altoHrefPageHrefMap.get(altoHref);
        } else {
            imgHref = href;
            altoHref = this.pageHrefAltoHrefMap.get(imgHref);
        }
        return {altoHref: altoHref, imgHref: imgHref};
    }

    private fillAltoHrefMap() {
        this.structureModel.imageList.forEach((image) => {
            const hasTextHref = image.additionalHrefs.has(MyCoReAltoModelProvider.TEXT_HREF);
            if (hasTextHref) {
                const altoHref = image.additionalHrefs.get(MyCoReAltoModelProvider.TEXT_HREF);
                this.pageHrefAltoHrefMap.set(image.href, altoHref);
                this.altoHrefPageHrefMap.set(altoHref, image.href);
            }
        });
    }


    public get handlesEvents(): string[] {
        if (this._settings.doctype == "mets") {
            return [RequestAltoModelEvent.TYPE, StructureModelLoadedEvent.TYPE];
        } else {
            return [];
        }
    }

    private resolveAltoModel(pageId: string, callback: (content: AltoFile) => void): void {
        const altoHref = this.pageHrefAltoHrefMap.get(pageId);
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

    public loadAltoXML(altoPath: string, successCallback: any, errorCallback: any): void {
        const requestObj: any = {
            url: altoPath,
            type: "GET",
            dataType: "xml",
            async: true,
            success: successCallback,
            error: errorCallback
        };
        jQuery.ajax(requestObj);
    }

    public loadedAltoModel(parentId: string,
                           altoHref: string,
                           xml: any,
                           callback: (altoContainer: AltoFile) => void): void {

        const pageStyles: NodeListOf<HTMLAreaElement> = xml.getElementsByTagName("Styles");
        const styles: Element = pageStyles.item(0);

        const layouts: NodeListOf<HTMLAreaElement> = xml.getElementsByTagName("Layout");
        const layout: Element = layouts.item(0);

        if (layout != null) {
            const altoContainer = new AltoFile(styles, layout);
            MyCoReAltoModelProvider.altoHrefModelMap.set(altoHref, altoContainer);
            callback(altoContainer);
        }
    }

}


