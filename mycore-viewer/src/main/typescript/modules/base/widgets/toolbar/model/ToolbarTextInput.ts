/// <reference path="../../../Utils.ts" />
/// <reference path="ToolbarComponent.ts" />

namespace mycore.viewer.widgets.toolbar {

    export class ToolbarTextInput extends ToolbarComponent {
        constructor(id: string, value:string, placeHolder: string) {
            super(id);
            this.addProperty(new ViewerProperty<string>(this, "value", value));
            this.addProperty(new ViewerProperty<string>(this, "placeHolder", placeHolder))
        }

        public get value(): string {
            return this.getProperty("value").value;
        }

        public set value(value: string) {
            this.getProperty("value").value = value;
        }

        public get placeHolder():string{
            return this.getProperty("placeHolder").value;
        }

        public set placeHolder(prefillText:string) {
            this.getProperty("placeHolder").value = prefillText;
        }

    }

}
