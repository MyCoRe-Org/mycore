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


import { Layer } from "../../base/components/model/Layer";
import { MyCoReMap } from "../../base/Utils";

export class TEILayer implements Layer {

  constructor(private _id: string, private _label: string, private mapping: MyCoReMap<string, string>, private contentLocation: string, private teiStylesheet: string) {
  }

  getId(): string {
    return this._id;
  }

  getLabel(): string {
    return this._label;
  }

  resolveLayer(pageHref: string, callback: (success: boolean, content?: HTMLElement) => void): void {
    if (this.mapping.has(pageHref)) {
      const url = `${this.contentLocation}${this.mapping.get(pageHref)}?XSL.Style=${this.teiStylesheet}`;

      fetch(url)
          .then(response => {
            if (!response.ok) {
              throw new Error('Network response was not ok ' + response.statusText);
            }
            return response.text();
          })
          .then(data => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(data, 'text/html');
              const rootNode = doc.getRootNode() as HTMLDocument;
              const wrapper = document.createElement("div");
              rootNode.body.childNodes.forEach(node => {
                 wrapper.append(node.cloneNode(true));
              });
              callback(true, wrapper);
          })
          .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
            callback(false);
          });
    } else {
      callback(false);
    }
  }
}

