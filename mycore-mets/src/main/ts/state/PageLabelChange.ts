///<reference path="ModelChange.ts"/>
namespace org.mycore.mets.model.state {
    export class PageLabelChange extends ModelChange {
        constructor(private page: simple.MCRMetsPage, private to: string, from?: string) {
            super();
            this.from = from || this.page.orderLabel;
        }

        private from: string;

        public doChange() {
            this.page.orderLabel = this.to;
        }

        public unDoChange() {
            this.page.orderLabel = this.from;
        }

        public getDescription(messages: any): string {
            const description = messages[ "PageLabelChangeDescription" ] || "???PageLabelChangeDescription??? {from} {to}";
            return description.replace("{from}", this.from).replace("{to}", this.to);
        }
    }
}
