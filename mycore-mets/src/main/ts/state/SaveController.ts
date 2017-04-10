///<reference path="../model/MetsModelSaveService.ts"/>
///<reference path="../model/MetsEditorModel.ts"/>
namespace org.mycore.mets.model.state {

    import MetsModelSave = org.mycore.mets.model.MetsModelSave;

    export class SaveController {

        constructor(public i18nModel, private saveService: MetsModelSave) {

        }

        public init(editorModel: MetsEditorModel) {
            this.metsEditorModel = editorModel;
        }

        private metsEditorModel: MetsEditorModel;

        canSave() {
            return !this.metsEditorModel.stateEngine.isServerState();
        }

        saveClicked() {
            this.saveService.save(this.metsEditorModel.targetServlet, this.metsEditorModel.metsModel,
                (success: boolean) => {
                    if (success) {
                        this.metsEditorModel.stateEngine.markServerState();
                        alert(this.i18nModel.messages[ "save.success" ]);
                    } else {
                        alert(this.i18nModel.messages[ "save.fail" ]);
                    }
                });
        }

    }
}
