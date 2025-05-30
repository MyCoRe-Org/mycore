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

import { loadManifest, Manifest, parseManifest } from "manifesto.js";
import { IIIFStructureBuilder } from "./IIIFStructureBuilder";

export class IviewIIIFProvider {

  public static loadModel(manifestDocumentLocation: string,
    imageAPIURL: string,
    tilePathBuilder: (href: string,
      width: number,
      height: number)
      => string): GivenViewerPromise<{
        model: StructureModel;
        document: Document
      }, any> {
    const promise = new ViewerPromise<{ model: StructureModel; document: Document }, any>();
    let document = undefined;
    loadManifest(manifestDocumentLocation).then((manifest) => {
      document = manifest;
      return parseManifest(manifest)
    }).then((manifestParsed) => {
      const builder = new IIIFStructureBuilder(manifestParsed as Manifest, tilePathBuilder, imageAPIURL);
      promise.resolve({ model: builder.processManifest(), document: document });
    }).catch((error) => {
      console.log("Error loading manifest: " + error);
      promise.reject(error);

    });
    return promise;
  }

}
