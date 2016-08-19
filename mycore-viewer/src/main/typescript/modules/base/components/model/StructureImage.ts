/// <reference path="../../widgets/events/ViewerEventManager.ts" />

module mycore.viewer.model {
    export class StructureImage {

        constructor(public type: string,
            public id: string,
            public order: number,
            public orderLabel: string,
            public href: string,
            public mimetype: string,
            public requestImgdataUrl: (callback: (imgdata: string) => void) => void,
            public additionalHrefs = new MyCoReMap<string, string>(),
            uniqueIdentifier?: string) {
            if (typeof uniqueIdentifier == "undefined" || uniqueIdentifier == null || uniqueIdentifier == "") {
                this.uniqueIdentifier = null;
            } else {
                this.uniqueIdentifier = uniqueIdentifier;
            }
        }
        
        public uniqueIdentifier: string


    }
}