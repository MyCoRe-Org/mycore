module mycore.viewer.model {
    export class MyCoReFrameToolbarModel extends model.MyCoReBasicToolbarModel {
        constructor() {
            super("MyCoReFrameToolbar");
        }


        public addComponents():void {
            this.addGroup(this._sidebarControllGroup);
            this.addGroup(this._zoomControllGroup);
            this.addGroup(this._imageChangeControllGroup);
            //this.addGroup(this._layoutControllGroup);
            //this.addGroup(this._actionControllGroup);
            var logoGroup = this.getGroup("LogoGroup");
            if (typeof  logoGroup != "undefined") {
                this.removeGroup(logoGroup);
            }

            var toolbarButton = new mycore.viewer.widgets.toolbar.ToolbarButton("MaximizeButton", "", "", "fullscreen");
            var toolbarGroup = new mycore.viewer.widgets.toolbar.ToolbarGroup("MaximizeToolbarGroup", true);

            this.addGroup(toolbarGroup);
            toolbarGroup.addComponent(toolbarButton);


        }


    }
}