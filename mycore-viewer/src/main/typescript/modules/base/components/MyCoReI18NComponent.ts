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

import {LanguageModel} from "./model/LanguageModel";
import {MyCoReMap, ViewerError, ViewerFormatString} from "../Utils";
import {ViewerComponent} from "./ViewerComponent";
import {MyCoReViewerSettings} from "../MyCoReViewerSettings";
import {LanguageModelLoadedEvent} from "./events/LanguageModelLoadedEvent";
import {I18NProvider} from "../widgets/i18n/XMLI18NProvider";

/**
 * Overwrite the default implementation of the I18NProvider.
 */
export class MyCoReI18NProvider implements I18NProvider {

    private static DEFAULT_ERROR_CALLBACK = (err) => {
        console.log(err);
        return;
    };

    private static VIEWER_PREFIX = "component.viewer.";
    private static METS_PREFIX = "component.mets.";

    public getLanguage(href: string, callback: (model: LanguageModel) => void, errorCallback: (err) => void = MyCoReI18NProvider.DEFAULT_ERROR_CALLBACK) {
        const settings = {
            url: href,
            dataType: 'json',
            success: function (response) {
                const newResponse = [];
                for (let keyIndex in response) {
                    let prefixEnd = 0;
                    if (keyIndex.indexOf(MyCoReI18NProvider.VIEWER_PREFIX) == 0) {
                        prefixEnd = MyCoReI18NProvider.VIEWER_PREFIX.length;
                    } else if (keyIndex.indexOf(MyCoReI18NProvider.METS_PREFIX) == 0) {
                        prefixEnd = MyCoReI18NProvider.METS_PREFIX.length;
                    }

                    let newKeyIndex = keyIndex.substr(prefixEnd);
                    newResponse[newKeyIndex] = response[keyIndex];
                }

                callback(new LanguageModel(new MyCoReMap<string, string>(newResponse)));
            },
            error: function (request, status, exception) {
                errorCallback(exception);
                callback(new LanguageModel(new MyCoReMap<string, string>()));
            }
        };

        jQuery.ajax(settings);
    }

}

export class MyCoReI18NComponent extends ViewerComponent {

    constructor(private _settings: MyCoReViewerSettings) {
        super();
        this._loadI18N();
    }

    private get _language() {
        return ("lang" in this._settings) ? this._settings.lang : "en";
    }

    private static I18N_PROVIDER = new MyCoReI18NProvider();

    private _loadI18N() {

        if (typeof this._settings.i18nURL == "undefined" || this._settings.i18nURL == null) {
            throw new ViewerError("i18nURL is not specified in settings!");
        }

        MyCoReI18NComponent.I18N_PROVIDER.getLanguage(ViewerFormatString(this._settings.i18nURL, {"lang": this._language}), (languageModel: LanguageModel) => {
            const loadedEvent = new LanguageModelLoadedEvent(this, languageModel);
            this.trigger(loadedEvent);
        });
    }

}

