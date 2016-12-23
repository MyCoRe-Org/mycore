///<reference path="ModelChange.ts"/>
///<reference path="../model/simple/MCRMetsSection.ts"/>
///<reference path="../tree/MetsEditorTreeModel.ts"/>

namespace org.mycore.mets.model.state {
    export class SectionMoveChange extends ModelChange {
        constructor(private section: simple.MCRMetsSection, private target: DropTarget) {
            super();
            this.previousParent = section.parent;
            this.previousPositionInParent = this.previousParent.metsSectionList.indexOf(section);
            this.previousParentLabel = this.previousParent.label;
            this.newParentLabel = this.getRealTargetSection().label;
            this.targetLabel = target.element.label;
        }

        private previousParent: simple.MCRMetsSection;
        private previousPositionInParent: number;

        private previousParentLabel: string;
        private newParentLabel: string;
        private targetLabel: string;

        public doChange() {
            const realTargetSection = this.getRealTargetSection();
            this.section.parent.removeSection(this.section);

            switch (this.target.position) {
                case "in":
                    realTargetSection.addSection(this.section);
                    break;
                case "after":
                    realTargetSection.addSectionAfter(this.section, this.target.element);
                    break;
                case "before":
                    realTargetSection.addSectionBefore(this.section, this.target.element);
                    break;
            }
        }

        private getRealTargetSection() {
            return (this.target.position === "after" || this.target.position === "before") ?
                this.target.element.parent : this.target.element;
        }

        public unDoChange() {
            this.section.parent.removeSection(this.section);
            this.previousParent.addSectionIndexPosition(this.section, this.previousPositionInParent);
        }

        public getDescription(messages: any): string {
            const targetPos = messages[ this.target.position ] || this.target.position;
            const description = messages[ "SectionMoveChangeDescription" ] ||
                "???SectionMoveChangeDescription??? {prevParent} {realTarget} {targetPos} {targetLabel}";
            return description
                .replace("{prevParent}", this.previousParentLabel)
                .replace("{realTarget}", this.newParentLabel)
                .replace("{targetPos}", targetPos)
                .replace("{targetLabel}", this.targetLabel);
        }
    }
}
