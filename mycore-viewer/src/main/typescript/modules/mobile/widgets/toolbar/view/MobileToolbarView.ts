namespace mycore.viewer.widgets.toolbar {
    export class MobileToolbarView implements ToolbarView {

        constructor() {
            this._toolbar = jQuery("<div></div>");
            this._toolbar.addClass("mobile-toolbar")
        }

        private _toolbar: JQuery;

        public addChild(child: JQuery): void {
            this._toolbar.append(child);
        }

        public removeChild(child: JQuery): void {
            child.remove();
        }

        public getElement(): JQuery {
            return this._toolbar;
        }
    }
} 