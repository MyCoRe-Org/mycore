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

///<reference path="PaginationModalModel.ts"/>
///<reference path="../model/simple/MCRMetsPage.ts"/>
///<reference path="../model/Pagination.ts"/>

namespace org.mycore.mets.controller.Pagination {

    import PaginationModalModel = org.mycore.mets.model.PaginationModalModel;

    export class PaginationModalController {
        public model: PaginationModalModel;
        private changes: {
            oldLabel: string;
            newLabel: string;
            page: org.mycore.mets.model.simple.MCRMetsPage
        }[] = [];

        constructor($scope: any, private $modalInstance: any) {
            this.model = <org.mycore.mets.model.PaginationModalModel> ($modalInstance.model);
            $scope.ctrl = this;
            $scope.$watch('ctrl.model.begin', () => this.doChanges());
            $scope.$watch('ctrl.model.method', () => this.doChanges());
            $scope.$watch('ctrl.model.reverse', () => this.doChanges());
            $scope.$watch('ctrl.model.value', () => {
                this.changeType();
                this.doChanges();
            });
        }

        public doChanges() {
            let changesLeft = true;

            while (changesLeft) {
                changesLeft = typeof this.changes.pop() !== 'undefined';
            }

            this.calculateChanges().forEach(c => this.changes.push(c));
        }

        public changeType() {
            const value = this.model.value;
            if (value !== null && typeof value !== 'undefined') {
                const newMethod = org.mycore.mets.model.pagination.detectPaginationMethodByPageLabel(value);
                if (newMethod !== null) {
                    this.model.method = newMethod;
                }
            }
        }

        public changeClicked(page: org.mycore.mets.model.simple.MCRMetsPage, index: number) {
            this.model.begin = index;
        }

        public calculateChanges(replaceOldLabel: boolean = true) {
            let changes;
            if (typeof this.model.method !== 'undefined' && this.model.method !== null &&
                this.model.method.test(this.model.value)) {
                changes = org.mycore.mets.model.pagination.getChanges(
                    0,
                    this.model.selectedPages.length,
                    this.model.begin,
                    this.model.value,
                    this.model.method,
                    this.model.reverse
                );
            } else {
                changes = this.model.selectedPages.map(() => '');
            }

            return this.model.selectedPages.map((page: org.mycore.mets.model.simple.MCRMetsPage, index: number) => {
                let oldLabel;
                if (replaceOldLabel) {
                    const pageNumber = (this.model.selectedPagesIndex + 1 + index);
                    const alternativeLabel = (this.model.messages.noOrderLabel + '(' + pageNumber + ')');
                    oldLabel = page.orderLabel || alternativeLabel;
                } else {
                    oldLabel = page.orderLabel;
                }
                return {
                    oldLabel : oldLabel,
                    newLabel : changes[ index ],
                    page : page
                };
            });
        }

        public change() {
            this.$modalInstance.close(this.calculateChanges(false).map((change) => {
                return new org.mycore.mets.model.state.PageLabelChange(change.page, change.newLabel, change.oldLabel);
            }));
        }

        public abort() {
            this.$modalInstance.dismiss();
        }
    }
}
