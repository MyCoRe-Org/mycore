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

///<reference path="ModelChange.ts"/>
///<reference path="../model/simple/MCRMetsSection.ts"/>
///<reference path="RemoveSectionLinkChange.ts"/>

namespace org.mycore.mets.model.state {
    export class SectionDeleteChange extends ModelChange {
        private insertPosition: number = null;
        private parent: simple.MCRMetsSection;
        private deleteLabel: string;
        private parentLabel: string;
        private subChanges: ModelChange[] = [];

        constructor(private sectionToDelete: simple.MCRMetsSection) {
            super();
            this.parent = this.sectionToDelete.parent;
            this.deleteLabel = sectionToDelete.label;
            this.parentLabel = this.parent.label;
        }

        public doChange() {
            // remove child sections first
            this.subChanges = this.sectionToDelete.metsSectionList.map((section) => {
                return new SectionDeleteChange(section);
            });

            // remove links first
            this.subChanges.push(...this.sectionToDelete.linkedPages.map((page) => {
                return new RemoveSectionLinkChange(this.sectionToDelete, page);
            }));

            this.subChanges.forEach(c => c.doChange());

            this.insertPosition = this.sectionToDelete.parent.metsSectionList.indexOf(this.sectionToDelete);
            this.parent.removeSection(this.sectionToDelete);
        }

        public unDoChange() {
            this.parent.addSectionIndexPosition(this.sectionToDelete, this.insertPosition);
            this.subChanges.reverse().forEach(c => c.unDoChange());
        }

        public getDescription(messages: any): string {
            const description = messages.SectionDeleteDescription || '???SectionDeleteDescription??? {toDelete} {parent}';
            return description.replace('{toDelete}', this.deleteLabel).replace('{parent}', this.parentLabel);
        }
    }
}
