/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
