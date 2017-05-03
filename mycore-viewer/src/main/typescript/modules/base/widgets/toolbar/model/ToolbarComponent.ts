/// <reference path="../../../Utils.ts" />

namespace mycore.viewer.widgets.toolbar {

    export class ToolbarComponent {
        constructor(id: string) {
            this._properties = new MyCoReMap<string, ViewerProperty<any>>();
            this.addProperty(new ViewerProperty(this, "id", id))
        }

        private _properties: MyCoReMap<string, ViewerProperty<any>>;

        public get id() {
            return this.getProperty("id").value;
        }

        public get PropertyNames() {
            return this._properties.keys;
        }

        public addProperty(property: ViewerProperty<any>) {
            this._properties.set(property.name, property);
        }

        public getProperty(name: string) {
            return this._properties.get(name);
        }

        public hasProperty(name: string) {
            return this._properties.has(name);
        }


    }

}