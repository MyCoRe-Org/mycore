namespace mycore.viewer.widgets.imagebar {

    export interface ImagebarImage {
        type:string;
        id:string;
        order:number;
        orderLabel:string;
        href:string;
        mimetype:string;
        requestImgdataUrl:(callback:(imgdata:string)=>void)=>void;
    }

    export class ImagebarModel {
        constructor(public images:Array<ImagebarImage>, public selected:ImagebarImage) {
        }

        public _lastPosition:number = 0;
    }
}
