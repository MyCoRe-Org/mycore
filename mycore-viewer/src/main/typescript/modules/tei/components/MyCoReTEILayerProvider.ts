/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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


import {ViewerComponent} from "../../base/components/ViewerComponent";
import {TEISettings} from "./TEISettings";
import {StructureModel} from "../../base/components/model/StructureModel";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {StructureModelLoadedEvent} from "../../base/components/events/StructureModelLoadedEvent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";
import {MyCoReMap} from "../../base/Utils";
import {ProvideLayerEvent} from "../../base/components/events/ProvideLayerEvent";
import {TEILayer} from "../widgets/TEILayer";

export class MyCoReTEILayerProvider extends ViewerComponent {

    constructor(private _settings: TEISettings) {
        super();
        this.contentLocation = this._settings.webApplicationBaseURL + "/servlets/MCRDerivateContentTransformerServlet/" + this._settings.derivate + "/";
    }

    private _model: StructureModel = null;
    private contentLocation = null;

    public init() {
        if (this._settings.doctype == "mets") {
            this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));
        }
    }


    public handle(e: ViewerEvent): void {
        if (e.type == StructureModelLoadedEvent.TYPE) {
            const smle = e as StructureModelLoadedEvent;
            this._model = smle.structureModel;

            const transcriptions = new MyCoReMap<string, string>();
            const translations = new MyCoReMap<string, MyCoReMap<string, string>>();
            const languages = new Array<string>();

            smle.structureModel._imageList.forEach((image) => {
                const additionalHrefs = image.additionalHrefs;
                additionalHrefs.forEach((name, href) => {
                    if (name.indexOf("TEI.") == 0) {
                        const language = name.substr("TEI.".length).toLocaleLowerCase();
                        if (!translations.has(language)) {
                            translations.set(language, new MyCoReMap<string, string>());
                        }

                        const idHrefTranslationMap = translations.get(language);
                        idHrefTranslationMap.set(image.href, href);

                        if (languages.indexOf(language) == -1) {
                            languages.push(language);
                        }
                    }
                });

            });

            if (!transcriptions.isEmpty()) {
                this.trigger(new ProvideLayerEvent(this, new TEILayer("transcription", "layer.transcription", transcriptions, this.contentLocation, this._settings.teiStylesheet || "html")));
            }

            const order = ["de", "en"];

            if (languages.length != 0) {
                languages
                    .sort((l1, l2) => {
                        const l1Order = order.indexOf(l1);
                        const l2Order = order.indexOf(l2);

                        return l1Order - l2Order;
                    })
                    .forEach((language) => {
                        const translationMap = translations.get(language);
                        this.trigger(new ProvideLayerEvent(this, new TEILayer(language, "layer." + language, translationMap, this.contentLocation, this._settings.teiStylesheet || "html")));
                    });
            }

            return;
        }
    }

    public get handlesEvents(): string[] {
        if (this._settings.doctype == "mets") {
            return [StructureModelLoadedEvent.TYPE];
        } else {
            return [];
        }
    }
}
