///<reference path="IndexSet.ts"/>
namespace org.mycore.mets.model {

    export class IndexMap<T1, T2> {
        constructor(private indexingFunction: IndexingFunction<T1>) {
            this.privateMapObject = {};
        }

        private privateMapObject: {[index: string]: Entry<T1, T2>; };

        public forEach(fn: (k: T1, v: T2) => void) {
            for (let keyString in this.privateMapObject) {
                if (this.privateMapObject.hasOwnProperty(keyString)) {
                    let entry = this.privateMapObject[ keyString ];
                    fn(entry.key, entry.value);
                }
            }
        }

        public put(key: T1, value: T2) {
            this.privateMapObject[ this.indexingFunction(key) ] = new Entry<T1, T2>(key, value);
        }

        public remove(key: T1) {
            delete this.privateMapObject[ this.indexingFunction(key) ];
        }

        public has(key: T1) {
            return this.indexingFunction(key) in this.privateMapObject;
        }

        public get(key: T1) {
            return this.privateMapObject[ this.indexingFunction(key) ].value;
        }

        public getCount(): number {
            let count = 0;
            for (let i in this.privateMapObject) {
                if (this.privateMapObject.hasOwnProperty(i)) {
                    count++;
                }
            }
            return count;
        }
    }

    export class Entry<T1, T2> {
        constructor(public key: T1, public value: T2) {
        }
    }
}
