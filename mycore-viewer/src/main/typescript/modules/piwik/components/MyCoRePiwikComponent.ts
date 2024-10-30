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

import { ViewerComponent } from "../../base/components/ViewerComponent";
import { MyCoReViewerSettings } from "../../base/MyCoReViewerSettings";
import { ViewerEvent } from "../../base/widgets/events/ViewerEvent";
import { ImageChangedEvent } from "../../base/components/events/ImageChangedEvent";
import { ClassDescriber } from "../../base/Utils";
import { ComponentInitializedEvent } from "../../base/components/events/ComponentInitializedEvent";

declare var Piwik;
declare var window: Window;

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

  public handle(e: ViewerEvent): void {
    if (this.initialized == true && e.type == ImageChangedEvent.TYPE) {
      const imageChangedEvent = e as ImageChangedEvent;
      this.trackImage(imageChangedEvent.image.href);
    } else if (e.type == ComponentInitializedEvent.TYPE) {
      const componentInitializedEvent = e as ComponentInitializedEvent;
      if (ClassDescriber.ofEqualClass(componentInitializedEvent.component, this)) {
        this.trackImage(this._settings.filePath);
        this.initialized = true;
      }
    }
  }

  public get handlesEvents(): string[] {
    const handleEvents: Array<string> = new Array<string>();
    handleEvents.push(ImageChangedEvent.TYPE);
    handleEvents.push(ComponentInitializedEvent.TYPE);
    return handleEvents;
  }

  public trackImage(image: string): void {
    if (typeof Piwik !== 'undefined') {
      const derivate = this._settings.derivate;
      const trackURL = this._settings.webApplicationBaseURL + "/servlets/MCRIviewClient?derivate=" + derivate + "&page=" + image;
      const tracker = Piwik.getAsyncTracker();
      tracker.trackLink(trackURL, 'download');
    } else {
      console.log("warn: unable to track image " + image + " cause Piwik js object is undefined");
    }
  }

  public init() {
    window["_paq"] = [];
    const piwikURL = this._settings["MCR.Piwik.baseurl"];
    if (piwikURL == null) {
      console.log("Error: unable to get piwik base url (MCR.Piwik.baseurl)");
    }
    const pageID = this._settings["MCR.Piwik.id"];
    if (pageID == null) {
      console.log("Error: unable to get piwik id (MCR.Piwik.id)");
    }
    window["_paq"].push(["setTrackerUrl", piwikURL + "piwik.php"]);
    window["_paq"].push(["setSiteId", pageID]);
    jQuery.getScript(piwikURL + 'piwik.js', () => {
      this.trigger(new ComponentInitializedEvent(this));
    });
  }

}


