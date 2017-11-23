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

namespace mycore.viewer.widgets.tei {
    export class TEILayer implements model.Layer {

        constructor(private _id: string, private _label: string, private mapping: MyCoReMap<string,string>, private contentLocation: string, private teiStylesheet: string) {
        }

        getId():string {
            return this._id;
        }

        getLabel():string {
            return this._label;
        }

        resolveLayer(pageHref:string, callback:(success:boolean, content?:JQuery)=>void):void {
            if (this.mapping.has(pageHref)) {
                var settings:JQueryAjaxSettings = {};

                settings.async = true;
                settings.success = function (data:any, textStatus:string, jqXHR:JQueryXHR) {
                    callback(true,jQuery(data));
                };

                jQuery.ajax(this.contentLocation + this.mapping.get(pageHref) + "?XSL.Style=" + this.teiStylesheet, settings);
            } else {
                callback(false);
            }
        }
    }
}
