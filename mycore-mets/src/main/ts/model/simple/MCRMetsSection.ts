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

///<reference path="MCRMetsAltoLink.ts"/>
///<reference path="MCRMetsPage.ts"/>
namespace org.mycore.mets.model.simple {
    export class MCRMetsSection {
        constructor(public id: string,
                    public type: string,
                    public label: string,
                    public metsSectionList: MCRMetsSection[] = [],
                    public parent: MCRMetsSection = null,
                    public linkedPages: MCRMetsPage[] = [],
                    public altoLinks: MCRMetsAltoLink[] = []) {
        }

        public static createRandomId() {
            return 'log_nnnnnn-nnnn-nnnn-nnnnnnnn'.split('n').map((n) => {
                return n + Math.ceil(15 * Math.random()).toString(36);
            }).join('');
        }

        public addSection(section: MCRMetsSection) {
            section.parent = this;
            this.metsSectionList.push(section);
        }

        public removeSection(section: MCRMetsSection) {
            this.metsSectionList.splice(this.metsSectionList.indexOf(section), 1)[ 0 ].parent = null;
        }

        public addSectionBefore(section: MCRMetsSection, before: MCRMetsSection) {
            this.addSectionPosition(section, before, 'before');
        }

        public addSectionAfter(section: MCRMetsSection, after: MCRMetsSection) {
            this.addSectionPosition(section, after, 'after');
        }

        public addSectionIndexPosition(section: MCRMetsSection, index: number) {
            this.metsSectionList.splice(index, 0, section);
            section.parent = this;
        }

        public getJsonObject(): any {
            return {
                id : this.id,
                type : this.type,
                label : this.label,
                metsSectionList : this.metsSectionList.map(s => s.getJsonObject()),
                altoLinks : this.altoLinks.map((al: MCRMetsAltoLink) => {
                    return {altoFile : al.altoFile.id.toString(), begin : al.begin, end : al.end};
                })
            };
        }

        private addSectionPosition(section: MCRMetsSection, sectionToAdd: MCRMetsSection, position: string) {
            const index = this.metsSectionList.indexOf(sectionToAdd);
            if (index === -1) {
                throw new Error(`Cannot insert section  ${section.label} ${position} ${sectionToAdd.label}` +
                    `in parent ${this.label} because ${sectionToAdd.label} is not in this.label`);
            }
            this.metsSectionList.splice(index + (position === 'after' ? 1 : 0), 0, section);
            section.parent = this;
        }

    }
}
