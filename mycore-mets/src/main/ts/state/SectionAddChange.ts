///<reference path="ModelChange.ts"/>
///<reference path="../model/simple/MCRMetsSection.ts"/>

namespace org.mycore.mets.model.state {
    export class SectionAddChange extends ModelChange {
        constructor(private sectionToAdd: simple.MCRMetsSection, private parent: simple.MCRMetsSection) {
            super();
            this.label = this.sectionToAdd.label;
            this.parentLabel = this.parent.label;
        }

        private label: string;
        private parentLabel: string;

        public doChange() {
            this.parent.addSection(this.sectionToAdd);
        }

        public unDoChange() {
            this.parent.removeSection(this.sectionToAdd);
        }

        public getDescription(messages: any): string {
            const description = messages[ "SectionAddDescription" ] || "???SectionAddDescription??? {new} {parent}";
            return description.replace("{new}", this.label).replace("{parent}", this.parentLabel);
        }
    }
}
