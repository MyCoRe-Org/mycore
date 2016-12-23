///<reference path="ModelChange.ts"/>
///<reference path="../tree/TreeController.ts"/>

namespace org.mycore.mets.model.state {
    export class RemoveSectionLinkChange extends ModelChange {
        constructor(private section: simple.MCRMetsSection, private page: simple.MCRMetsPage) {
            super();
            this.pageLabel = page.orderLabel;
            this.sectionLabel = this.section.label;
        }

        private pageLabel: string;
        private sectionLabel: string;
        private addedTo: MCRMetsSection;

        private getRoot(section = this.section) {
            return (section.parent !== null) ? this.getRoot(section.parent) : section;
        }

        private isPageLinked(root: simple.MCRMetsSection, page: simple.MCRMetsPage) {
            let thisLinked = root.linkedPages.indexOf(page);
            if (thisLinked) {
                return true;
            }

            for (let si in root.metsSectionList) {
                const childSection = root.metsSectionList[ si ];
                const childLinked = this.isPageLinked(childSection, page);
                if (childLinked) {
                    return true;
                }
            }

            return false;
        }

        public doChange() {
            this.section.linkedPages.splice(this.section.linkedPages.indexOf(this.page), 1);

            this.addedTo = this.getRoot(this.section);

            if (this.addedTo !== this.section && this.addedTo !== null) {
                this.addedTo.linkedPages.push(this.page);
            } else {
                this.addedTo = null;
            }
        }

        public unDoChange() {
            this.section.linkedPages.push(this.page);
            if (this.addedTo !== null && typeof this.addedTo !== "undefined") {
                const linkedPages = this.addedTo.linkedPages;
                linkedPages.splice(linkedPages.indexOf(this.page), 1);
            }
        }

        public getDescription(messages: any): string {
            return (messages[ "RemoveSectionLinkChangeDescription" ] || "???RemoveSectionLinkChange???")
                .replace("{pageLabel}", this.pageLabel)
                .replace("{sectionLabel}", this.sectionLabel);
        }

    }
}

