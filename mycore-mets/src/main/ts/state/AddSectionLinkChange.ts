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

///<reference path="../tree/TreeController.ts"/>
namespace org.mycore.mets.model.state {
    export class AddSectionLinkChange extends ModelChange {
        private pageLabel: string;
        private sectionLabel: string;
        private parent: simple.MCRMetsSection = null;

        constructor(private section: simple.MCRMetsSection, private page: simple.MCRMetsPage) {
            super();
            this.pageLabel = page.orderLabel;
            this.sectionLabel = this.section.label;
        }

        public findParentWithLink(section: MCRMetsSection = this.section): { pos: number; parentSection: MCRMetsSection } {
            const pos = section.linkedPages.indexOf(this.page);
            if (pos !== -1) {
                return {pos : pos, parentSection : section};
            } else if (section.parent === null) {
                return {pos : -1, parentSection : null};
            } else {
                return this.findParentWithLink(section.parent);
            }
        }

        public doChange() {
            if (this.section.parent !== null) {
                const {pos, parentSection} = this.findParentWithLink(this.section);
                if (parentSection !== null) {
                    this.parent = parentSection;
                    parentSection.linkedPages.splice(pos, 1);
                }
            }
            this.section.linkedPages.push(this.page);
        }

        public unDoChange() {
            this.section.linkedPages.splice(this.section.linkedPages.indexOf(this.page), 1);
            if (this.parent !== null) {
                this.parent.linkedPages.push(this.page);
            }
        }

        public getDescription(messages: any): string {
            return (messages.AddSectionLinkChangeDescription || '???AddSectionLinkChangeDescription???')
                .replace('{pageLabel}', this.pageLabel)
                .replace('{sectionLabel}', this.sectionLabel);
        }
    }
}
