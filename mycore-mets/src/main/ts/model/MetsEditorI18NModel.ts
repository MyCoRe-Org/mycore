///<reference path="MetsEditorConfiguration.ts"/>

namespace org.mycore.mets.model {
    export function i18n($http: ng.IHttpService,
                         $location: ng.ILocationService,
                         $log: ng.ILogService, editorConfiguration: MetsEditorConfiguration) {
        let metsEditorMessageModel = {messages : new Array()};

        (<any> $http.get(editorConfiguration.i18URL)).success((i18nData) => {
            for (let index in i18nData) {
                if (i18nData.hasOwnProperty(index)) {
                    let betterKey = index;
                    if (index.indexOf("component.mets.editor") === 0) {
                        betterKey = index.substr("component.mets.editor.".length);
                    } else if (index.indexOf("component.mets.dfgStructureSet") === 0) {
                        betterKey = index.substr("component.mets.dfgStructureSet.".length);
                    }
                    metsEditorMessageModel.messages[ betterKey ] = i18nData[ index ];
                }
            }
        });

        return metsEditorMessageModel;
    }
}
