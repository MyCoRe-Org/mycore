namespace mycore.viewer.widgets.alto {

    export interface AltoChangeSet {
        wordChanges:Array<AltoWordChange>;
        derivateID:string;
    }

    export class AltoChange {
        constructor(public file:string, public type:string,public pageOrder:number){

        }


    }

    export class AltoWordChange extends AltoChange{
        public static TYPE = "AltoWordChange";

        constructor(file: string,
                    public hpos: number,
                    public vpos: number,
                    public width:number,
                    public height:number,
                    public from: string,
                    public to: string,
                    pageOrder:number) {
            super(file, AltoWordChange.TYPE, pageOrder);
        }



    }


}
