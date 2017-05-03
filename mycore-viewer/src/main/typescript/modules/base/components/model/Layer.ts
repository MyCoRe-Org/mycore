/// <reference path="../../Utils.ts" />
/// <reference path="StructureImage.ts" />
/// <reference path="../../definitions/jquery.d.ts" />

namespace mycore.viewer.model {
    export interface Layer {
        getLabel():string;
        getId():string;
        resolveLayer(pageHref:string, callback:(success:boolean,content?:JQuery)=>void):void;
    }

}
