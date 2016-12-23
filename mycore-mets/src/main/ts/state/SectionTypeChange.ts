///<reference path="ModelChange.ts"/>
///<reference path="../model/simple/MCRMetsSection.ts"/>

namespace org.mycore.mets.model.state {
    export class SectionTypeChange extends ModelChange {
        constructor(private section: simple.MCRMetsSection, private to: string, from?: string) {
            super();
            this.from = from || this.section.type;
        }

        private from: string;

        public doChange() {
            this.section.type = this.to;
        }

        public unDoChange() {
            this.section.type = this.from;
        }

        public getDescription(messages: any): string {
            const description = messages[ "SectionTypeChangeDescription" ] ||
                "???SectionTypeChangeDescription??? {from} {to} {obj}";
            return description.replace("{from}", this.from).replace("{to}", this.to).replace("{obj}", this.section.label);
        }
    }
}
