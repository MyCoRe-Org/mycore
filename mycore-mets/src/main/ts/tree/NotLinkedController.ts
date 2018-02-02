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

///<reference path="../model/simple/MCRMetsSection.ts"/>
///<reference path="../model/simple/MCRMetsSimpleModel.ts"/>
///<reference path="../model/MetsEditorModel.ts"/>
///<reference path="../model/simple/MCRMetsPage.ts"/>

namespace org.mycore.mets.controller {

    import MCRMetsSection = org.mycore.mets.model.simple.MCRMetsSection;
    import MCRMetsSimpleModel = org.mycore.mets.model.simple.MCRMetsSimpleModel;
    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;
    import MCRMetsPage = org.mycore.mets.model.simple.MCRMetsPage;

    export class NotLinkedController {
        private model: MetsEditorModel;

        constructor() {
            /* */
        }

        public init(model: MetsEditorModel) {
            this.model = model;
        }

        public getNotLinkedPages() {
            const linkedPages = [];

            const addLinkedPages = (section: MCRMetsSection) => {
                section.linkedPages.forEach((p: MCRMetsPage) => {
                    linkedPages[ p.id ] = true;
                });

                section.metsSectionList.forEach(addLinkedPages);
            };
            addLinkedPages(this.model.metsModel.rootSection);

            return this.model.metsModel.metsPageList.filter((p: MCRMetsPage) => {
                return linkedPages[ p.id ] !== true;
            });
        }

        public getPageLabel(p: MCRMetsPage) {
            return p.orderLabel || '[' + (this.model.metsModel.metsPageList.indexOf(p) + 1) + ']';
        }
    }
}
