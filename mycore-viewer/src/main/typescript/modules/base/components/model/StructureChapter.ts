module mycore.viewer.model {
    export class StructureChapter {
        constructor(private _parent:StructureChapter,
                    private _type:string,
                    private _id:string,
                    private _order:number,
                    private _label:string,
                    private _chapter:Array<StructureChapter> = new Array<StructureChapter>(),
                    private _additional:MyCoReMap<string, any> = new MyCoReMap<string,any>()) {
        }

        public get parent() {
            return this._parent;
        }

        public get type() {
            return this._type;
        }

        public get id() {
            return this._id;
        }

        public get label() {
            return this._label;
        }

        public get chapter():Array<StructureChapter> {
            return this._chapter;
        }

        public set chapter(chapter:Array<StructureChapter>) {
            this._chapter = chapter;

        }

        public get additional():MyCoReMap<string, any> {
            return this._additional;
        }

    }
}