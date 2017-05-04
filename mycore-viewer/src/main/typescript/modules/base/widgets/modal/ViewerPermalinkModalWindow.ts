/// <reference path="../../Utils.ts" />
/// <reference path="../../definitions/jquery.d.ts" />
/// <reference path="ViewerModalWindow.ts" />

namespace mycore.viewer.widgets.modal {
    export class ViewerPermalinkModalWindow extends IviewModalWindow{
        constructor(_mobile:boolean) {
            super(_mobile, "Permalink");
            var that = this;

            this._textArea = jQuery("<textarea></textarea>");
            this._textArea.addClass("form-control");
            this._textArea.appendTo(this.modalBody);
            this._textArea.on("click", function () {
                that._textArea.select();
            });

        }

        private _textArea:JQuery;

        public set permalink(link:string) {
            this._textArea.text(link);
        }


    }
}