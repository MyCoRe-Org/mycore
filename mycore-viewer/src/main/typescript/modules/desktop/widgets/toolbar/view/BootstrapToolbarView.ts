namespace mycore.viewer.widgets.toolbar {
    export class BootstrapToolbarView implements ToolbarView {

        constructor() {
            this._toolbar = jQuery("<section></section>");
            this._toolbar.addClass("navbar navbar-default");
        }

        private _toolbar: JQuery;
//navbar-header
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