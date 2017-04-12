namespace org.mycore.mets.model {

    export class IndexSet<T> {
        constructor(private indexingFunction: IndexingFunction<T>) {
            this.privateSetObject = {};
        }

        private privateSetObject: {[index: string]: T; };

        public add(element: T) {
            this.privateSetObject[ this.indexingFunction(element) ] = element;
        }

        public remove(element: T) {
            delete this.privateSetObject[ this.indexingFunction(element) ];
        }

        public getCount(): number {
            let count = 0;
            for (let i in this.privateSetObject) {
                if (this.privateSetObject.hasOwnProperty(i)) {
                    count++;
                }
            }
            return count;
        }

        public has(key: T) {
            return this.indexingFunction(key) in this.privateSetObject;
        }

        public getAllSelected() {
            const arr = new Array<T>();
            for (let i in this.privateSetObject) {
                if (this.privateSetObject.hasOwnProperty(i)) {
                    arr.push(this.privateSetObject[ i ]);
                }
            }
            return arr;
        }
    }

    export interface IndexingFunction<T> {
        (T): string;
    }

}
