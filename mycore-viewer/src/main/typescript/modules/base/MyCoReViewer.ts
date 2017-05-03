/// <reference path="components/ViewerComponent.ts" />
/// <reference path="Utils.ts" />
/// <reference path="definitions/jquery.d.ts" />
/// <reference path="MyCoReViewerSettings.ts" />
/// <reference path="widgets/events/ViewerEvent.ts" />
/// <reference path="components/events/WaitForEvent.ts" />
/// <reference path="components/MyCoReViewerContainerComponent.ts" />
/// <reference path="components/MyCoReToolbarComponent.ts" />
/// <reference path="components/MyCoReImageOverviewComponent.ts" />
/// <reference path="components/MyCoReChapterComponent.ts" />
/// <reference path="components/MyCoRePermalinkComponent.ts" />
/// <reference path="components/MyCoReI18NComponent.ts" />
/// <reference path="components/MyCoReImageScrollComponent.ts" />
/// <reference path="components/MyCoReLayerComponent.ts" />
/// <reference path="components/MyCoReButtonChangeComponent.ts" />

var IVIEW_COMPONENTS:Array<any> = VIEWER_COMPONENTS || [];

namespace mycore.viewer {

    export class MyCoReViewer {

        constructor(private _container:JQuery, private _settings:MyCoReViewerSettings) {
            this._eventHandlerMap = new MyCoReMap<string, Array<mycore.viewer.components.ViewerComponent>>();
            this._components = new Array<mycore.viewer.components.ViewerComponent>();
            this._initializingEvents = new Array<mycore.viewer.widgets.events.ViewerEvent>();

            this.initialize();
        }


        private _eventHandlerMap:MyCoReMap<string, Array<mycore.viewer.components.ViewerComponent>>;
        private _initializing:boolean = false;
        private _initializingEvents:Array<mycore.viewer.widgets.events.ViewerEvent>;
        private _components:Array<mycore.viewer.components.ViewerComponent>;

        public get container() {
            return this._container;
        }

        public addComponent(ic:mycore.viewer.components.ViewerComponent) {
            var that = this;
            ic.bind(function (event:mycore.viewer.widgets.events.ViewerEvent) {
                that.eventTriggered(event);
            });

            var events = ic.handlesEvents;

            if (typeof events != "undefined" && events != null && events instanceof Array) {
                events.push(components.events.WaitForEvent.TYPE)

                for (var eIndex in events) {
                    var e = events[ eIndex ];
                    // initialize the array for the event first
                    if (!this._eventHandlerMap.has(e)) {
                        this._eventHandlerMap.set(e, new Array<mycore.viewer.components.ViewerComponent>());
                    }

                    this._eventHandlerMap.get(e).push(ic);
                }
            } else {
                console.log(ViewerFormatString("The component {comp} doesnt have a valid handlesEvents!", {comp : ic}))
            }

            this._components.push(ic);
            ic.init();
        }

        private eventTriggered(e:mycore.viewer.widgets.events.ViewerEvent) {
            if (this._eventHandlerMap.has(e.type)) {
                var handlers = this._eventHandlerMap.get(e.type);
                for (var componentIndex in handlers) {
                    var component = handlers[ componentIndex ];
                    component._handle(e);
                }
            }
        }

        private initialize():void {
            this._settings = MyCoReViewerSettings.normalize(this._settings);
            if(!this._container.hasClass("mycoreViewer")){
                this._container.addClass("mycoreViewer");
            }

            for (var i in IVIEW_COMPONENTS) {
                var ic = IVIEW_COMPONENTS[ i ];
                try {
                    this.addComponent(new ic(this._settings, this._container));
                } catch (e) {
                    console.log("Unable to add component");
                    console.log(e);
                }
            }

        }

    }

}


addViewerComponent(mycore.viewer.components.MyCoReViewerContainerComponent);
addViewerComponent(mycore.viewer.components.MyCoReI18NComponent);
addViewerComponent(mycore.viewer.components.MyCoReImageOverviewComponent);
addViewerComponent(mycore.viewer.components.MyCoReToolbarComponent);
addViewerComponent(mycore.viewer.components.MyCoReImageScrollComponent);
addViewerComponent(mycore.viewer.components.MyCoReChapterComponent);
addViewerComponent(mycore.viewer.components.MyCoRePermalinkComponent);
addViewerComponent(mycore.viewer.components.MyCoReLayerComponent);
addViewerComponent(mycore.viewer.components.MyCoReButtonChangeComponent);
