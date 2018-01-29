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

///<reference path="../model/simple/MCRMetsPage.ts"/>
///<reference path="../model/MetsEditorModel.ts"/>
///<reference path="../model/simple/MCRMetsFile.ts"/>

namespace org.mycore.mets.controller {
    import MCRMetsPage = org.mycore.mets.model.simple.MCRMetsPage;
    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;
    import MCRMetsFile = org.mycore.mets.model.simple.MCRMetsFile;

    export interface PagePresenter {

    }

    export class DefaultPagePresenter implements PagePresenter {

        public currentPage: MCRMetsPage = null;
        private urlPattern: string = null;

        public init(metsEditorModel: MetsEditorModel) {
            this.urlPattern = metsEditorModel.configuration.imageLocationPattern.replace('{derivate}', metsEditorModel.metsId);
        }

        public getPreviewURL(fileList: MCRMetsFile[]) {
            const href = fileList.filter((f) => f.use === 'MASTER')[ 0 ].href;
            return this.urlPattern.replace('{image}', href).replace('{quality}', 'MID');
        }

        public getImageURL(fileList: MCRMetsFile[]) {
            const href = fileList.filter((f) => f.use === 'MASTER')[ 0 ].href;
            return this.urlPattern.replace('{image}', href).replace('{quality}', 'MAX');
        }
    }

}
