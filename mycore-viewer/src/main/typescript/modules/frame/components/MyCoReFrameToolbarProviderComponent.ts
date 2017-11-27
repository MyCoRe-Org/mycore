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

/// <reference path="model/MyCoReFrameToolbarModel.ts" />

namespace mycore.viewer.components {

    export class MyCoReFrameToolbarProviderComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        public get handlesEvents():string[] {
            return [mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE];
        }

        public init() {
            var frameToolbarModel = new mycore.viewer.model.MyCoReFrameToolbarModel();

            this.trigger(new events.ProvideToolbarModelEvent(
                this, frameToolbarModel));
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE) {
                var bpe = <mycore.viewer.widgets.toolbar.events.ButtonPressedEvent>e;
                if (bpe.button.id == "MaximizeButton") {
                    this.trigger(new events.RequestPermalinkEvent(this, (permalink)=> {
                        window.top.location.assign(permalink);
                    }));
                }
            }
        }
    }

}

addViewerComponent(mycore.viewer.components.MyCoReFrameToolbarProviderComponent);
