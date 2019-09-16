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

namespace mycore.viewer.widgets.modal {
    export class ViewerPrintModalWindow extends IviewModalWindow {

        constructor(_mobile: boolean) {
            super(_mobile, "CreatePDF");

            /*this.wrapper.removeClass("bs-modal-sm")
            this.wrapper.addClass("bs-modal-lg")

            this.box.removeClass("modal-sm")
            this.box.addClass("modal-lg")
             */
            this._inputRow = jQuery("<div></div>");
            this._inputRow.addClass("row");
            this._inputRow.appendTo(this.modalBody);

            this._previewBox = jQuery("<div></div>");
            this._previewBox.addClass("printPreview");
            this._previewBox.addClass("col-sm-12");
            this._previewBox.addClass("thumbnail");
            this._previewBox.appendTo(this._inputRow);

            this._previewImage = jQuery("<img />");
            this._previewImage.appendTo(this._previewBox);

            this._pageSelectBox = jQuery("<form></form>");
            this._pageSelectBox.addClass("printForm");
            this._pageSelectBox.addClass("col-sm-12");
            this._pageSelectBox.appendTo(this._inputRow);

            this._selectGroup = jQuery("<div></div>");
            this._selectGroup.addClass("form-group");
            this._selectGroup.appendTo(this._pageSelectBox);

            this._createRadioAllPages();
            this._createRadioCurrentPage();
            this._createRadioRangePages();
            this._createRadioChapters();

            this._validationRow = jQuery("<div></div>");
            this._validationRow.addClass("row");
            this._validationRow.appendTo(this.modalBody);

            this._validationMessage = jQuery("<p></p>");
            this._validationMessage.addClass("col-sm-12");
            this._validationMessage.addClass("pull-right");
            this._validationMessage.appendTo(this._validationRow);

            this._okayButton = jQuery("<a>OK</a>");
            this._okayButton.attr("type", "button");
            this._okayButton.addClass("btn btn-default");
            this._okayButton.appendTo(this.modalFooter);

            this._maximalPageMessage = jQuery("<div class='row'><span class='col-sm-12 message'></span></div>");
            this._maximalPageMessage.appendTo(this.modalBody);

            this._maximalPageNumber = jQuery("<span></span>");
            this._maximalPageNumber.text("");
            this._maximalPageMessage.children().append(this._maximalPageNumber);

            this._okayButton.click(() => {
                if (this.okayClickHandler != null) {
                    this.okayClickHandler();
                }
            });

        }

        private static INPUT_IDENTIFIER = "pages";
        private static INPUT_RANGE_IDENTIFIER = "range";
        private static INPUT_RANGE_TEXT_IDENTIFIER = ViewerPrintModalWindow.INPUT_RANGE_IDENTIFIER + "-text";
        private static INPUT_CHAPTER_VALUE = "chapter";

        public static INPUT_ALL_VALUE = "all";
        public static INPUT_RANGE_VALUE = "range";
        public static INPUT_CURRENT_VALUE = "current";

        private _validationRow: JQuery;
        private _inputRow: JQuery;

        private _previewBox: JQuery;
        private _previewImage: JQuery;
        private _pageSelectBox: JQuery;
        private _okayButton: JQuery;
        private _selectGroup: JQuery;

        private _radioAllPages: JQuery;
        private _radioAllPagesLabelElement: JQuery;
        private _radioAllPagesLabel: JQuery;
        private _radioAllPagesInput: JQuery;

        private _radioCurrentPage: JQuery;
        private _radioCurrentPageLabelElement: JQuery;
        private _radioCurrentPageLabel: JQuery;
        private _radioCurrentPageInput: JQuery;

        private _radioRangePages: JQuery;
        private _radioRangePagesLabelElement: JQuery;
        private _radioRangePagesLabel: JQuery;
        private _radioRangePagesInput: JQuery;
        private _radioRangePagesInputText: JQuery;

        private _radioChapter: JQuery;
        private _radioChapterInput: JQuery;
        private _chapterLabelElement: JQuery;
        private _radioChapterLabel: JQuery;
        private _chapterSelect: JQuery;

        private _validationMessage: JQuery;
        private _maximalPageMessage: JQuery;
        private _maximalPageNumber: JQuery;

        public checkEventHandler: (id: string) => void = null;
        public rangeInputEventHandler: (text: string) => void = null;
        public chapterInputEventHandler: (id: string) => void = null;

        public okayClickHandler: () => void = null;

