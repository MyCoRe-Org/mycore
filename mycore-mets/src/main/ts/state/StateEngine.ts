///<reference path="ModelChange.ts"/>

namespace org.mycore.mets.model.state {
    export class StateEngine {
        private privateLastChanges: Array<ModelChange> = new Array<ModelChange>();
        private privateRevertedChanges: Array<ModelChange> = new Array<ModelChange>();
        private serverState: ModelChange = null;

        public getLastChanges(): Array<ModelChange> {
            return this.privateLastChanges.slice(0);
        }

        public getRevertedChanges(): Array<ModelChange> {
            return this.privateRevertedChanges.slice(0);
        }

        public changeModel(change: ModelChange) {
            this.clearRevertedChanges();
            change.doChange();
            this.privateLastChanges.push(change);
        }

        public back() {
            if (!this.canBack()) {
                throw "privateLastChanges is empty!";
            } else {
                const lastChange = this.privateLastChanges.pop();
                lastChange.unDoChange();
                this.privateRevertedChanges.push(lastChange);
            }
        }

        public canBack() {
            return this.privateLastChanges.length > 0;
        }


        public forward() {
            if (!this.canForward()) {
                throw "privateRevertedChanges is empty!";
            } else {
                const lastRevertedChange = this.privateRevertedChanges.pop();
                lastRevertedChange.doChange();
                this.privateLastChanges.push(lastRevertedChange);
            }
        }

        public canForward() {
            return this.privateRevertedChanges.length > 0;
        }

        public markServerState() {
            this.serverState = this.getLastChange();
        }

        public isServerState() {
            return this.serverState === this.getLastChange();
        }

        private getLastChange() {
            const lastChanges = this.getLastChanges();
            if (lastChanges.length === 0) {
                return null;
            }
            return lastChanges[ lastChanges.length - 1 ];
        }

        private clearRevertedChanges() {
            if (this.privateRevertedChanges.length > 0) {
                this.privateRevertedChanges = new Array<ModelChange>();
            }
        }
    }
}
