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
///<reference path="../state/StateEngine.ts"/>
///<reference path="simple/MCRMetsSimpleModel.ts"/>

namespace org.mycore.mets.model {
    export class MetsEditorModel {

        public mode: EditorMode = EditorMode.Pagination;
        public dataLoaded: boolean = false;
        public metsModel: simple.MCRMetsSimpleModel;
        public metsId: string;
        public middleView: ViewOptions = ViewOptions.SectionTree;
        public pageSelection: { from: number, to: number, lastExpand: 'top'|'bottom' } = {
            from : null,
            to : null,
            lastExpand : 'top'
        };
        public targetServlet: string;
        public lockURL: string;
        public unLockURL: string;
        public locked: boolean = false;
        public stateEngine: org.mycore.mets.model.state.StateEngine = new org.mycore.mets.model.state.StateEngine();

        constructor(public configuration: MetsEditorConfiguration) {
        }

        public onModelLoad(metsSimpleModel: simple.MCRMetsSimpleModel) {
            this.metsModel = metsSimpleModel;
            this.dataLoaded = true;
        }

    }

    export enum EditorMode {
        Pagination = 'pagination', Structuring = 'structuring', Association = 'association'
    }

    export enum ViewOptions {
        SectionTree = 'sectionTree', Pages = 'pages'
    }
}