        private _createRadioAllPages() {
            this._radioAllPages = jQuery("<div></div>");
            this._radioAllPages.addClass("radio");
            this._radioAllPagesLabelElement = jQuery("<label></label>");
            this._radioAllPagesInput = jQuery("<input>");
            this._radioAllPagesInput.attr("type", "radio");
            this._radioAllPagesInput.attr("name", ViewerPrintModalWindow.INPUT_IDENTIFIER);
            this._radioAllPagesInput.attr("id", ViewerPrintModalWindow.INPUT_IDENTIFIER);
            this._radioAllPagesInput.attr("value", ViewerPrintModalWindow.INPUT_ALL_VALUE);
            this._radioAllPagesLabel = jQuery("<p></p>");

            this._radioAllPagesInput.change(() => {
                if (this.checkEventHandler != null) {
                    this.checkEventHandler(ViewerPrintModalWindow.INPUT_ALL_VALUE);
                }
            });

            this._radioAllPages.append(this._radioAllPagesLabelElement);
            this._radioAllPagesLabelElement.append(this._radioAllPagesInput);
            this._radioAllPagesLabelElement.append(this._radioAllPagesLabel);

            this._radioAllPages.appendTo(this._selectGroup);
        }


        private _createRadioCurrentPage() {
            this._radioCurrentPage = jQuery("<div></div>");
            this._radioCurrentPage.addClass("radio");
            this._radioCurrentPageLabelElement = jQuery("<label></label>");
            this._radioCurrentPageInput = jQuery("<input>");
            this._radioCurrentPageInput.attr("type", "radio");
            this._radioCurrentPageInput.attr("name", ViewerPrintModalWindow.INPUT_IDENTIFIER);
            this._radioCurrentPageInput.attr("id", ViewerPrintModalWindow.INPUT_IDENTIFIER);
            this._radioCurrentPageInput.attr("value", ViewerPrintModalWindow.INPUT_CURRENT_VALUE);
            this._radioCurrentPageLabel = jQuery("<p></p>");

            this._radioCurrentPageInput.change(() => {
                if (this.checkEventHandler != null) {
                    this.checkEventHandler(ViewerPrintModalWindow.INPUT_CURRENT_VALUE);
                }
            });

            this._radioCurrentPage.append(this._radioCurrentPageLabelElement);
            this._radioCurrentPageLabelElement.append(this._radioCurrentPageInput);
            this._radioCurrentPageLabelElement.append(this._radioCurrentPageLabel);

            this._radioCurrentPage.appendTo(this._selectGroup);
        }


        private _createRadioRangePages() {
            this._radioRangePages = jQuery("<div></div>");
            this._radioRangePages.addClass("radio");
            this._radioRangePagesLabelElement = jQuery("<label></label>");
            this._radioRangePagesInput = jQuery("<input>");
            this._radioRangePagesInput.attr("type", "radio");
            this._radioRangePagesInput.attr("name", ViewerPrintModalWindow.INPUT_IDENTIFIER);
            this._radioRangePagesInput.attr("id", ViewerPrintModalWindow.INPUT_IDENTIFIER);
            this._radioRangePagesInput.attr("value", ViewerPrintModalWindow.INPUT_RANGE_VALUE);
            this._radioRangePagesLabel = jQuery("<p></p>");
            this._radioRangePagesInputText = jQuery("<input>");
            this._radioRangePagesInputText.addClass("form-control");
            this._radioRangePagesInputText.attr("type", "text");
            this._radioRangePagesInputText.attr("name", ViewerPrintModalWindow.INPUT_RANGE_TEXT_IDENTIFIER);
            this._radioRangePagesInputText.attr("id", ViewerPrintModalWindow.INPUT_RANGE_TEXT_IDENTIFIER);
            this._radioRangePagesInputText.attr("placeholder", "1,3-5,8");

            const that = this;
            const onActivateHandler = () => {
                if (that.checkEventHandler != null) {
                    that.checkEventHandler(ViewerPrintModalWindow.INPUT_RANGE_VALUE);
                }
                this._radioRangePagesInputText.focus();
            };
            this._radioRangePagesInput.change(onActivateHandler);
            this._radioRangePagesInput.click(onActivateHandler);

            this._radioRangePagesInputText.click(() => {
                this.allChecked = false;
                this.currentChecked = false;
                this.rangeChecked = true;
                onActivateHandler();
                this._radioRangePagesInputText.focus();
            });

            this._radioRangePagesInputText.keyup(() => {
                if (that.rangeInputEventHandler != null) {
                    this.rangeInputEventHandler(this._radioRangePagesInputText.val());
                }
            });

            this._radioRangePages.append(this._radioRangePagesLabelElement);
            this._radioRangePagesLabelElement.append(this._radioRangePagesInput);
            this._radioRangePagesLabelElement.append(this._radioRangePagesLabel);
            this._radioRangePagesLabelElement.append(this._radioRangePagesInputText);
            this._radioRangePages.appendTo(this._selectGroup);
        }

