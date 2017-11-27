/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

/// <reference path="../widgets/XMLImageInformationProvider.ts" />
/// <reference path="../widgets/tei/TEILayer.ts" />
/// <reference path="MetsSettings.ts" />
namespace mycore.viewer.components {

    export class MyCoReTEILayerProvider extends ViewerComponent {

        constructor(private _settings:MetsSettings) {
            super();
            this.contentLocation = this._settings.webApplicationBaseURL + "/servlets/MCRDerivateContentTransformerServlet/" + this._settings.derivate + "/";
        }

        private _model:model.StructureModel = null;

        private static TEI_TRANSCRIPTION = "TeiTranscriptionHref";
        private static TEI_TRANSLATION = "TeiTranslationHref";
        private contentLocation = null;

        public init() {
            if (this._settings.doctype == "mets") {
                this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
            }
        }


        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var smle = <events.StructureModelLoadedEvent>e;
                this._model = smle.structureModel;

                var transcriptions = new MyCoReMap<string,string>();
                var translations = new MyCoReMap<string,MyCoReMap<string, string>>();
                var languages = new Array<string>();

                smle.structureModel._imageList.forEach((image)=> {
                    var additionalHrefs = image.additionalHrefs;
                    if (additionalHrefs.has(MyCoReTEILayerProvider.TEI_TRANSCRIPTION)) {
                        transcriptions.set(image.href, additionalHrefs.get(MyCoReTEILayerProvider.TEI_TRANSCRIPTION));
                    }

 
                    additionalHrefs.forEach((name, href)=> {
                        if (name.indexOf(MyCoReTEILayerProvider.TEI_TRANSLATION + ".") == 0) {
                            var language = name.split(".")[ 1 ];
                            if (!translations.has(language)) {
                                translations.set(language, new MyCoReMap<string, string>());
                            }

                            var idHrefTranslationMap = translations.get(language);
                            idHrefTranslationMap.set(image.href, href);

                            if (languages.indexOf(language) == -1) {
                                languages.push(language);
                            }
                        }
                    });

                });

                if (!transcriptions.isEmpty()) {
                    this.trigger(new events.ProvideLayerEvent(this, new widgets.tei.TEILayer("transcription", "transcription", transcriptions, this.contentLocation, this._settings.teiStylesheet || "html")));
                }

                var order = [ "de", "en" ];

                if (languages.length != 0) {
                    languages
                        .sort((l1, l2)=> {
                            var l1Order = order.indexOf(l1);
                            var l2Order = order.indexOf(l2);

                            return l1Order-l2Order;
                        })
                        .forEach((language)=> {
                            var translationMap = translations.get(language);
                            this.trigger(new events.ProvideLayerEvent(this, new widgets.tei.TEILayer("translation_" + language, "translation_" + language, translationMap, this.contentLocation, this._settings.teiStylesheet || "html")));
                        });
                }

                return;
            }
        }

        public get handlesEvents():string[] {
            if (this._settings.doctype == "mets") {
                return [ events.StructureModelLoadedEvent.TYPE ];
            } else {
                return [];
            }
        }
    }


}

addViewerComponent(mycore.viewer.components.MyCoReTEILayerProvider);
