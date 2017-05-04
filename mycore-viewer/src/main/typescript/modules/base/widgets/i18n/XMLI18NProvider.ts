/// <reference path="../../Utils.ts" />
/// <reference path="../../definitions/jquery.d.ts" />
/// <reference path="../../components/model/LanguageModel.ts" />

namespace mycore.viewer.widgets.i18n {

    export class XMLI18NProvider implements I18NProvider {

        private static DEFAULT_ERROR_CALLBACK = (err)=> {
            console.log(err);
            return;
        };

        public getLanguage(href:string, callback:(model:model.LanguageModel)=>void, errorCallback:(err) => void = XMLI18NProvider.DEFAULT_ERROR_CALLBACK) {
            var settings = {
                url: href,
                success: function (response) {
                    callback(new model.LanguageModel(new MyCoReMap<string, string>(response)));
                },
                error: function (request, status, exception) {
                    errorCallback(exception);
                }
            };

            jQuery.ajax(settings);
        }


    }


    export interface I18NProvider {
        getLanguage(href:string, callback:(model:model.LanguageModel)=>void, errorCallback?:(err) => void);
    }


    export var I18NPROVIDER:I18NProvider = new XMLI18NProvider();

}