module mycore.viewer.components {

    export interface MetadataSettings extends MyCoReViewerSettings {
        objId: string;
        metadataURL: string;
    }

    export class MyCoReMetadataComponent extends ViewerComponent {

        constructor(private _settings:MetadataSettings) {
            super();
        }

        private _container:JQuery;
        private _spinner:JQuery = jQuery("<img />");

        public init() {
            this._container = jQuery("<div></div>");
            this._container.addClass("panel-body");
            if (typeof this._settings.metadataURL != "undefined" && this._settings.metadataURL != null) {
                var metadataUrl = ViewerFormatString(this._settings.metadataURL, {
                    derivateId : this._settings.derivate,
                    objId : this._settings.objId
                });
                this._container.load(metadataUrl, {}, ()=> {
                    /*
                     if the container is scrolled we want to restore the scroll position before this._container was inserted
                     */
                    if (this._container.parent().scrollTop() > 0) {
                        var containerHeightDiff = this._container.height();
                        var parent = this._container.parent();
                        parent.scrollTop(parent.scrollTop() + containerHeightDiff);
                    }
                });
            }

            this.trigger(new events.ComponentInitializedEvent(this));
            this.trigger(new events.WaitForEvent(this, events.ShowContentEvent.TYPE));
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            /* for extra sidebar
             if (e.type == events.ProvideToolbarModelEvent.TYPE) {
             var ptme = <events.ProvideToolbarModelEvent>e;
             ptme.model._dropdownChildren.unshift({id: "metadata", label: "Metadaten"
             });
             ptme.model._sidebarControllDropdownButton.children = ptme.model._dropdownChildren;
             } *

             if (e.type == widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
             var tbpe = <widgets.toolbar.events.DropdownButtonPressedEvent>e;
             if (tbpe.childId == "metadata") {
             this.trigger(new events.ShowContentEvent(this, this._container, events.ShowContentEvent.DIRECTION_WEST));
             }
             }           */


            if (e.type == events.ShowContentEvent.TYPE) {
                var sce = <events.ShowContentEvent>e;

                if (sce.component instanceof MyCoReChapterComponent) {
                    sce.content.prepend(this._container);
                }
            }
        }

        public get handlesEvents():string[] {
            return [ /*events.ProvideToolbarModelEvent.TYPE,*/ events.ShowContentEvent.TYPE /*widgets.toolbar.events.DropdownButtonPressedEvent.TYPE*/ ];
        }
    }
}

addViewerComponent(mycore.viewer.components.MyCoReMetadataComponent);