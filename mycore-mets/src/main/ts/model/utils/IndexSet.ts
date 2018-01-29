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

namespace org.mycore.mets.model {

    export class IndexSet<T> {
        private privateSetObject: { [index: string]: T; };

        constructor(private indexingFunction: IndexingFunction<T>) {
            this.privateSetObject = {};
        }

        public add(element: T) {
            this.privateSetObject[ this.indexingFunction(element) ] = element;
        }

        public remove(element: T) {
            delete this.privateSetObject[ this.indexingFunction(element) ];
        }

        public getCount(): number {
            let count = 0;
            for (const i in this.privateSetObject) {
                if (this.privateSetObject.hasOwnProperty(i)) {
                    count++;
                }
            }
            return count;
        }

        public has(key: T) {
            return this.indexingFunction(key) in this.privateSetObject;
        }

        public getAllSelected() {
            const arr = [];
            for (const i in this.privateSetObject) {
                if (this.privateSetObject.hasOwnProperty(i)) {
                    arr.push(this.privateSetObject[ i ]);
                }
            }
            return arr;
        }
    }

    export type IndexingFunction<T> = (key: T) => string;

}
