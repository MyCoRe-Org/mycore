/// <reference path="ViewerComponent.ts" />
/// <reference path="../MyCoReViewerSettings.ts" />
/// <reference path="events/LanguageModelLoadedEvent.ts" />
/// <reference path="model/LanguageModel.ts" />
/// <reference path="../widgets/i18n/XMLI18NProvider.ts" />

namespace mycore.viewer.components {


    /**
     * Overwrite the the default implementation of the I18NProvider.
     */
    export class MyCoReI18NProvider implements mycore.viewer.widgets.i18n.I18NProvider {

        private static DEFAULT_ERROR_CALLBACK = (err)=> {
            console.log(err);
            return;
        };

        private static VIEWER_PREFIX = "component.viewer.";
        private static METS_PREFIX = "component.mets.";

        public getLanguage(href:string, callback:(model:model.LanguageModel)=>void, errorCallback:(err) => void = MyCoReI18NProvider.DEFAULT_ERROR_CALLBACK) {
            var settings = {
                url: href,
                success: function (response) {
                    var newResponse = [];
                    for(var keyIndex in response){
                        var prefixEnd = 0;
                        if (keyIndex.indexOf(MyCoReI18NProvider.VIEWER_PREFIX) == 0) {
                            prefixEnd = MyCoReI18NProvider.VIEWER_PREFIX.length;
                        } else if (keyIndex.indexOf(MyCoReI18NProvider.METS_PREFIX) == 0) {
                            prefixEnd = MyCoReI18NProvider.METS_PREFIX.length;
                        }

                        var newKeyIndex = keyIndex.substr(prefixEnd);
                        newResponse[newKeyIndex] = response[keyIndex];
                    }

                    callback(new model.LanguageModel(new MyCoReMap<string, string>(newResponse)));
                },
                error: function (request, status, exception) {
                    errorCallback(exception);
                    callback(new model.LanguageModel(new MyCoReMap<string, string>()));
                }
            };

            jQuery.ajax(settings);
        }

    }

    {
        mycore.viewer.widgets.i18n.I18NPROVIDER = new MyCoReI18NProvider();
    }

    export class MyCoReI18NComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
            this._loadI18N();
        }

        private _language = ("lang" in this._settings) ? this._settings.lang : "en";

        private _loadI18N() {
            var that = this;

            if (typeof this._settings.i18nURL == "undefined" || this._settings.i18nURL == null) {
                throw new ViewerError("i18nURL is not specified in settings!");
            }
            widgets.i18n.I18NPROVIDER.getLanguage(ViewerFormatString(this._settings.i18nURL, {"lang": this._language}), (languageModel:model.LanguageModel)=> {
                var loadedEvent = new events.LanguageModelLoadedEvent(that,languageModel);
                that.trigger(loadedEvent);
            });
        }

    }
}
