/// <reference path="../../../Utils.ts" />
/// <reference path="ToolbarComponent.ts" />

namespace mycore.viewer.widgets.toolbar {

    export class ToolbarText extends ToolbarComponent {
        constructor(id: string, text: string) {
            super(id);
            this.addProperty(new ViewerProperty<string>(this, "text", text));
        }

        public get text(): string {
            return this.getProperty("text").value;
        }

        public set text(text: string) {
            this.getProperty("text").value = text;
        }
    }

}