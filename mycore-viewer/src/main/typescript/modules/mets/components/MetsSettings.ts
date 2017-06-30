namespace mycore.viewer.components {
    import AltoChangeSet = mycore.viewer.widgets.alto.AltoChangeSet;
    export interface MetsSettings extends MyCoReViewerSettings {
        altoChangePID: string;
        metsURL: string;
        imageXmlPath: string;
        pageRange: number;
        pdfCreatorURI: string;
        pdfCreatorStyle: string;
        pdfCreatorFormatString?:string;
        pdfCreatorRestrictionFormatString?:string;
        teiStylesheet: string;
        altoChanges?:AltoChangeSet;
        altoEditorPostURL?:string;
        altoReviewer?:boolean;
    }
}
