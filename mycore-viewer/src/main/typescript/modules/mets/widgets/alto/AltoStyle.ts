
namespace mycore.viewer.widgets.alto {

    export class AltoStyle {
        // style of alto

        //Attribute der Elemente (die hier genannten MÜSSEN vorliegen)

        constructor(private _id: string,
                    private _fontFamily: string,
                    private _fontSize: number,
                    private _fontStyle: string
        ){
        }

        public getId(): string {
            return this._id;
        }

        public getFontFamily(): string {
            return this._fontFamily;
        }

        public getFontSize(): number {
            return this._fontSize;
        }

        public getFontStyle(): string {
            return this._fontStyle;
        }

    }

}