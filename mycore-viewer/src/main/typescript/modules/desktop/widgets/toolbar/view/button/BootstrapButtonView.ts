namespace mycore.viewer.widgets.toolbar {

    export class BootstrapButtonView implements ButtonView {

        constructor(id: string) {
            this._buttonElement = jQuery("<button></button>");
            this._buttonElement.attr("data-id", id);
            this._buttonElement.addClass("btn btn-default navbar-btn");

            this._buttonLabel = jQuery("<span></span>");
            this._buttonLabel.appendTo(this._buttonElement);

            this._icon = jQuery("<i></i>");
            this._attached = false;
            this._lastIconClass = "";
            this._lastButtonClass = null;
        }

        public _buttonElement: JQuery;
        private _icon: JQuery;
        private _attached: boolean;
        private _buttonLabel: JQuery;
        private _lastIconClass: string;
        private _lastButtonClass: string;

        public updateButtonLabel(label: string): void { 
            this._buttonLabel.text(label);
            if (label.length > 0) {
                this._icon.addClass("textpresent");
            } else {
                this._icon.removeClass("textpresent");
            }
        }
        public updateButtonTooltip(tooltip: string): void {
            this._buttonElement.attr("title", tooltip);
        }

        public updateButtonIcon(icon: string): void {
            if (!this._attached && icon != null) {
                this._icon.prependTo(this._buttonElement);
                this._attached = true;
            } else if (this._attached && icon == null) {
                this._icon.remove();
                this._attached = false;
                return;
            }

            this._icon.removeClass("glyphicon-" + this._lastIconClass);
            this._icon.removeClass(this._lastIconClass);
            this._icon.removeClass("icon-" + this._lastIconClass);

            if (icon.indexOf("fa") == 0) {
                this._icon.addClass("fa");
                this._icon.addClass(icon);
            } else {
                this._icon.addClass("glyphicon");
                this._icon.addClass("glyphicon-" + icon);
                this._icon.addClass("icon-" + icon);
            }

            this._lastIconClass = icon;
        }

        updateButtonClass(buttonClass: string): void {
            if (this._lastButtonClass != null) {
                this._buttonElement.removeClass("btn-" + this._lastButtonClass);
            }

            this._buttonElement.addClass("btn-" + buttonClass);
            this._lastButtonClass = buttonClass;
        }

        updateButtonActive(active: boolean): void {
            var isActive = this._buttonElement.hasClass("active");

            if (active && !isActive) {
                this._buttonElement.addClass("active");
            }

            if (!active && isActive) {
                this._buttonElement.removeClass("active");
            }
        }

        updateButtonDisabled(disabled: boolean): void {
            var isDisabled = this._buttonElement.attr("disabled") == "disabled";

            if (disabled && !isDisabled) {
                this._buttonElement.attr("disabled", "disabled");
            }

            if (!disabled && isDisabled) {
                this._buttonElement.removeAttr("disabled");
            }
        }

        public static getBootstrapIcon(icon: string): string {
            return "glyphicon-" + icon;
        }

        public getElement(): JQuery {
            return this._buttonElement;
        }
    }

}
