namespace mycore.viewer.widgets.toolbar {

    export class BootstrapImageView implements ImageView {

        constructor(id: string) {
            this._element = jQuery("<img />");
            this._element.attr("data-id", id);
        }

        private _element: JQuery;

        updateHref(href: string): void {
            this._element.attr("src", href);
        }
        
        getElement(): JQuery {
            return this._element;
        }

    }

}