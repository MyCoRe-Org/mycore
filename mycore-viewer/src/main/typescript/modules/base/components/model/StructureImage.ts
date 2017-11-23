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

/// <reference path="../../widgets/events/ViewerEventManager.ts" />

namespace mycore.viewer.model {
    export class StructureImage {

        /**
         * Creates a new StructureImage.
         * @param type of image (currently not actively used)
         * @param id
         * @param order
         * @param orderLabel
         * @param href
         * @param mimetype
         * @param requestImgdataUrl
         * @param additionalHrefs
         * @param uniqueIdentifier like a persistent identifier (URN, PURL or DOI)
         */
        constructor(public type: string,
            public id: string,
            public order: number,
            public orderLabel: string,
            public href: string,
            public mimetype: string,
            public requestImgdataUrl: (callback: (imgdata: string) => void) => void,
            public additionalHrefs = new MyCoReMap<string, string>(),
            uniqueIdentifier?: string) {
            if (typeof uniqueIdentifier == "undefined" || uniqueIdentifier == null || uniqueIdentifier == "") {
                this.uniqueIdentifier = null;
            } else {
                this.uniqueIdentifier = uniqueIdentifier;
            }
        }
        
        public uniqueIdentifier: string


    }
}
