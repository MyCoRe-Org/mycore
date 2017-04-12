namespace org.mycore.mets.model.state {
    export class BatchChange extends ModelChange {
        constructor(private changes: Array<ModelChange>) {
            super();
        }

        public getChanges() {
            return this.changes;
        }

        public doChange() {
            this.changes.forEach((change) => {
                change.doChange();
            });
        }

        public unDoChange() {
            this.changes.forEach((change) => {
                change.unDoChange();
            });
        }

        public getDescription(messages: any): string {
            return this.changes.map((change) => change.getDescription(messages)).join("; ");
        }
    }
}
