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

namespace mycore.viewer.model {
    export class StructureChapter {
        /**
         * Creates a new structure chapter.
         * @param _parent the parent chapter or null if its the root
         * @param _type the type of the chapter (currently not used)
         * @param _id a unique id
         * @param _label the label of the structure element
         * @param _chapter a array with all child chapters
         * @param _additional a map with additional objects eg. blocklist for the alto plugin
         * @param _destinationResolver this will be used to resolve destination on runtime (better use chapterToImageMap of StructureModel)
         */
        constructor(private _parent:StructureChapter,
                    private _type:string,
                    private _id:string,
                    private _label:string,
                    private _chapter:Array<StructureChapter> = new Array<StructureChapter>(),
                    private _additional: MyCoReMap<string, any> = new MyCoReMap<string,any>(),
                    private _destinationResolver: (callbackFn: (targetId) => void) => void = () => null) {
        }

        public get parent() {
            return this._parent;
        }

        public get type() {
            return this._type;
        }

        public get id() {
            return this._id;
        }

        public get label() {
            return this._label;
        }

        public get chapter():Array<StructureChapter> {
            return this._chapter;
        }

        public set chapter(chapter:Array<StructureChapter>) {
            this._chapter = chapter;

        }

        public get additional():MyCoReMap<string, any> {
            return this._additional;
        }

        public resolveDestination(callbackFn: (targetId) => void): void {
            this._destinationResolver(callbackFn);
        }

    }
}
