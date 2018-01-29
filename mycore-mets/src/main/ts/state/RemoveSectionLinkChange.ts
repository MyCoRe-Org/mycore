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
///<reference path="../tree/TreeController.ts"/>

namespace org.mycore.mets.model.state {
    export class RemoveSectionLinkChange extends ModelChange {
        private pageLabel: string;
        private sectionLabel: string;
        private addedTo: MCRMetsSection;

        constructor(private section: simple.MCRMetsSection, private page: simple.MCRMetsPage) {
            super();
            this.pageLabel = page.orderLabel;
            this.sectionLabel = this.section.label;
        }

        public doChange() {
            this.section.linkedPages.splice(this.section.linkedPages.indexOf(this.page), 1);

            this.addedTo = this.getRoot(this.section);

            if (this.addedTo !== this.section && this.addedTo !== null) {
                this.addedTo.linkedPages.push(this.page);
            } else {
                this.addedTo = null;
            }
        }

        public unDoChange() {
            this.section.linkedPages.push(this.page);
            if (this.addedTo !== null && typeof this.addedTo !== 'undefined') {
                const linkedPages = this.addedTo.linkedPages;
                linkedPages.splice(linkedPages.indexOf(this.page), 1);
            }
        }

        public getDescription(messages: any): string {
            return (messages.RemoveSectionLinkChangeDescription || '???RemoveSectionLinkChange???')
                .replace('{pageLabel}', this.pageLabel)
                .replace('{sectionLabel}', this.sectionLabel);
        }

        private getRoot(section: MCRMetsSection = this.section) {
            return (section.parent !== null) ? this.getRoot(section.parent) : section;
        }

        private isPageLinked(root: simple.MCRMetsSection, page: simple.MCRMetsPage) {
            const thisLinked = root.linkedPages.indexOf(page);
            if (thisLinked) {
                return true;
            }

            for (const childSection of root.metsSectionList) {
                const childLinked = this.isPageLinked(childSection, page);
                if (childLinked) {
                    return true;
                }
            }

            return false;
        }

    }
}
