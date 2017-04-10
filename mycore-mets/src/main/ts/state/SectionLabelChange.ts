///<reference path="ModelChange.ts"/>
///<reference path="../model/simple/MCRMetsSection.ts"/>

namespace org.mycore.mets.model.state {
    export class SectionLabelChange extends ModelChange {
        constructor(private section: simple.MCRMetsSection, private to: string, from?: string) {
            super();
            this.from = from || this.section.label;
        }

        private from: string;

        public doChange() {
            this.section.label = this.to;
        }

        public unDoChange() {
            this.section.label = this.from;
        }

        public getDescription(messages: any): string {
            let description = messages[ "SectionLabelChangeDescription" ] || "???SectionLabelChangeDescription??? {from} {to}";
            return description.replace("{from}", this.from).replace("{to}", this.to);
        }
    }
}
