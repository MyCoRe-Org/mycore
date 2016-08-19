/// <reference path="MetsStructureModel.ts" />
/// <reference path="MetsStructureBuilder.ts" />

module mycore.viewer.widgets.mets {
    export class IviewMetsProvider {

        public static loadModel(metsDocumentLocation:string, tilePathBuilder:(href:string)=>string):GivenViewerPromise<{model:model.StructureModel; metsObject:any}, any> {
            var promise = new ViewerPromise<{model:model.StructureModel; metsObject:any}, any>();
            var settings = {
                url: metsDocumentLocation,
                success: function (response) {
                    var builder = new MetsStructureBuilder(response, tilePathBuilder);
                    promise.resolve({model : builder.processMets(), metsObject : response});
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