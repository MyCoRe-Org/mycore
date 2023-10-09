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


import {Utils} from "./Utils";

export class I18N {
    private static DEFAULT_FETCH_LEVEL = 1;

    private static keyObj = {};

    private static fetchKeyHandlerList = {};

    private static currentLanguage: string = null;

    static async translate2(key: string): Promise<string> {
        return new Promise((accept, reject) =>{
            this.translate(key, (translation)=>{
                accept(translation);
            })
        });
    }

    static translate(key: string, callback: (translation: string) => void) {
        let baseUrl: string = Utils.getUploadSettings().webAppBaseURL;
        let resourceUrl = baseUrl + "rsc/locale/translate/" + this.getCurrentLanguage() + "/";

        if (key in I18N.keyObj) {
            callback(I18N.keyObj[key]);
        } else {
            let fetchKey = key;
            if (key.indexOf(".") != -1) {
                fetchKey = key.split('.', I18N.DEFAULT_FETCH_LEVEL).join(".") + "*";
            }

            let wrappedCallback = () => key in I18N.keyObj ? callback(I18N.keyObj[key]) : callback("???" + key + "???");

            if (fetchKey in I18N.fetchKeyHandlerList) {
                I18N.fetchKeyHandlerList[fetchKey].push(wrappedCallback);
            } else {
                I18N.fetchKeyHandlerList[fetchKey] = [wrappedCallback];

                let xhttp = new XMLHttpRequest();
                xhttp.onreadystatechange = () => {
                    if (xhttp.readyState === XMLHttpRequest.DONE && xhttp.status == 200) {
                        let jsonData = JSON.parse(xhttp.response);
                        for (let key in jsonData) {
                            I18N.keyObj[key] = jsonData[key];
                        }

                        for (let index in I18N.fetchKeyHandlerList[fetchKey]) {
                            I18N.fetchKeyHandlerList[fetchKey][index]()
                        }
                        delete I18N.fetchKeyHandlerList[fetchKey];
                    }
                };
                xhttp.open('GET', resourceUrl + fetchKey, true);
                xhttp.send();
            }
        }
    }

    static translateElements(element: HTMLElement) {
        Array.prototype.slice.call(element.querySelectorAll("[data-i18n]")).forEach(childElement => {
            let child = <HTMLElement>childElement;
            let attr = child.getAttribute("data-i18n");
            I18N.translate(attr, translation => {
                child.innerHTML = translation;
            });
        })
    }

    static getCurrentLanguage(): string {
        return window["mcrLanguage"];
    }
}