        private _createRadioChapters() {
            this._radioChapter = jQuery("<div></div>");
            this._radioChapter.addClass("radio");

            this._radioChapterInput = jQuery("<input>");
            this._radioChapterInput.attr("type", "radio");
            this._radioChapterInput.attr("name", ViewerPrintModalWindow.INPUT_IDENTIFIER);
            this._radioChapterInput.attr("id", ViewerPrintModalWindow.INPUT_IDENTIFIER);
            this._radioChapterInput.attr("value", ViewerPrintModalWindow.INPUT_CHAPTER_VALUE);

            this._chapterLabelElement = jQuery("<label></label>");
            this._radioChapterLabel = jQuery("<p></p>");
            this._chapterSelect = jQuery("<select></select>");

            this._radioRangePages.append(this._chapterLabelElement);
            this._chapterLabelElement.append(this._radioChapterInput);
            this._chapterLabelElement.append(this._radioChapterLabel);
            this._chapterLabelElement.append(this._chapterSelect);
            this._radioChapter.appendTo(this._selectGroup);

            let onActivate = () => {
                if (this.checkEventHandler != null) {
                    this.checkEventHandler(ViewerPrintModalWindow.INPUT_CHAPTER_VALUE);
                }
            };
            this._radioChapterInput.change(onActivate);

            this._chapterSelect.change(()=>{
                onActivate();
                this.chapterChecked = true;
                if(this.chapterInputEventHandler != null) {
                    this.chapterInputEventHandler(this._chapterSelect.val());
                }
            });
        }

        public set rangeChecked(checked: boolean) {
            this._radioRangePagesInput.prop("checked", checked);
        }

        public get rangeChecked() {
            return <boolean>this._radioRangePagesInput.prop("checked");
        }

        public set allChecked(checked: boolean) {
            this._radioAllPagesInput.prop("checked", checked);
        }

        public get allChecked() {
            return <boolean>this._radioAllPagesInput.prop("checked");
        }

        public set currentChecked(checked: boolean) {
            this._radioCurrentPageInput.prop("checked", checked);
        }

        public get currentChecked() {
            return <boolean>this._radioCurrentPageInput.prop("checked");
        }

        public set chapterChecked(checked: boolean) {
            this._radioChapterInput.prop("checked", checked);
        }

        public get chapterChecked() {
            return <boolean>this._radioChapterInput.prop("checked");
        }

        public set validationResult(success: boolean) {
            if (success) {
                this._validationMessage.removeClass("text-danger");
                this._validationMessage.addClass("text-success");
                this._okayButton.removeClass("disabled");
            } else {
                this._validationMessage.removeClass("text-success");
                this._validationMessage.addClass("text-danger");
                this._okayButton.addClass("disabled");
            }
        }

        public set validationMessage(message: string) {
            this._validationMessage.text(message);
        }

        public get validationMessage() {
            return <string>this._validationMessage.text();
        }

        public set currentPageLabel(label: string) {
            this._radioCurrentPageLabel.text(label);
        }

        public get currentPageLabel() {
            return this._radioCurrentPageLabel.text();
        }

        public set allPagesLabel(label: string) {
            this._radioAllPagesLabel.text(label);
        }

        public get allPagesLabel() {
            return this._radioAllPagesLabel.text();
        }

        public set rangeLabel(label: string) {
            this._radioRangePagesLabel.text(label);
        }

        public get rangeLabel() {
            return this._radioRangePagesLabel.text();
        }

        public set previewImageSrc(src: string) {
            this._previewImage.attr("src", src);
        }

        public get rangeInputVal() {
            return this._radioRangePagesInputText.val();
        }

        public set rangeInputEnabled(enabled: boolean) {
            this._radioRangePagesInputText.val("");
            this._radioRangePagesInputText.attr("enabled", enabled);
        }

        public get rangeInputEnabled() {
            return this._radioRangePagesInputText.attr("enabled") == "true";
        }

        public get previewImageSrc() {
            return <string>this._previewImage.attr("src");
        }

        public get chapterLabel(){
            return this._radioChapterLabel.text();
        }

        public set chapterLabel(label:string){
            this._radioChapterLabel.text(label);
        }

        public set maximalPages(number: string) {
            this._maximalPageNumber.text(number);
        }

        public set maximalPageMessage(message: string) {
            this._maximalPageNumber.detach();
            let messageDiv = this._maximalPageMessage.find(".message");
            messageDiv.text(message + " ");
            messageDiv.append(this._maximalPageNumber);
        }

        public setChapterTree(chapters: Array<{ id: string, label: string }>) {
            this._chapterSelect.html(chapters.map((entry) => {
                return `<option value="${entry.id}">${entry.label}</option>`;
            }).join(""));
        }

        public get chapterInputVal(){
            return this._chapterSelect.val();
        }

    }
}
