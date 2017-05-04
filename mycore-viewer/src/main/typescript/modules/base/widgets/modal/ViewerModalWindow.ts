/// <reference path="../../Utils.ts" />
/// <reference path="../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.modal {
    export class IviewModalWindow {
        constructor(private _mobile:boolean, _title:string, parent:HTMLElement = document.body) {
            var that = this;

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

            this._header = jQuery("<div><h4 class=\"modal-title\">" + _title + "</h4></div>");
            this._header.addClass("modal-header");
            this._header.appendTo(this._content);

            this._body = jQuery("<div></div>");
            this._body.addClass("modal-body");
            this._body.appendTo(this._content);

            this._footer = jQuery("<div></div>");
            this._footer.addClass("modal-footer");
            this._footer.appendTo(this._content);

            this._close = jQuery("<a>Close</a>");
            this._close.attr("type", "button");
            this._close.addClass("btn btn-default");
            this._close.appendTo(this._footer);

            var that = this;
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
    }
}