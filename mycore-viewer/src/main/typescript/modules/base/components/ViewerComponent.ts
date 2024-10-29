/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import {MyCoReMap} from "../Utils";
import {ViewerEvent} from "../widgets/events/ViewerEvent";
import {ViewerEventManager} from "../widgets/events/ViewerEventManager";
import {WaitForEvent} from "./events/WaitForEvent";

export class ViewerComponent extends ViewerEventManager {

    constructor() {
        super();
        this._eventCache = new MyCoReMap<string, ViewerEvent>();
    }

    private _eventCache: MyCoReMap<string, ViewerEvent>;

    public init() {
        console.info("Warning: IviewComponent doesnt implements init " + this);
        return;
    }

    public get handlesEvents(): string[] {
        return [];
    }

    public _handle(e: ViewerEvent): void {
        if (e instanceof WaitForEvent) {

            if (this._eventCache.has(e.eventType)) {
                const cachedEvent = this._eventCache.get(e.eventType);
                e.component.handle(cachedEvent);
            }
        }

        this.handle(e);
        return;
    }

    public handle(e: ViewerEvent): void {
    }

    public trigger(e: ViewerEvent) {
        this._eventCache.set(e.type, e);
        super.trigger(e);
    }

}
