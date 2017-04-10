///<reference path="simple/MCRMetsSimpleModel.ts"/>
///<reference path="MetsModelLoaderService.ts"/>
///<reference path="MetsEditorConfiguration.ts"/>
///<reference path="MetsEditorModel.ts"/>
///<reference path="MetsEditorParameter.ts"/>

namespace org.mycore.mets.model {

    import MetsModel = org.mycore.mets.model.simple.MCRMetsSimpleModel;

    /**
     * This is the service which loads the mets file and parses it into simple model.
     */
    export class MetsEditorModelFactory {
        constructor(modelLoader: MetsModelLoader, editorConfiguration: MetsEditorConfiguration) {
            this.metsModelLoaderService = modelLoader;
            this.metsEditorConfiguration = editorConfiguration;
        }

        private metsModelLoaderService: MetsModelLoader;
        private metsEditorConfiguration: MetsEditorConfiguration;

        public getInstance(metsEditorParameter: MetsEditorParameter): MetsEditorModel {
            let metsEditorModel = new MetsEditorModel(this.metsEditorConfiguration);

            metsEditorModel.metsId = metsEditorParameter.metsId;
            metsEditorModel.targetServlet = metsEditorParameter.targetServletURL;
            this.metsModelLoaderService.load(metsEditorParameter.sourceMetsURL, (metsModel: MetsModel) => {
                metsEditorModel.onModelLoad(metsModel);
            });

            metsEditorModel.lockURL = metsEditorParameter.lockURL;
            metsEditorModel.unLockURL = metsEditorParameter.unLockURL;

            return metsEditorModel;
        }

    }
}

