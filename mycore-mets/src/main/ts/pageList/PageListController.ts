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

///<reference path="../model/MetsEditorModel.ts"/>
///<reference path="../state/PagesMoveChange.ts"/>
///<reference path="../model/simple/MCRMetsPage.ts"/>

namespace org.mycore.mets.controller {
    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;
    import PagesMoveChange = org.mycore.mets.model.state.PagesMoveChange;

    export class PageListController {

        public thumbnails: boolean = false;
        public editable: boolean;
        private messages: string[];
        private model: MetsEditorModel;
        private prevent: boolean = true;

        constructor(ngDraggable: any, private timeout: any, modal: any, hotkeys: any, i18NModel: any) {
            this.messages = i18NModel.messages;
            hotkeys.add({
                combo : 'up',
                description : '',
                callback : () => {
                    const pageSelection = this.model.pageSelection;
                    if (pageSelection.from === null) {
                        pageSelection.from = pageSelection.to = this.model.metsModel.metsPageList.length - 1;
                    } else if (pageSelection.from === 0) {
                        pageSelection.from = pageSelection.to = null;
                    } else {
                        pageSelection.to = pageSelection.from = pageSelection.from - 1;
                        return false;
                    }
                }
            });

            hotkeys.add({
                combo : 'down',
                description : '',
                callback : () => {
                    const pageSelection = this.model.pageSelection;
                    if (pageSelection.from === null) {
                        pageSelection.from = pageSelection.to = 0;
                    } else if (pageSelection.from >= this.model.metsModel.metsPageList.length) {
                        pageSelection.from = pageSelection.to = null;
                    } else {
                        pageSelection.to = pageSelection.from = pageSelection.from + 1;
                        return false;
                    }
                }
            });
        }

        public init(model: MetsEditorModel, editable: boolean = true, thumbnails: boolean = false) {
            this.model = model;
            this.editable = editable;
            this.thumbnails = thumbnails;
        }

        public getPageList() {
            return this.model.metsModel.metsPageList;
        }

        public getPageIndex(page: model.simple.MCRMetsPage) {
            return this.model.metsModel.metsPageList.indexOf(page);
        }

        public isPageSelected(page: model.simple.MCRMetsPage) {
            const pageIndex = this.getPageIndex(page);
            return this.model.pageSelection.from !== null &&
                this.model.pageSelection.from <= pageIndex &&
                this.model.pageSelection.to >= pageIndex;
        }

        public isPageAloneSelected(page: model.simple.MCRMetsPage) {
            const pageIndex = this.getPageIndex(page);
            return (this.model.pageSelection.from === this.model.pageSelection.to) && (this.model.pageSelection.from === pageIndex);
        }

        public getSelectedPages() {
            const selectedPageFilter = (page) => this.isPageSelected(page);
            return this.model.metsModel.metsPageList.filter(selectedPageFilter);
        }

        public pageClicked(page: model.simple.MCRMetsPage, event: any) {
            // prevent selection
            if (this.prevent) {
                this.prevent = false;
                return;
            }

            if (angular.element(event.target).is('[type=\'text\']')) {
                return;
            }

            document.getSelection().removeAllRanges();
            const pageIndex = this.getPageIndex(page);
            if (this.model.pageSelection.from === null || !event.shiftKey) {
                // user doest used shift key
                if (this.model.pageSelection.from !== pageIndex && this.model.pageSelection.to !== pageIndex) {
                    this.model.pageSelection.from = pageIndex;
                    this.model.pageSelection.to = pageIndex;
                } else {
                    this.model.pageSelection.from = this.model.pageSelection.to = null;
                }
            } else {
                event.preventDefault();
                event.stopPropagation();
                // used shift key
                if (this.model.pageSelection.from < pageIndex && pageIndex < this.model.pageSelection.to) {
                    // user clicked in range
                    if (this.model.pageSelection.lastExpand === 'top') {
                        this.model.pageSelection.from = pageIndex;
                    } else {
                        this.model.pageSelection.to = pageIndex;
                    }
                } else {
                    // user clicked out of page range
                    if (this.model.pageSelection.from > pageIndex) {
                        // user clicked above selection
                        this.model.pageSelection.from = pageIndex;
                        this.model.pageSelection.lastExpand = 'top';
                    } else {
                        // user clicked under selection
                        this.model.pageSelection.to = pageIndex;
                        this.model.pageSelection.lastExpand = 'bottom';
                    }
                }
            }
        }

        public dropSuccess(element: any, position: any, data: any, event: any) {
            const metsPageList = this.model.metsModel.metsPageList;

            if (typeof this.model.pageSelection === 'undefined' ||
                this.model.pageSelection === null ||
                this.model.pageSelection.from === null ||
                this.model.pageSelection.to === null ||
                (!(this.model.pageSelection.from in metsPageList)) ||
                (!(this.model.pageSelection.to in metsPageList))
            ) {
                throw new Error('invalid selection!');
            }

            const fromPage = metsPageList[ this.model.pageSelection.from ];
            const toPage = metsPageList[ this.model.pageSelection.to ];

            const range = {from : fromPage, to : toPage};
            const target = {before : position === 'before', element : element};

            this.model.stateEngine.changeModel(new PagesMoveChange(metsPageList, range, target));

            this.model.pageSelection.from = this.model.pageSelection.to = null;
        }

        public onDragComplete(data: any, event: any) {
            // workarround because click is triggered before drop success
            this.prevent = true;
        }

    }
}
