
namespace mycore.viewer.components {

    export interface LogoSettings extends MyCoReViewerSettings{
        logoURL: string
    }

    /**
     * A Logo component wich inserts a application specific logo to the Toolbar
     * 1. if you implement your own iview configuration you can add this with setProperty("logoUrl", url);
     * 2. if you use the default configuration you can add MCR.Module-iview2.logoUrl to mycore properties
     */
    export class MyCoReLogoComponent extends ViewerComponent {

        constructor(private _settings: LogoSettings) {
            super();
        }

        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (e.type == events.ProvideToolbarModelEvent.TYPE && !this._settings.mobile) {
                if (this._settings.logoURL) {
                    var logoUrl = this._settings.logoURL;

                    var ptme = <events.ProvideToolbarModelEvent>e;

                    var logoGroup = new widgets.toolbar.ToolbarGroup("LogoGroup", true);
                    var logo = new widgets.toolbar.ToolbarImage("ToolbarImage", logoUrl);

                    logoGroup.addComponent(logo);

                    ptme.model.addGroup(logoGroup);
                }
            }
        }

        public get handlesEvents(): string[] {
            var handleEvents: Array<string> = new Array<string>();
            handleEvents.push(events.ProvideToolbarModelEvent.TYPE);
            return handleEvents;
        }


        public init() {
            this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
        }

    }

}
addViewerComponent(mycore.viewer.components.MyCoReLogoComponent);