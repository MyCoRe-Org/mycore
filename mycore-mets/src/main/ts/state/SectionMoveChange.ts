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
///<reference path="../tree/MetsEditorTreeModel.ts"/>

namespace org.mycore.mets.model.state {
    export class SectionMoveChange extends ModelChange {
        private previousParent: simple.MCRMetsSection;
        private previousPositionInParent: number;
        private previousParentLabel: string;
        private newParentLabel: string;
        private targetLabel: string;

        constructor(private section: simple.MCRMetsSection, private target: DropTarget) {
            super();
            this.previousParent = section.parent;
            this.previousPositionInParent = this.previousParent.metsSectionList.indexOf(section);
            this.previousParentLabel = this.previousParent.label;
            this.newParentLabel = this.getRealTargetSection().label;
            this.targetLabel = target.element.label;
        }

        public doChange() {
            const realTargetSection = this.getRealTargetSection();
            this.section.parent.removeSection(this.section);

            switch (this.target.position) {
                case 'in':
                    realTargetSection.addSection(this.section);
                    break;
                case 'after':
                    realTargetSection.addSectionAfter(this.section, this.target.element);
                    break;
                case 'before':
                    realTargetSection.addSectionBefore(this.section, this.target.element);
            }
        }

        public unDoChange() {
            this.section.parent.removeSection(this.section);
            this.previousParent.addSectionIndexPosition(this.section, this.previousPositionInParent);
        }

        public getDescription(messages: any): string {
            const targetPos = messages[ this.target.position ] || this.target.position;
            const description = messages.SectionMoveChangeDescription ||
                '???SectionMoveChangeDescription??? {prevParent} {realTarget} {targetPos} {targetLabel}';
            return description
                .replace('{prevParent}', this.previousParentLabel)
                .replace('{realTarget}', this.newParentLabel)
                .replace('{targetPos}', targetPos)
                .replace('{targetLabel}', this.targetLabel);
        }

        private getRealTargetSection() {
            return (this.target.position === 'after' || this.target.position === 'before') ?
                this.target.element.parent : this.target.element;
        }
    }
}
