
namespace mycore.viewer.widgets.modal {
    export class IviewPrintModalWindow extends IviewModalWindow {

        constructor(_mobile: boolean) {
            super(_mobile, "CreatePDF");
            var that = this;

            /*this.wrapper.removeClass("bs-modal-sm")
            this.wrapper.addClass("bs-modal-lg")

            this.box.removeClass("modal-sm")
            this.box.addClass("modal-lg")
             */
            this._inputRow = jQuery("<div></div>")
            this._inputRow.addClass("row");
            this._inputRow.appendTo(this.modalBody);

            this._previewBox = jQuery("<div></div>");
            this._previewBox.addClass("printPreview");
            this._previewBox.addClass("col-sm-6");
            this._previewBox.addClass("thumbnail");
            this._previewBox.appendTo(this._inputRow);

            this._previewImage = jQuery("<img />");
            this._previewImage.appendTo(this._previewBox);

            this._pageSelectBox = jQuery("<form></form>");
            this._pageSelectBox.addClass("printForm");
            this._pageSelectBox.addClass("col-sm-6");
            this._pageSelectBox.appendTo(this._inputRow);

            this._selectGroup = jQuery("<div></div>");
            this._selectGroup.addClass("form-group");
            this._selectGroup.appendTo(this._pageSelectBox);

            this._createRadioAllPages();
            this._createRadioCurrentPage();
            this._createRadioRangePages();

            this._validationRow = jQuery("<div></div>");
            this._validationRow.addClass("row");
            this._validationRow.appendTo(this.modalBody);

            this._validationMessage = jQuery("<p></p>");
            this._validationMessage.addClass("col-sm-6");
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

            var that = this;
            this._okayButton.click(() => {
                if (that.okayClickHandler != null) {
                    that.okayClickHandler();
                }
            });

        }

        private static INPUT_IDENTIFIER = "pages";
        private static INPUT_RANGE_IDENTIFIER = "range";
        private static INPUT_RANGE_TEXT_IDENTIFIER = IviewPrintModalWindow.INPUT_RANGE_IDENTIFIER + "-text";

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

        private _validationMessage: JQuery;
        private _maximalPageMessage: JQuery;
        private _maximalPageNumber: JQuery;

        public checkEventHandler: (id: string) => void = null;
        public rangeInputEventHandler: (text: string) => void = null;
        public okayClickHandler: () => void = null;

        private _createRadioAllPages() {
            this._radioAllPages = jQuery("<div></div>");
            this._radioAllPages.addClass("radio");
            this._radioAllPagesLabelElement = jQuery("<label></label>");
            this._radioAllPagesInput = jQuery("<input>");
            this._radioAllPagesInput.attr("type", "radio");
            this._radioAllPagesInput.attr("name", IviewPrintModalWindow.INPUT_IDENTIFIER);
            this._radioAllPagesInput.attr("id", IviewPrintModalWindow.INPUT_IDENTIFIER);
            this._radioAllPagesInput.attr("value", IviewPrintModalWindow.INPUT_ALL_VALUE);
            this._radioAllPagesLabel = jQuery("<p></p>");

            var that = this;
            this._radioAllPagesInput.change(() => {
                if (that.checkEventHandler != null) {
                    that.checkEventHandler(IviewPrintModalWindow.INPUT_ALL_VALUE);
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
            this._radioCurrentPageInput.attr("name", IviewPrintModalWindow.INPUT_IDENTIFIER);
            this._radioCurrentPageInput.attr("id", IviewPrintModalWindow.INPUT_IDENTIFIER);
            this._radioCurrentPageInput.attr("value", IviewPrintModalWindow.INPUT_CURRENT_VALUE);
            this._radioCurrentPageLabel = jQuery("<p></p>");

            var that = this;
            this._radioCurrentPageInput.change(() => {
                if (that.checkEventHandler != null) {
                    that.checkEventHandler(IviewPrintModalWindow.INPUT_CURRENT_VALUE);
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
            this._radioRangePagesInput.attr("name", IviewPrintModalWindow.INPUT_IDENTIFIER);
            this._radioRangePagesInput.attr("id", IviewPrintModalWindow.INPUT_IDENTIFIER);
            this._radioRangePagesInput.attr("value", IviewPrintModalWindow.INPUT_RANGE_VALUE);
            this._radioRangePagesLabel = jQuery("<p></p>");
            this._radioRangePagesInputText = jQuery("<input>");
            this._radioRangePagesInputText.addClass("form-control");
            this._radioRangePagesInputText.attr("type", "text");
            this._radioRangePagesInputText.attr("name", IviewPrintModalWindow.INPUT_RANGE_TEXT_IDENTIFIER);
            this._radioRangePagesInputText.attr("id", IviewPrintModalWindow.INPUT_RANGE_TEXT_IDENTIFIER);
            this._radioRangePagesInputText.attr("placeholder", "1,3-5,8");

            var that = this;
            var onActivateHandler = () => {
                if (that.checkEventHandler != null) {
                    that.checkEventHandler(IviewPrintModalWindow.INPUT_RANGE_VALUE);
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

        public set validationResult(success: boolean) {
            if (success) {
                this._validationMessage.removeClass("text-danger");
                this._validationMessage.addClass("text-success")
                this._okayButton.removeClass("disabled");
            } else {
                this._validationMessage.removeClass("text-success");
                this._validationMessage.addClass("text-danger")
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


        public set maximalPages(number:string) {
            this._maximalPageNumber.text(number);
        }

        public set maximalPageMessage(message:string) {
            this._maximalPageNumber.detach()
            var messageDiv = this._maximalPageMessage.find(".message");
            messageDiv.text(message + " ");
            messageDiv.append(this._maximalPageNumber);
        }


    }
}