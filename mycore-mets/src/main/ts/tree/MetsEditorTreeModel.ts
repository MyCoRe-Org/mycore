///<reference path="../model/utils/IndexSet.ts"/>

namespace org.mycore.mets.model {
    export class MetsEditorTreeModel {
        private indexById: IndexingFunction <{
            id: string
        }> = function (objectToIdex: {id: string}) {
            return objectToIdex.id;
        };

        public root: any = null;

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
            if (position !== "after" && position !== "before" && position !== "in") {
                throw `invalid drag and drop target $position $element`;
            }
        }
    }
}
