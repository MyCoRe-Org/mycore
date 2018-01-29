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
///<reference path="../state/StateEngine.ts"/>
///<reference path="../model/MetsEditorModel.ts"/>
///<reference path="../state/PageLabelChange.ts"/>

namespace org.mycore.mets.controller {
    import MCRMetsPage = org.mycore.mets.model.simple.MCRMetsPage;
    import StateEngine = org.mycore.mets.model.state.StateEngine;
    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;
    import I18nModel = org.mycore.mets.model.I18nModel;

    export class PageController {

        public messages: any;
        public page: MCRMetsPage;
        public thumbnail: boolean = false;
        public editable: boolean;
        private stateMachine: StateEngine;
        private edit: any = {label : null};
        private metsEditorModel: MetsEditorModel;
        private urlPrefix: string;
        private imageLocation: string;

        constructor(public i18NModel: I18nModel) {
            this.messages = i18NModel.messages;
        }

        public init(page: MCRMetsPage,
                    stateMachine: StateEngine,
                    metsEditorModel: MetsEditorModel,
                    editable: boolean = true) {

            this.page = page;
            this.stateMachine = stateMachine;
            this.metsEditorModel = metsEditorModel;
            this.editable = editable;
            this.imageLocation = metsEditorModel.configuration.imageLocationPattern
                .replace('{quality}', 'MIN')
                .replace('{derivate}', metsEditorModel.metsId)
                .replace('{image}', this.getFiles().filter((f) => f.use === 'MASTER')[ 0 ].href);
        }

        public removePagination(me: JQueryMouseEventObject) {
            const pageLabelChange = new org.mycore.mets.model.state.PageLabelChange(this.page, null, this.page.orderLabel);
            this.stateMachine.changeModel(pageLabelChange);
            me.preventDefault();
            me.stopImmediatePropagation();
            me.stopPropagation();
        }

        public hasLabel() {
            return 'orderLabel' in this.page && typeof this.page.orderLabel !== 'undefined' && this.page.orderLabel !== null;
        }

        public getFiles() {
            return this.page.fileList;
        }

        public editInputClicked(event: JQueryMouseEventObject) {
            event.stopPropagation();
            event.stopImmediatePropagation();
        }

        public editLabelKeyUp(keyEvent: JQueryKeyEventObject) {
            keyEvent.stopImmediatePropagation();
            keyEvent.stopPropagation();

            switch (keyEvent.keyCode) {
                case 13: // enter
                    this.applyEdit();
                    break;
                case 27: // esc
                    this.throwEdit();
                    break;
                default :
            }
        }

        public editLabel(clickEvent: JQueryMouseEventObject) {
            this.startEditLabel();
            clickEvent.stopPropagation();
            clickEvent.preventDefault();
        }

        private changeLabel(to: string) {
            const change = new org.mycore.mets.model.state.PageLabelChange(this.page, to, this.page.orderLabel);
            this.stateMachine.changeModel(change);
        }

        private throwEdit(event?: JQueryEventObject) {
            if (event !== null && typeof event !== 'undefined') {
                event.stopImmediatePropagation();
                event.stopPropagation();
            }

            this.edit.label = null;
        }

        private applyEdit(event?: JQueryEventObject) {
            if (event !== null && typeof event !== 'undefined') {
                event.stopImmediatePropagation();
                event.stopPropagation();
            }

            if (this.isValidLabel(this.edit.label)) {
                this.changeLabel(this.edit.label);
                this.edit.label = null;
            }
        }

        private isValidLabel(label: string) {
            return label !== null && typeof label !== 'undefined' && label.trim().length > 0;
        }

        private startEditLabel() {
            this.edit.label = this.page.orderLabel || '';
        }

    }
}
