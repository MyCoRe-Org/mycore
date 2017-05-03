namespace mycore.viewer.widgets.toolbar {

    export class MobileButtonView implements ButtonView {

        constructor(id:string) {
            this._buttonElement = jQuery("<a></a>");
            this._buttonElement.addClass("button");
            this._buttonElement.attr("data-id", id);


            this._buttonLabel = jQuery("<span></span>");
            this._buttonLabel.addClass("buttonLabel");
            this._buttonElement.append(this._buttonLabel);

            this._buttonIcon = jQuery("<span></span>");
            this._buttonElement.append(this._buttonIcon);
        }

        public _buttonElement:JQuery;
        private _buttonLabel:JQuery;
        private _buttonIcon:JQuery;
        private _lastIcon = "";

        public updateButtonLabel(label:string):void {
           this._buttonLabel.text(label);
        }

        public updateButtonTooltip(tooltip:string):void {
        }

        public updateButtonIcon(icon:string):void {
            this._buttonIcon.removeClass("fa");
            this._buttonIcon.removeClass("fa-" + this._lastIcon);
            this._buttonIcon.removeClass("icon-" + this._lastIcon);

            this._lastIcon = icon;
            this._buttonIcon.addClass("fa");
            this._buttonIcon.addClass("fa-" + icon);
            this._buttonIcon.addClass("icon-" + icon);

        }

        public updateButtonClass(buttonClass:string):void {
        }

        public updateButtonActive(active:boolean):void {

        }

        public updateButtonDisabled(disabled:boolean):void {

        }


        public getElement():JQuery {
            return this._buttonElement;
        }
    }

}