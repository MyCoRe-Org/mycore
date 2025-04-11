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

import { GivenViewerPromise, ViewerPromise } from "../../base/Utils";
import { StructureModel } from "../../base/components/model/StructureModel";
import { MetsStructureBuilder } from "./MetsStructureBuilder";


export class IviewMetsProvider {

  public static loadModel(metsDocumentLocation: string, tilePathBuilder: (href: string) => string): GivenViewerPromise<{
    document: Document;
    model: StructureModel
  }, any> {
    let promise = new ViewerPromise<{ model: StructureModel; document: Document }, any>();
    fetch(metsDocumentLocation)
    .then(response => {
      if (!response.ok) {
        promise.reject(response.statusText);
        return null;
      }
      return response.text();
    })
    .then(text => {
      let parser = new DOMParser();
      let xmlDoc = parser.parseFromString(text, "text/xml");
      let builder = new MetsStructureBuilder(xmlDoc, tilePathBuilder);
      promise.resolve({ model: builder.processMets(), document: xmlDoc });
    }).catch(error => {
      promise.reject(error);
    });
    return promise;
  }

}


