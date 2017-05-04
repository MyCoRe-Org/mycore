namespace mycore.viewer.widgets.toolbar {

    export class MobileGroupView {

        constructor(id:string, align:string) {
            this._element = jQuery("<div></div>");
            this._element.addClass("group");
            this._element.attr("data-id", id);
            this._element.css({"float": align});
        }

        private _element:JQuery;

        public addChild(child:JQuery):void {
            this._element.append(child);
        }

        public removeChild(child:JQuery):void {
            child.remove();
        }

        public getElement():JQuery {
            return this._element;
        }
    }
}