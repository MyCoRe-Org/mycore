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


import {EpubStructureChapter} from "./EpubStructureChapter";
import {StructureChapter} from "../../base/components/model/StructureChapter";

export class EpubStructureBuilder {

    public convertToChapter(item: any, parent?: EpubStructureChapter): EpubStructureChapter {
        const chapters: StructureChapter[] = [];
        const chapter = new EpubStructureChapter(
            parent,
            typeof parent === 'undefined' ? 'root' : '',
            item.label,
            chapters,
            item);

        if (item.subitems != null) {
            item.subitems
                .map((childItem) => {
                    return this.convertToChapter(childItem, chapter);
                })
                .forEach((childChapter) => {
                    chapters.push(childChapter);
                });
        }

        return chapter;
    }

}


