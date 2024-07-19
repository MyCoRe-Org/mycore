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


import {LanguageModel} from "../../components/model/LanguageModel";
import {MyCoReMap} from "../../Utils";

export class XMLI18NProvider implements I18NProvider {

    private static DEFAULT_ERROR_CALLBACK = (err) => {
        console.log(err);
        return;
    };

    public getLanguage(href: string,
                       callback: (model: LanguageModel) => void,
                       errorCallback: (err) => void = XMLI18NProvider.DEFAULT_ERROR_CALLBACK) {
        var settings = {
            url: href,
            success: function (response) {
                callback(new LanguageModel(new MyCoReMap<string, string>(response)));
            },
            error: function (request, status, exception) {
                errorCallback(exception);
            }
        };

        jQuery.ajax(settings);
    }
}


export interface I18NProvider {
    getLanguage(href: string, callback: (model: LanguageModel) => void, errorCallback?: (err) => void): void;
}


