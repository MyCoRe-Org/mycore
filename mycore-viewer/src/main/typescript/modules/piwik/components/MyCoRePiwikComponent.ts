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

declare var Piwik;
declare var window: Window;

namespace mycore.viewer.components {

    export interface MyCoRePiwikComponentSettings extends MyCoReViewerSettings {
        "MCR.Piwik.baseurl": string;
        "MCR.Piwik.id": string
    }

    /**
     * A piwik component that tracks page change events as downloads.
     * You have to set the following two properties:
     * 1. MCR.Piwik.baseurl - which is the url to the piwik server ending with a slash
     * 2. MCR.Piwik.id - id of the website. If you only track one site this is 1 by default
     */
    export class MyCoRePiwikComponent extends ViewerComponent {

        private initialized: boolean = false;

        constructor(private _settings: MyCoRePiwikComponentSettings) {
            super();
        }

        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (this.initialized == true && e.type == events.ImageChangedEvent.TYPE) {
                var imageChangedEvent = <events.ImageChangedEvent> e;
                this.trackImage(imageChangedEvent.image.href);
            } else if (e.type == events.ComponentInitializedEvent.TYPE) {
                var componentInitializedEvent = <events.ComponentInitializedEvent> e;
                if (ClassDescriber.ofEqualClass(componentInitializedEvent.component, this)) {
                    this.trackImage(this._settings.filePath);
                    this.initialized = true;
                }
            }
        }

        public get handlesEvents(): string[] {
            var handleEvents: Array<string> = new Array<string>();
            handleEvents.push(events.ImageChangedEvent.TYPE);
            handleEvents.push(events.ComponentInitializedEvent.TYPE);
            return handleEvents;
        }

        public trackImage(image: string): void {
            if (typeof Piwik !== 'undefined') {
                var derivate = this._settings.derivate;
                var trackURL = this._settings.webApplicationBaseURL + "/servlets/MCRIviewClient?derivate=" + derivate + "&page=" + image;
                var tracker = Piwik.getAsyncTracker();
                tracker.trackLink(trackURL, 'download');
            } else {
                console.log("warn: unable to track image " + image + " cause Piwik js object is undefined");
            }
        }

        public init() {
            window["_paq"] = [];
            var piwikURL = this._settings["MCR.Piwik.baseurl"];
            if (piwikURL == null) {
                console.log("Error: unable to get piwik base url (MCR.Piwik.baseurl)");
            }
            var pageID = this._settings["MCR.Piwik.id"];
            if (pageID == null) {
                console.log("Error: unable to get piwik id (MCR.Piwik.id)");
            }
            window["_paq"].push(["setTrackerUrl", piwikURL + "piwik.php"]);
            window["_paq"].push(["setSiteId", pageID]);
            var that = this;
            jQuery.getScript(piwikURL + 'piwik.js', function() {
                that.trigger(new events.ComponentInitializedEvent(that));
            });
        }

    }

}

addViewerComponent(mycore.viewer.components.MyCoRePiwikComponent);
