namespace mycore.viewer.widgets.events {

    export interface ViewerEvent {
        type:string;
    }


    export class DefaultViewerEvent implements ViewerEvent {
        constructor(private _type:string) {
        }

        public get type() {
            return this._type;
        }

    }
}