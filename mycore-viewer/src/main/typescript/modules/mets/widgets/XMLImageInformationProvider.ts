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

namespace mycore.viewer.widgets.image {

    export class XMLImageInformationProvider {
        public static getInformation(basePath:string, href: string, callback: (XMLImageInformation) => void, errorCallback: (err) => void = (err) => {
            return;
        }) {

            var settings = {
                url: basePath + href + "/imageinfo.xml",
                async: true,
                success: function(response) {
                    var imageInformation = XMLImageInformationProvider.proccessXML(response, href);
                    callback(imageInformation);
                },
                error: function(request, status, exception) {
                    errorCallback(exception);
                }
            };

            jQuery.ajax(settings);

        }

        private static proccessXML(imageInfo, path:string): XMLImageInformation {
            var node = jQuery(imageInfo.childNodes[0]);
            return new XMLImageInformation(node.attr("derivate"), path, parseInt(node.attr("tiles")), parseInt(node.attr("width")), parseInt(node.attr("height")), parseInt(node.attr("zoomLevel")));
        }

    }

    /**
     * Represents information of a Image
     */
    export class XMLImageInformation {
        constructor(private _derivate: string, private _path: string, private _tiles, private _width: number, private _height: number, private _zoomlevel: number) {
        }

        public get derivate() {
            return this._derivate;
        }

        public get path() {
            return this._path;
        }

        public get tiles() {
            return this._tiles;
        }

        public get width() {
            return this._width;
        }

        public get height() {
            return this._height;
        }

        public get zoomlevel() {
            return this._zoomlevel;
        }

    }


}
