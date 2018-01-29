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

///<reference path="../model/utils/IndexSet.ts"/>

namespace org.mycore.mets.model {
    export class MetsEditorTreeModel {
        public root: any = null;
        private indexById: IndexingFunction<{
            id: string
        }> = objectToIdex => objectToIdex.id;
        // If a section is in this set, then it is marked as closed in the tree
        private closeSectionSet: IndexSet<any> = new IndexSet<any>(this.indexById);

        public getElementOpen(treeElement: any): boolean {
            return !this.closeSectionSet.has(treeElement);
        }

        public setElementOpen(section: any, open: boolean): void {
            if (open && this.closeSectionSet.has(section)) {
                this.closeSectionSet.remove(section);
            } else if (!open && !this.closeSectionSet.has(section)) {
                this.closeSectionSet.add(section);
            }
        }
    }

    export class DropTarget {
        constructor(public element: any, public position: string) {
            if (position !== 'after' && position !== 'before' && position !== 'in') {
                throw new Error(`invalid drag and drop target $position $element`);
            }
        }
    }
}
