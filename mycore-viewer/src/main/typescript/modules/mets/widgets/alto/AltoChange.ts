namespace mycore.viewer.widgets.alto {

    export interface AltoChangeSet {
        wordChanges:Array<AltoWordChange>;
    }

    export interface AltoWordChange {
        file:string;
        x:number;
        y:number;
        from:string;
        to:string;
    }

}
