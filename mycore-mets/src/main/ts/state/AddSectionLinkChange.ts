///<reference path="../tree/TreeController.ts"/>
namespace org.mycore.mets.model.state {
    export class AddSectionLinkChange extends ModelChange {
        constructor(private section: simple.MCRMetsSection, private page: simple.MCRMetsPage) {
            super();
            this.pageLabel = page.orderLabel;
            this.sectionLabel = this.section.label;
        }

        private pageLabel: string;
        private sectionLabel: string;
        private parent: simple.MCRMetsSection = null;


        public findParentWithLink(section = this.section): {pos: number; parentSection: MCRMetsSection} {
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
                let {pos, parentSection} = this.findParentWithLink(this.section);
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
            return (messages[ "AddSectionLinkChangeDescription" ] || "???AddSectionLinkChangeDescription???")
                .replace("{pageLabel}", this.pageLabel)
                .replace("{sectionLabel}", this.sectionLabel);
        }

    }
}

