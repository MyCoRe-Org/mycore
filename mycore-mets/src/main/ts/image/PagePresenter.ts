///<reference path="../model/simple/MCRMetsPage.ts"/>
///<reference path="../model/MetsEditorModel.ts"/>
///<reference path="../model/simple/MCRMetsFile.ts"/>

namespace org.mycore.mets.controller {
    import MCRMetsPage = org.mycore.mets.model.simple.MCRMetsPage;
    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;
    import MCRMetsFile = org.mycore.mets.model.simple.MCRMetsFile;
    export interface PagePresenter {

    }


    export class DefaultPagePresenter implements PagePresenter {

        public init(metsEditorModel: MetsEditorModel) {
            this.urlPattern = metsEditorModel.configuration.imageLocationPattern.replace("{derivate}", metsEditorModel.metsId);
        }

        public getPreviewURL(fileList: Array<MCRMetsFile>) {
            const href = fileList.filter((f) => f.use === "MASTER")[ 0 ].href;
            return this.urlPattern.replace("{image}", href).replace("{quality}", "MID");
        }

        public getImageURL(fileList: Array<MCRMetsFile>) {
            const href = fileList.filter((f) => f.use === "MASTER")[ 0 ].href;
            return this.urlPattern.replace("{image}", href).replace("{quality}", "MAX");
        }

        private urlPattern: string = null;
        public currentPage: MCRMetsPage = null;
    }


}
