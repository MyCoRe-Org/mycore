/// <reference path="MetsStructureModel.ts" />
/// <reference path="MetsStructureBuilder.ts" />

namespace mycore.viewer.widgets.mets {
    export class IviewMetsProvider {

        public static loadModel(metsDocumentLocation:string, tilePathBuilder:(href:string)=>string):GivenViewerPromise<{model:model.StructureModel; document:Document}, any> {
            let promise = new ViewerPromise<{model:model.StructureModel; document:Document}, any>();
            let settings = {
                url: metsDocumentLocation,
                success: function (response) {
                    let builder = new MetsStructureBuilder(response, tilePathBuilder);
                    promise.resolve({model : builder.processMets(), document : response});
                },
                error: function (request, status, exception) {
                    promise.reject(exception);
                }
            };
            jQuery.ajax(settings);
            return promise;
        }

    }

}
