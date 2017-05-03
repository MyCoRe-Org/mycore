namespace mycore.viewer.widgets.toolbar {

    export class BootstrapLargeDropdownView implements DropdownView {

        constructor(private _id:string) {
            this._buttonElement = jQuery("<select></select>");
            this._buttonElement.addClass("btn btn-default navbar-btn dropdown");
            this._childMap = new MyCoReMap<string, JQuery>();
        }

        private _buttonElement: JQuery;

        public updateButtonLabel(label:string):void {
        }

        public updateButtonTooltip(tooltip:string):void {
        }

        public updateButtonIcon(icon:string):void {
        }

        public updateButtonClass(buttonClass:string):void {
        }

        public updateButtonActive(active:boolean):void {
        }

        public updateButtonDisabled(disabled:boolean):void {
        }

        private _childMap:MyCoReMap<string, JQuery>;

        public updateChilds(childs:Array<{id:string;label:string
        }>):void {
            this._childMap.forEach(function (key, val) {
                val.remove();
            });
            this._childMap.clear();

            for (var childIndex in childs) {
                var current:{id:string;label:string
                } = childs[childIndex];
                var newChild = jQuery("<option value='" + current.id  + "' data-id=\"" + current.id + "\">" + current.label + "</option>");
                this._childMap.set(current.id, newChild);
                newChild.appendTo(this._buttonElement);
            }
        }

        public getChildElement(id:string):JQuery {
            return this._childMap.get(id) || null;
        }

        public getElement():JQuery {
            return jQuery().add(this._buttonElement).add(this._buttonElement);
        }

    }
}