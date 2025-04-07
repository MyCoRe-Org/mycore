/*
 * This file is part of *** M y C o R e ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe. If not, see <http://www.gnu.org/licenses/>.
 */

import { MyCoReViewerSettings } from "../../base/MyCoReViewerSettings";
import { ViewerComponent } from "../../base/components/ViewerComponent";
import {getElementHeight, singleSelectShim, ViewerFormatString, XMLUtil} from "../../base/Utils";
import { ShowContentEvent } from "../../base/components/events/ShowContentEvent";
import { MyCoReChapterComponent } from "../../base/components/MyCoReChapterComponent";
import { ViewerEvent } from "../../base/widgets/events/ViewerEvent";
import { WaitForEvent } from "../../base/components/events/WaitForEvent";
import { ComponentInitializedEvent } from "../../base/components/events/ComponentInitializedEvent";

export interface MetadataSettings extends MyCoReViewerSettings {
  objId: string;
  metadataURL: string;
  metsURL?: string;
}

export class MyCoReMetadataComponent extends ViewerComponent {

  constructor(private _settings: MetadataSettings) {
    super();
  }

  private _container: HTMLElement;
  private _enabled: boolean = true;

  public init() {
    this._container = document.createElement("div");
    this._container.classList.add("card-body");
    if (typeof this._settings.metadataURL !== "undefined" && this._settings.metadataURL !== null) {
      const metadataUrl = ViewerFormatString(this._settings.metadataURL, {
        derivateId: this._settings.derivate,
        objId: this._settings.objId
      });
      fetch(metadataUrl)
          .then(response => response.text())
          .then(data => {
            this._container.innerHTML = data;
            this.correctScrollPosition();
          })
          .catch(error => console.error('Error:', error));
    } else if ("metsURL" in this._settings) {
      const xpath = "/mets:mets/*/mets:techMD/mets:mdWrap[@OTHERMDTYPE='MCRVIEWER_HTML']/mets:xmlData/*";
      const metsURL = this._settings.metsURL;
      fetch(metsURL)
          .then(response => response.text())
          .then(data => {
            const parser = new DOMParser();
            const xmlDoc = parser.parseFromString(data, "application/xml");
            let htmlElement = singleSelectShim(xmlDoc, xpath, XMLUtil.NS_MAP);
            if (htmlElement !== null) {
              if ("xml" in htmlElement) {
                // htmlElement is IXMLDOMElement
                htmlElement = parser.parseFromString((<any>htmlElement).xml, "text/html");
              }
              this._container.appendChild(htmlElement);
            } else {
              this._container.remove();
            }
          })
          .catch(error => console.error('Error:', error));
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
    const parent = this._container.parentElement;
    if (parent.scrollTop > 0) {
      const containerHeightDiff = getElementHeight(this._container);
      parent.scrollTop += containerHeightDiff;
    }
  }

  public handle(e: ViewerEvent): void {
    if (this._enabled && e.type === ShowContentEvent.TYPE) {
      const sce = e as ShowContentEvent;
      if (sce.component instanceof MyCoReChapterComponent) {
        if(sce.content instanceof HTMLElement) {
          (sce.content as HTMLElement).prepend(this._container);
        } else if (sce.content instanceof Array) {
          for (let i = 0; i < sce.content.length; i++) {
            if (sce.content[i] instanceof HTMLElement) {
              (sce.content[i] as HTMLElement).prepend(this._container);
            }
          }
        }
      }
    }
  }

  public get handlesEvents(): string[] {
    return [ShowContentEvent.TYPE];
  }
}
