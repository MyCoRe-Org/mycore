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

/// <reference path="../../Utils.ts" />
/// <reference path="../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.modal {
    import LanguageModel = mycore.viewer.model.LanguageModel;
    export class IviewModalWindow {
        constructor(private _mobile:boolean, _title:string, parent:HTMLElement = document.body) {
            let that = this;

            this._wrapper = jQuery("<div></div>");
            this._wrapper.addClass("modal fade bs-modal-sm");
            this._wrapper.attr("tabindex", "-1");
            this._wrapper.attr("role", "dialog");
            this._wrapper.attr("aria-labeleby", "permalinkLabel");
            this._wrapper.attr("aria-hidden", "true");
            this._wrapper.on("click", function (e) {
                if (e.target == that._wrapper[0]) {
                    that.hide();
                }
            });

            this._box = jQuery("<div></div>");
            this._box.addClass("modal-dialog modal-sm");
            this._box.appendTo(this._wrapper);

            this._content = jQuery("<div></div>");
            this._content.addClass("modal-content");
            this._content.appendTo(this._box);

            this._header = jQuery("<div><h4 class='modal-title' data-i18n='" + _title + "'>" + _title + "</h4></div>");
            this._header.addClass("modal-header");
            this._header.appendTo(this._content);

            this._body = jQuery("<div></div>");
            this._body.addClass("modal-body");
            this._body.appendTo(this._content);

            this._footer = jQuery("<div></div>");
            this._footer.addClass("modal-footer");
            this._footer.appendTo(this._content);

            this._close = jQuery("<a data-i18n='modal.close'>Close</a>");
            this._close.attr("type", "button");
            this._close.addClass("btn btn-default");
            this._close.appendTo(this._footer);

            this._close.click(()=> {
                that.hide();
            });

            if (!this._mobile) {
                (<any>this._wrapper).modal({show: false});
            } else {
                this.hide();
            }

            jQuery(parent).prepend(this._wrapper);
        }

        private _wrapper:JQuery;
        private _box:JQuery;
        private _content:JQuery;
        private _header:JQuery;
        private _body:JQuery;
        private _footer:JQuery;
        private _close:JQuery;

        public get box() {
            return this._box;
        }

        public get wrapper() {
            return this._wrapper;
        }

        public get modalContent() {
            return this._content;
        }

        public get modalHeader() {
            return this._header;
        }

        public get modalBody() {
            return this._body;
        }

        public get modalFooter() {
            return this._footer;
        }

        public show():void {
            if (!this._mobile) {
                (<any>this._wrapper).modal("show");
            } else {
                this._wrapper.show();
            }
        }

        public hide():void {
            if (!this._mobile) {
                (<any>this._wrapper).modal("hide");
            } else {
                this._wrapper.hide();
            }
        }

        public get closeButton() {
            return this._close;
        }

        public set closeLabel(label:string) {
            this._close.text(label);
        }

        public get title() {
            return <string>this._header.find(".modal-title").text();
        }

        public set title(title:string) {
            this._header.find(".modal-title").text(title);
        }

        public updateI18n(languageModel:LanguageModel):IviewModalWindow {
            languageModel.translate(this._wrapper);
            return this;
        }

    }
}
