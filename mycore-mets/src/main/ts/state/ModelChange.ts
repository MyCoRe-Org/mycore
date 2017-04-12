namespace org.mycore.mets.model.state {
    export class ModelChange {
        constructor() {
            this.id = this.createRandomId();
            this.date = new Date();
        }

        private id: string;
        private date: Date;


        private createRandomId() {
            return "nnnnnn-nnnn-nnnn-nnnnnnnn".split("n").map((n) => n + Math.ceil(15 * Math.random()).toString(36)).join("");
        }

        public doChange(): void {
            throw "doChange is not implemened!";
        }

        public unDoChange(): void {
            throw "unDoChange is not implemened!";
        }

        public getDescription(messages: any): string {
            throw "getDescription is not implemened!";
        }

        public getDate() {
            return this.date;
        }
    }
}
