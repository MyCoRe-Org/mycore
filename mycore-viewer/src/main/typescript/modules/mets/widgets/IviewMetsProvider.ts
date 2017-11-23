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

/// <reference path="MetsStructureModel.ts" />
/// <reference path="MetsStructureBuilder.ts" />

namespace mycore.viewer.widgets.mets {
    export class IviewMetsProvider {

        public static loadModel(metsDocumentLocation:string, tilePathBuilder:(href:string)=>string):GivenViewerPromise<{model:model.StructureModel; document:Document}, any> {
            let promise = new ViewerPromise<{model:model.StructureModel; document:Document}, any>();
            let settings = {
                url: metsDocumentLocation,
                success: function (response) {
                    let builder = new MetsStructureBuilder(response, tilePathBuilder);
                    promise.resolve({model : builder.processMets(), document : response});
                },
                error: function (request, status, exception) {
                    promise.reject(exception);
                }
            };
            jQuery.ajax(settings);
            return promise;
        }

    }

}
