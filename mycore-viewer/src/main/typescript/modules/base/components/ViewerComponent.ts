/// <reference path="../widgets/events/ViewerEvent.ts" />
/// <reference path="events/MyCoReImageViewerEvent.ts" />
/// <reference path="../widgets/events/ViewerEventManager.ts" />
/// <reference path="../Utils.ts" />
/// <reference path="events/WaitForEvent.ts" />

namespace mycore.viewer.components {

    export class ViewerComponent extends mycore.viewer.widgets.events.ViewerEventManager {

        constructor() {
            super();
            this._eventCache = new MyCoReMap<string, mycore.viewer.widgets.events.ViewerEvent>();
        }

        private _eventCache: MyCoReMap<string, mycore.viewer.widgets.events.ViewerEvent>;

        public init() {
            console.info("Warning: IviewComponent doesnt implements init " + this);
            return;
        }

        public get handlesEvents(): string[] {
            return [];
        }

        public _handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if(e instanceof events.WaitForEvent){
                var wfe = <events.WaitForEvent>e;

                if(this._eventCache.has(wfe.eventType)) {
                    var cachedEvent = this._eventCache.get(wfe.eventType);
                    wfe.component.handle(cachedEvent);
                }


            }

            this.handle(e);
            return;
        }

        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
        }
        
        public trigger(e: mycore.viewer.widgets.events.ViewerEvent) {
            this._eventCache.set(e.type, e);
            super.trigger(e);
        }

    }
}