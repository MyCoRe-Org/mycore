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

///<reference path="IndexSet.ts"/>
namespace org.mycore.mets.model {

    export class IndexMap<T1, T2> {
        constructor(private indexingFunction: IndexingFunction<T1>) {
            this.privateMapObject = {};
        }

        private privateMapObject: {[index: string]: Entry<T1, T2>; };

        public forEach(fn: (k: T1, v: T2) => void) {
            for (let keyString in this.privateMapObject) {
                if (this.privateMapObject.hasOwnProperty(keyString)) {
                    let entry = this.privateMapObject[ keyString ];
                    fn(entry.key, entry.value);
                }
            }
        }

        public put(key: T1, value: T2) {
            this.privateMapObject[ this.indexingFunction(key) ] = new Entry<T1, T2>(key, value);
        }

        public remove(key: T1) {
            delete this.privateMapObject[ this.indexingFunction(key) ];
        }

        public has(key: T1) {
            return this.indexingFunction(key) in this.privateMapObject;
        }

        public get(key: T1) {
            return this.privateMapObject[ this.indexingFunction(key) ].value;
        }

        public getCount(): number {
            let count = 0;
            for (let i in this.privateMapObject) {
                if (this.privateMapObject.hasOwnProperty(i)) {
                    count++;
                }
            }
            return count;
        }
    }

    export class Entry<T1, T2> {
        constructor(public key: T1, public value: T2) {
        }
    }
}
