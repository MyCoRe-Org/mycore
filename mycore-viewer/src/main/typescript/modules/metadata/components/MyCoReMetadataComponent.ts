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


import {MyCoReViewerSettings} from "../../base/MyCoReViewerSettings";
import {ViewerComponent} from "../../base/components/ViewerComponent";
import {singleSelectShim, ViewerFormatString, XMLUtil} from "../../base/Utils";
import {ShowContentEvent} from "../../base/components/events/ShowContentEvent";
import {MyCoReChapterComponent} from "../../base/components/MyCoReChapterComponent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {ComponentInitializedEvent} from "../../base/components/events/ComponentInitializedEvent";

export interface MetadataSettings extends MyCoReViewerSettings {
    objId: string;
    metadataURL: string;
}

export class MyCoReMetadataComponent extends ViewerComponent {

    constructor(private _settings: MetadataSettings) {
        super();
    }

    private _container: JQuery;
    private _spinner: JQuery = jQuery("<img />");
    private _enabled: boolean = true;

    public init() {
        this._container = jQuery("<div></div>");
        this._container.addClass("card-body");
        if (typeof this._settings.metadataURL != "undefined" && this._settings.metadataURL != null) {
            const metadataUrl = ViewerFormatString(this._settings.metadataURL, {
                derivateId: this._settings.derivate,
                objId: this._settings.objId
            });
            this._container.load(metadataUrl, {}, () => {
                this.correctScrollPosition();
            });
        } else if ("metsURL" in this._settings) {
            const xpath = "/mets:mets/*/mets:techMD/mets:mdWrap[@OTHERMDTYPE='MCRVIEWER_HTML']/mets:xmlData/*";
            const metsURL = (<any>this._settings).metsURL;
            const settings = {
                url: metsURL,
                success: (response) => {
                    let htmlElement:any = singleSelectShim(<any>response, xpath, XMLUtil.NS_MAP);
                    if (htmlElement != null) {
                        if ("xml" in htmlElement) {
                            // htmlElement is IXMLDOMElement
                            htmlElement = jQuery((<any>htmlElement).xml);
                        }
                        this._container.append(htmlElement);
                    } else {
                        this._container.remove();
                    }
                },
                error: (request, status, exception) => {
                    console.log(status);
                    console.error(exception);
                }
            };

            jQuery.ajax(settings);
        } else {
            this._enabled = false;
            return;
        }

        this.trigger(new ComponentInitializedEvent(this));
        this.trigger(new WaitForEvent(this, ShowContentEvent.TYPE));
    }

    private correctScrollPosition() {
        /*
         if the container is scrolled we want to restore the scroll position before this._container was inserted
         */
        if (this._container.parent().scrollTop() > 0) {
            const containerHeightDiff = this._container.height();
            const parent = this._container.parent();
            parent.scrollTop(parent.scrollTop() + containerHeightDiff);
        }
    }

    public handle(e: ViewerEvent): void {
        if (this._enabled && e.type == ShowContentEvent.TYPE) {
            const sce = e as ShowContentEvent;
            if (sce.component instanceof MyCoReChapterComponent) {
                sce.content.prepend(this._container);
            }
        }
    }

    public get handlesEvents(): string[] {
        return [ /*events.ProvideToolbarModelEvent.TYPE,*/ ShowContentEvent.TYPE /*widgets.toolbar.events.DropdownButtonPressedEvent.TYPE*/];
    }
}


