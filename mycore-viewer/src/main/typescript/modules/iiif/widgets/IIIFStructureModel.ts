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


import {MyCoReMap} from "../../base/Utils";
import {StructureChapter} from "../../base/components/model/StructureChapter";
import {StructureImage} from "../../base/components/model/StructureImage";
import {StructureModel} from "../../base/components/model/StructureModel";

export class IIIFStructureModel extends StructureModel {

    constructor(
        public smLinkMap: MyCoReMap<string, string[]>,
        rootChapter: StructureChapter,
        imageList: StructureImage[],
        chapterToImageMap: MyCoReMap<string, StructureImage>,
        imageToChapterMap: MyCoReMap<string, StructureChapter>,
        imageHrefImageMap: MyCoReMap<string, StructureImage>) {
        super(rootChapter, imageList, chapterToImageMap, imageToChapterMap, imageHrefImageMap, false);
    }

}

