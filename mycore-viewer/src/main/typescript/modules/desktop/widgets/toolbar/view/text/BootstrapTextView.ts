namespace mycore.viewer.widgets.toolbar {

    export class BootstrapTextView implements TextView{

        constructor(private _id:string) { 
            this.element = jQuery("<p></p>");
            this.element.addClass("navbar-text");
        }
        
        private element: JQuery;

        updateText(text: string): void {
            this.element.text(text);
        }
        getElement(): JQuery {
            return this.element;
        }
    }
}