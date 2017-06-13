///<reference path="MetsEditorConfiguration.ts"/>
///<reference path="../state/StateEngine.ts"/>
///<reference path="simple/MCRMetsSimpleModel.ts"/>

namespace org.mycore.mets.model {
    export class MetsEditorModel {
        constructor(public configuration: MetsEditorConfiguration) {
        }

        public static EDITOR_PAGINATION = "pagination";
        public static EDITOR_STRUCTURING = "structuring";
        public static EDITOR_ASSOCIATION = "association";

        public mode: string = MetsEditorModel.EDITOR_PAGINATION;
        public dataLoaded: boolean = false;
        public metsModel: simple.MCRMetsSimpleModel;
        public metsId: string;
        public middleView: string = "sectionTree";
        public middleViewOptions = [ "sectionTree", "pages" ];
        public pageSelection = {from : null, to : null, lastExpand : "top"};
        public targetServlet: string;
        public lockURL: string;
        public unLockURL: string;
        public locked: boolean = false;

        public stateEngine: org.mycore.mets.model.state.StateEngine = new org.mycore.mets.model.state.StateEngine();

        public onModelLoad(metsSimpleModel: simple.MCRMetsSimpleModel) {
            this.metsModel = metsSimpleModel;
            this.dataLoaded = true;
        }


    }
}
