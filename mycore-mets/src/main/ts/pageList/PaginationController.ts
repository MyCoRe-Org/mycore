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

///<reference path="../model/Pagination.ts"/>
///<reference path="PaginationModalModel.ts"/>
///<reference path="../state/PageLabelChange.ts"/>
///<reference path="../state/BatchChange.ts"/>
///<reference path="../model/MetsEditorModel.ts"/>

namespace org.mycore.mets.controller {

    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;

    export class PaginationController {

        constructor(private $modal, private i18nModel, hotkeys) {
            hotkeys.add({
                combo : "ctrl+p",
                description : "",
                callback : () => {
                    this.paginationClicked();
                }
            });
        }

        private metsEditorModel: MetsEditorModel;
        private changeListModalInstance;

        public init(metsEditorModel: MetsEditorModel) {
            this.metsEditorModel = metsEditorModel;
        }

        public paginationClicked() {
            this.openPaginationModal();
        }

        public openPaginationModal(begin = 0, method = model.Pagination.paginationMethods[ 0 ], value = "") {
            const options = {
                templateUrl : "pageList/paginationModal.html",
                controller : "PaginationModalController",
                size : "lg"
            };

            this.changeListModalInstance = this.$modal.open(options);
            this.changeListModalInstance.model = new org.mycore.mets.model.PaginationModalModel(this.i18nModel.messages,
                this.metsEditorModel.metsModel.metsPageList.filter((t, index) => {
                    return (this.metsEditorModel.pageSelection.from !== null) ?
                        index >= this.metsEditorModel.pageSelection.from && index <= this.metsEditorModel.pageSelection.to :
                        true;
                }),
                this.metsEditorModel.pageSelection.from,
                begin,
                method,
                value);

            this.changeListModalInstance.result.then((lChanges: Array<org.mycore.mets.model.state.PageLabelChange>) => {
                this.metsEditorModel.stateEngine.changeModel(new org.mycore.mets.model.state.BatchChange(lChanges));
            }, () => {
                // Dismiss
            });

        }

    }
}

