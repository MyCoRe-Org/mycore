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


import {LanguageModelLoadedEvent} from "../../base/components/events/LanguageModelLoadedEvent";
import {RequestPermalinkEvent} from "../../base/components/events/RequestPermalinkEvent";
import {ButtonPressedEvent} from "../../base/widgets/toolbar/events/ButtonPressedEvent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {ProvideToolbarModelEvent} from "../../base/components/events/ProvideToolbarModelEvent";
import {MyCoReViewerSettings} from "../../base/MyCoReViewerSettings";
import {ViewerComponent} from "../../base/components/ViewerComponent";
import {ToolbarButton} from "../../base/widgets/toolbar/model/ToolbarButton";
import {MyCoReFrameToolbarModel} from "./model/MyCoReFrameToolbarModel";

export class MyCoReFrameToolbarProviderComponent extends ViewerComponent {

    constructor(private _settings: MyCoReViewerSettings) {
        super();
    }

    private btn: ToolbarButton = null;
    private translation: string = null;

    public get handlesEvents(): string[] {
        return [ButtonPressedEvent.TYPE,
            LanguageModelLoadedEvent.TYPE];
    }

    public init() {
        const frameToolbarModel = new MyCoReFrameToolbarModel();

        if (this._settings.mobile) {
            frameToolbarModel.shrink();
        }

        this.trigger(new ProvideToolbarModelEvent(
            this, frameToolbarModel));
        this.btn = frameToolbarModel.maximizeViewerToolbarButton;
        this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));
    }

    public handle(e: ViewerEvent): void {
        if (e.type == ButtonPressedEvent.TYPE) {
            const bpe = e as ButtonPressedEvent;
            if (bpe.button.id == "MaximizeButton") {
                this.trigger(new RequestPermalinkEvent(this, (permalink) => {
                    window.top.location.assign(permalink);
                }));
            }
        }

        if (e.type === LanguageModelLoadedEvent.TYPE) {
            const lmle = e as LanguageModelLoadedEvent;
            this.translation = lmle.languageModel.getTranslation('toolbar.maximize');
            if (this.translation != null && this.btn != null) {
                this.btn.tooltip = this.translation;
            }
        }
    }
}



