module mycore.viewer.components {
    export interface MetsSettings extends MyCoReViewerSettings {
        metsURL: string;
        imageXmlPath: string;
        pageRange: number;
        pdfCreatorURI: string;
        pdfCreatorStyle: string;
    }
}