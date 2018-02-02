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

///<reference path="MetsEditorConfiguration.ts"/>

namespace org.mycore.mets.model {
    export function i18n($http: ng.IHttpService,
                         $location: ng.ILocationService,
                         $log: ng.ILogService, editorConfiguration: MetsEditorConfiguration) {
        const metsEditorMessageModel = {messages : []};

        (<any> $http.get(editorConfiguration.i18URL)).success((i18nData) => {
            for (const index in i18nData) {
                if (i18nData.hasOwnProperty(index)) {
                    let betterKey = index;
                    if (index.indexOf('component.mets.editor') === 0) {
                        betterKey = index.substr('component.mets.editor.'.length);
                    } else if (index.indexOf('component.mets.dfgStructureSet') === 0) {
                        betterKey = index.substr('component.mets.dfgStructureSet.'.length);
                    }
                    metsEditorMessageModel.messages[ betterKey ] = i18nData[ index ];
                }
            }
        });

        return metsEditorMessageModel;
    }

    export interface I18nModel {
        messages: any;
    }
}
