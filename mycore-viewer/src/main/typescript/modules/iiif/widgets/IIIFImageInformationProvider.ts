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

    /**
     * Represents information of a Image
     */
    export class IIIFImageInformation {
        constructor(private vPath: string, private vTiles: number,
                    private vWidth: number, private vHeight: number, private vZoomlevel: number) {
        }

        public get path() {
            return this.vPath;
        }

        public get tiles() {
            return this.vTiles;
        }

        public get width() {
            return this.vWidth;
        }

        public get height() {
            return this.vHeight;
        }

        public get zoomlevel() {
            return this.vZoomlevel;
        }
    }

    export class IIIFImageInformationProvider {
        public static getInformation(href: string, callback: (info: IIIFImageInformation)
            => void, errorCallback: (err: string) => void = (err: string) => {
            return;
        }) {
            const settings = {
                url: href + '/info.json',
                async: true,
                success: (response: any) => {
                    const imageInformation = IIIFImageInformationProvider.proccessJSON(response, href);
                    callback(imageInformation);
                },
                error: (request: any, status: string, exception: string) => {
                    errorCallback(exception);
                }
            };

            jQuery.ajax(settings);

        }

        private static proccessJSON(node: any, path: string): IIIFImageInformation {
            const zommLevels = node.tiles[0].scaleFactors;
            const width = node.width;
            const height = node.height;
            let tiles = 0;
            for (let i = 0; i < zommLevels.length + 1; i++) {
                tiles = tiles + (Math.ceil(width / (256 * Math.pow(2, i))) * Math.ceil(height / (256 * Math.pow(2, i))));
            }
            return new IIIFImageInformation(path, tiles, width , height, zommLevels.length);
        }

    }
}
