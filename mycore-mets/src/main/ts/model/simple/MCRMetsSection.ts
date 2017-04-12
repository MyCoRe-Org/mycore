///<reference path="MCRMetsAltoLink.ts"/>
///<reference path="MCRMetsPage.ts"/>
namespace org.mycore.mets.model.simple {
    export class MCRMetsSection {
        constructor(public id: string,
                    public type: string,
                    public label: string,
                    public metsSectionList: Array<MCRMetsSection> = new Array<MCRMetsSection>(),
                    public parent: MCRMetsSection = null,
                    public linkedPages: Array<MCRMetsPage> = new Array<MCRMetsPage>(),
                    public altoLinks: Array<MCRMetsAltoLink> = new Array<MCRMetsAltoLink>()) {
        }

        public static createRandomId() {
            return "nnnnnn-nnnn-nnnn-nnnnnnnn".split("n").map((n) => {
                return n + Math.ceil(15 * Math.random()).toString(36);
            }).join("");
        }

        public addSection(section: MCRMetsSection) {
            section.parent = this;
            this.metsSectionList.push(section);
        }

        public removeSection(section: MCRMetsSection) {
            this.metsSectionList.splice(this.metsSectionList.indexOf(section), 1)[ 0 ].parent = null;
        }

        public addSectionBefore(section: MCRMetsSection, before: MCRMetsSection) {
            this.addSectionPosition(section, before, "before");
        }

        public addSectionAfter(section: MCRMetsSection, after: MCRMetsSection) {
            this.addSectionPosition(section, after, "after");
        }

        public addSectionIndexPosition(section: MCRMetsSection, index: number) {
            this.metsSectionList.splice(index, 0, section);
            section.parent = this;
        }

        private addSectionPosition(section: MCRMetsSection, sectionToAdd: MCRMetsSection, position: string) {
            const index = this.metsSectionList.indexOf(sectionToAdd);
            if (index === -1) {
                throw `Cannot insert section  ${section.label} ${position} ${sectionToAdd.label}` +
                `in parent ${this.label} because ${sectionToAdd.label} is not in this.label`;
            }
            this.metsSectionList.splice(index + (position === "after" ? 1 : 0), 0, section);
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


    }
}
