namespace mycore.viewer.widgets.alto {

    export interface AltoChangeSet {
        wordChanges:Array<AltoWordChange>;
    }

    export class AltoChange {
        constructor(public file:string, private type:string,private pageOrder:number){

        }

        public getType(){
            return this.type;
        }

        public getPageOrder(){
            return this.pageOrder;
        }
    }

    export class AltoWordChange extends AltoChange{
        static TYPE = "AltoWordChange";

        constructor(file: string,
                    public x: number,
                    public y: number,
                    public from: string,
                    public to: string,
                    pageOrder:number) {
            super(file, AltoWordChange.TYPE, pageOrder);
        }



    }


}
