///<reference path="ModelChange.ts"/>
///<reference path="../model/simple/MCRMetsSection.ts"/>
///<reference path="RemoveSectionLinkChange.ts"/>

namespace org.mycore.mets.model.state {
    export class SectionDeleteChange extends ModelChange {
        constructor(private sectionToDelete: simple.MCRMetsSection) {
            super();
            this.parent = this.sectionToDelete.parent;
            this.deleteLabel = sectionToDelete.label;
            this.parentLabel = this.parent.label;
        }

        private insertPosition: number = null;
        private parent: simple.MCRMetsSection;

        private deleteLabel: string;
        private parentLabel: string;

        private subChanges: Array<ModelChange> = new Array();

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
            const description = messages[ "SectionDeleteDescription" ] || "???SectionDeleteDescription??? {toDelete} {parent}";
            return description.replace("{toDelete}", this.deleteLabel).replace("{parent}", this.parentLabel);
        }
    }
}
