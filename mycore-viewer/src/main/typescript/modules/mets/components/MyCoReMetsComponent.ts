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

/// <reference path="../widgets/IviewMetsProvider.ts" />
/// <reference path="../components/events/MetsLoadedEvent.ts" />

/// <reference path="MetsSettings.ts" />

/// <reference path="../../base/components/MyCoReStructFileComponent.ts" />

namespace mycore.viewer.components {

    export class MyCoReMetsComponent extends MyCoReStructFileComponent {

        constructor(protected settings: MetsSettings, protected container: JQuery) {
            super(settings, container);
        }

        private structFileAndLanguageSync = Utils.synchronize<MyCoReMetsComponent>([
            (context: MyCoReMetsComponent)=> context.mm != null,
            (context: MyCoReMetsComponent)=> context.lm != null
        ], (context: MyCoReMetsComponent)=> {
            this.structFileLoaded(this.mm.model);
            this.trigger(new events.MetsLoadedEvent(this, this.mm));
        });

        public init() {
            let settings = this.settings;
            if (settings.doctype === 'mets') {
                if ((settings.imageXmlPath.charAt(settings.imageXmlPath.length - 1) != '/')) {
                    settings.imageXmlPath = settings.imageXmlPath + '/';
                }

                if ((settings.tileProviderPath.charAt(settings.tileProviderPath.length - 1) != '/')) {
                    settings.tileProviderPath = settings.tileProviderPath + '/';
                }

                const that = this;
                this.vStructFileLoaded = false;
                const tilePathBuilder = (image: string) => {
                    return that.settings.tileProviderPath.split(',')[0] + that.settings.derivate + '/' + image + '/0/0/0.jpg';
                };

                const metsPromise = mycore.viewer.widgets.mets.IviewMetsProvider.loadModel(this.settings.metsURL, tilePathBuilder);
                metsPromise.then((resolved:{ model: model.StructureModel; document: Document }) => {
                    const model = resolved.model;
                    this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));

                    if (model === null) {
                        this.error = true;
                        this.errorSync(this);
                        return;
                    }

                    this.mm = resolved;

                    this.structFileAndLanguageSync(this);
                });


                metsPromise.onreject(() =>{
                    this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
                    this.error = true;
                    this.errorSync(this);
                });

                this.trigger(new events.ComponentInitializedEvent(this));
            }
        }

        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {

            if (e.type === events.LanguageModelLoadedEvent.TYPE) {
                var languageModelLoadedEvent = <events.LanguageModelLoadedEvent>e;
                this.lm = languageModelLoadedEvent.languageModel;
                this.errorSync(this);
                this.structFileAndLanguageSync(this);
            }

            return;
        }

    }
}

addViewerComponent(mycore.viewer.components.MyCoReMetsComponent);
