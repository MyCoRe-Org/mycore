namespace mycore.viewer.model {
    import DropdownButtonController = mycore.viewer.widgets.toolbar.DropdownButtonController;
    export class MyCoReDesktopToolbarModel extends model.MyCoReBasicToolbarModel {

        constructor(name = "MyCoReDesktopToolbar") {
            super(name);
        }

        public _viewSelectGroup:widgets.toolbar.ToolbarGroup;
        public viewSelectChilds:Array<widgets.toolbar.ToolbarDropdownButtonChild>;
        public viewSelect:widgets.toolbar.ToolbarDropdownButton;

        public addComponents():void {
            this._viewSelectGroup=new widgets.toolbar.ToolbarGroup("viewSelectGroup");

            this.addGroup(this._sidebarControllGroup);
            this.addGroup(this._zoomControllGroup);
            this.addGroup(this._layoutControllGroup);
            this.addGroup(this._viewSelectGroup);
            this.addGroup(this._imageChangeControllGroup);
            this.addGroup(this._actionControllGroup);
            this.addGroup(this._closeViewerGroup);
            this.addGroup(this._searchGroup);
        }


        public addViewSelectButton():void {
            this.viewSelectChilds = new Array<widgets.toolbar.ToolbarDropdownButtonChild>();

            this.viewSelectChilds.push({
                id: "imageView",
                label: "imageView"
            });

            this.viewSelectChilds.push({
                id: "mixedView",
                label: "mixedView"
            });

            this.viewSelectChilds.push({
                id: "textView",
                label: "textView"
            });

            this.viewSelect = new widgets.toolbar.ToolbarDropdownButton("viewSelect", "viewSelect", this.viewSelectChilds, "eye-open");
            this._viewSelectGroup.addComponent(this.viewSelect);
        }


    }
}
