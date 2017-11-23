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

namespace mycore.viewer.components {

    export interface MetadataSettings extends MyCoReViewerSettings {
        objId: string;
        metadataURL: string;
    }

    export class MyCoReMetadataComponent extends ViewerComponent {

        constructor(private _settings:MetadataSettings) {
            super();
        }

        private _container:JQuery;
        private _spinner:JQuery = jQuery("<img />");
        private _enabled:boolean = true;

        public init() {
            this._container = jQuery("<div></div>");
            this._container.addClass("panel-body");
            if (typeof this._settings.metadataURL != "undefined" && this._settings.metadataURL != null) {
                var metadataUrl = ViewerFormatString(this._settings.metadataURL, {
                    derivateId : this._settings.derivate,
                    objId : this._settings.objId
                });
                this._container.load(metadataUrl, {}, ()=> {
                    this.correctScrollPosition();
                });
            } else if ("metsURL" in this._settings) {
                var xpath = "/mets:mets/*/mets:techMD/mets:mdWrap[@OTHERMDTYPE='MCRVIEWER_HTML']/mets:xmlData/*";
                var metsURL = (<any>this._settings).metsURL;
                var settings = {
                    url : metsURL,
                    success : (response) => {
                        var htmlElement = <any>singleSelectShim(<any>response, xpath, XMLUtil.NS_MAP);
                        if (htmlElement != null) {
                            if("xml" in htmlElement){
                                // htmlElement is IXMLDOMElement
                                htmlElement = jQuery((<any> htmlElement).xml);
                            }
                            this._container.append(htmlElement);
                        } else {
                            this._container.remove();
                        }
                    },
                    error : (request, status, exception) => {
                        console.log(status);
                        console.error(exception);
                    }
                };

                jQuery.ajax(settings);
            } else {
                this._enabled = false;
                return;
            }

            this.trigger(new events.ComponentInitializedEvent(this));
            this.trigger(new events.WaitForEvent(this, events.ShowContentEvent.TYPE));
        }

        private correctScrollPosition() {
            /*
             if the container is scrolled we want to restore the scroll position before this._container was inserted
             */
            if (this._container.parent().scrollTop() > 0) {
                var containerHeightDiff = this._container.height();
                var parent = this._container.parent();
                parent.scrollTop(parent.scrollTop() + containerHeightDiff);
            }
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (this._enabled && e.type == events.ShowContentEvent.TYPE) {
                var sce = <events.ShowContentEvent>e;
                if (sce.component instanceof MyCoReChapterComponent) {
                    sce.content.prepend(this._container);
                }
            }
        }

        public get handlesEvents():string[] {
            return [ /*events.ProvideToolbarModelEvent.TYPE,*/ events.ShowContentEvent.TYPE /*widgets.toolbar.events.DropdownButtonPressedEvent.TYPE*/ ];
        }
    }
}

addViewerComponent(mycore.viewer.components.MyCoReMetadataComponent);
