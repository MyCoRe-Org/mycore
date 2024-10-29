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


import {ViewerComponent} from "./ViewerComponent";
import {MyCoReViewerSettings} from "../MyCoReViewerSettings";
import {Utils, ViewerFormatString, ViewerParameterMap} from "../Utils";
import {ViewerPermalinkModalWindow} from "../widgets/modal/ViewerPermalinkModalWindow";
import {ComponentInitializedEvent} from "./events/ComponentInitializedEvent";
import {WaitForEvent} from "./events/WaitForEvent";
import {LanguageModelLoadedEvent} from "./events/LanguageModelLoadedEvent";
import {RestoreStateEvent} from "./events/RestoreStateEvent";
import {ProvideToolbarModelEvent} from "./events/ProvideToolbarModelEvent";
import {ViewerEvent} from "../widgets/events/ViewerEvent";
import {ButtonPressedEvent} from "../widgets/toolbar/events/ButtonPressedEvent";
import {RequestStateEvent} from "./events/RequestStateEvent";

import {ImageChangedEvent} from "./events/ImageChangedEvent";
import {RequestPermalinkEvent} from "./events/RequestPermalinkEvent";
import {UpdateURLEvent} from "./events/UpdateURLEvent";


/**
 * permalink.updateHistory: boolean         if true the url will update on image change (default: true)
 * permalink.viewerLocationPattern :string  a patter wich will be used to build the location to the viewer. (default: {baseURL}rsc/viewer/{derivate}/{file})
 *
 */
export class MyCoRePermalinkComponent extends ViewerComponent {

    constructor(private _settings: MyCoReViewerSettings) {
        super();
        this._enabled = Utils.getVar(this._settings, "permalink.enabled", true);
    }

    private _enabled: boolean;
    private _modalWindow: ViewerPermalinkModalWindow;
    private _currentState: ViewerParameterMap = new ViewerParameterMap();

    public init() {
        this.trigger(new ComponentInitializedEvent(this));

        if (this._enabled) {
            this._modalWindow = new ViewerPermalinkModalWindow(this._settings.mobile);
            this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));
            const parameter = ViewerParameterMap.fromCurrentUrl();
            if (!parameter.isEmpty()) {
                this.trigger(new RestoreStateEvent(this, parameter));
            }
        } else {
            this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));
        }
    }


    public handle(e: ViewerEvent) {
        if (e.type == ProvideToolbarModelEvent.TYPE) {
            const ptme = e as ProvideToolbarModelEvent;
            const group = ptme.model.getGroup(ptme.model._actionControllGroup.name);

            if (typeof group != "undefined" && group != null) { // dont need to remove the component if the group is not added.
                ptme.model.getGroup(ptme.model._actionControllGroup.name).removeComponent(ptme.model._shareButton);
            }
        }

        if (e.type == LanguageModelLoadedEvent.TYPE) {
            const languageModelLoadedEvent = e as LanguageModelLoadedEvent;
            this._modalWindow.closeLabel = languageModelLoadedEvent.languageModel.getTranslation("permalink.close");
            this._modalWindow.title = languageModelLoadedEvent.languageModel.getTranslation("permalink.title");
        }

        if (e.type == ButtonPressedEvent.TYPE) {
            const bpe = e as ButtonPressedEvent;
            if (bpe.button.id == "ShareButton") {
                let state = new ViewerParameterMap();
                this.trigger(new RequestStateEvent(this, state, true));
                let permalink = this.buildPermalink(state);
                this._modalWindow.permalink = permalink;
                this._modalWindow.show();
            }
        }

        if (e.type == RequestPermalinkEvent.TYPE) {
            let rpe = e as RequestPermalinkEvent;
            let state = new ViewerParameterMap();
            this.trigger(new RequestStateEvent(this, state, true));
            let permalink = this.buildPermalink(state);
            rpe.callback(permalink);
        }

        if (e.type == ImageChangedEvent.TYPE) {
            let ice = e as ImageChangedEvent;
            if (typeof ice.image != "undefined") {
                this.updateHistory();
            }
        }

        if (e.type == UpdateURLEvent.TYPE) {
            this.updateHistory();
        }
    }

    private updateHistory() {
        const updateHistory = Utils.getVar(this._settings, "permalink.updateHistory", true);
        const state = new ViewerParameterMap();
        this.trigger(new RequestStateEvent(this, state, false));
        if (updateHistory) {
            window.history.replaceState({}, "", this.buildPermalink(state));
        }
    }

    private buildPermalink(state: ViewerParameterMap) {
        let file: string;
        if (this._settings.doctype === 'mets' || this._settings.doctype === 'manifest') {
            file = state.get('page');
            state.remove('page');
        } else {
            file = this._settings.filePath;
        }
        const baseURL = this.getBaseURL(file);
        state.remove('derivate');
        return baseURL + state.toParameterString();
    }

    private getBaseURL(file) {
        const iiif = (location.href.indexOf("/iiif/") > 0) ? "iiif/" : "";
        const pattern = Utils.getVar<string>(this._settings, 'permalink.viewerLocationPattern', '{baseURL}/rsc/viewer/' + iiif + '{derivate}{file}', (p) => p != null);
        return ViewerFormatString(pattern, {
            baseURL: this._settings.webApplicationBaseURL,
            derivate: (file.indexOf('_derivate_') > 0) ? '' : this._settings.derivate + '/',
            file: file
        });
    }

    public get handlesEvents(): string[] {
        const handles = new Array<string>();

        if (this._enabled) {
            handles.push(ButtonPressedEvent.TYPE);
            handles.push(LanguageModelLoadedEvent.TYPE);
            handles.push(RequestPermalinkEvent.TYPE);
            handles.push(ImageChangedEvent.TYPE);
            handles.push(UpdateURLEvent.TYPE);
        } else {
            handles.push(ProvideToolbarModelEvent.TYPE);
        }

        return handles;
    }


}
