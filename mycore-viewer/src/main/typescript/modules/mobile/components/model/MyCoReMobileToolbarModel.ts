namespace mycore.viewer.model {
    export class MyCoReMobileToolbarModel extends MyCoReBasicToolbarModel {
        constructor() {
            super("MyCoReMobileToolbar");
        }


        public addComponents():void {


            this.addGroup(this._sidebarControllGroup);
            this.addGroup(this._zoomControllGroup);

            if (viewerDeviceSupportTouch) {
                this._zoomControllGroup.removeComponent(this._zoomInButton);
                this._zoomControllGroup.removeComponent(this._zoomOutButton);
                this._zoomControllGroup.removeComponent(this._rotateButton);            }

            this.addGroup(this._actionControllGroup);
            this.addGroup(this._closeViewerGroup);

            this.changeIcons();
        }

        public changeIcons():void {
            this._zoomInButton.icon = "search-plus";
            this._zoomOutButton.icon = "search-minus";
            this._zoomFitButton.icon = "expand";
            this._zoomWidthButton.icon = "arrows-h";
            this._closeViewerButton.icon = "power-off";
            this._rotateButton.icon = "rotate-right";
        }

    }
}