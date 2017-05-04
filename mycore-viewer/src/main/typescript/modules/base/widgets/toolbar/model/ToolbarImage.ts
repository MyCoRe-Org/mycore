/// <reference path="../../../Utils.ts" />
/// <reference path="ToolbarComponent.ts" />

namespace mycore.viewer.widgets.toolbar {

    export class ToolbarImage extends ToolbarComponent {
        constructor(id: string, href: string) {
            super(id);
            this.addProperty(new ViewerProperty<string>(this, "href", href));
        }

        public get href(): string {
            return this.getProperty("href").value;
        }

        public set href(href: string) {
            this.getProperty("href").value = href;
        }
    }

}