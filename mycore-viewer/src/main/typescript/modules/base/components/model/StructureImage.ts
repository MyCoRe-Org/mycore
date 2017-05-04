/// <reference path="../../widgets/events/ViewerEventManager.ts" />

namespace mycore.viewer.model {
    export class StructureImage {

        /**
         * Creates a new StructureImage.
         * @param type of image (currently not actively used)
         * @param id
         * @param order
         * @param orderLabel
         * @param href
         * @param mimetype
         * @param requestImgdataUrl
         * @param additionalHrefs
         * @param uniqueIdentifier like a persistent identifier (URN, PURL or DOI)
         */
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
