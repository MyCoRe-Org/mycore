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

    export class IIIFImageInformationProvider {
        public static getInformation(href: string, callback: (IIIFImageInformation) => void, errorCallback: (err) => void = (err) => {
            return;
        }) {
            var settings = {
                url: href + "/info.json",
                async: true,
                success: function(response) {
                    var imageInformation = IIIFImageInformationProvider.proccessJSON(response, href);
                    callback(imageInformation);
                },
                error: function(request, status, exception) {
                    errorCallback(exception);
                }
            };

            jQuery.ajax(settings);

        }

        private static proccessJSON(node, path:string): IIIFImageInformation {
            var zommLevels = node.tiles[0].scaleFactors;
            var width = node.width;
            var height = node.height;
            var tiles = 0;
            for (let i = 0; i < zommLevels.length + 1; i++) {
                tiles = tiles + (Math.ceil(width / (256 * Math.pow(2, i))) * Math.ceil(height / (256 * Math.pow(2, i))));
            }
            return new IIIFImageInformation(path, tiles, width , height, zommLevels.length);
        }

        // private static getDerivateFromURL(url: string) {
        //     let id = url.substr(url.lastIndexOf("/") + 1);
        //     return id.substring(0, url.indexOf("%2F"));
        // }

    }

    /**
     * Represents information of a Image
     */
    export class IIIFImageInformation {
        constructor(private _path: string, private _tiles, private _width: number, private _height: number, private _zoomlevel: number) {
        }

        // public get derivate() {
        //     return this._derivate;
        // }

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
