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

namespace mycore.viewer {

    export class MyCoReViewerSettings {
        mobile: boolean;
        doctype: string;
        tileProviderPath: string;
        filePath: string;
        derivate: string;
        i18nURL: string;
        lang: string;
        webApplicationBaseURL: string;
        derivateURL: string;
        onClose:()=>void;
        adminMail:string;
        leftShowOnStart:string;

        static normalize(settings: MyCoReViewerSettings): MyCoReViewerSettings {
            var parameter = ViewerParameterMap.fromCurrentUrl();

            if (typeof settings.filePath != "undefined" && settings.filePath != null && settings.filePath.charAt(0) == '/') {
                settings.filePath = settings.filePath.substring(1);
            }

            if (typeof settings.derivateURL != "undefined" &&
                settings.derivateURL != null &&
                settings.derivateURL.charAt(settings.derivateURL.length - 1) != '/') {
                settings.derivateURL += "/";
            }

            settings.filePath = encodeURI(settings.filePath);

            if(settings.webApplicationBaseURL.lastIndexOf("/")==settings.webApplicationBaseURL.length-1){
                settings.webApplicationBaseURL = settings.webApplicationBaseURL.substring(0, settings.webApplicationBaseURL.length-1);
            }

            return settings;
        }

    }
}
