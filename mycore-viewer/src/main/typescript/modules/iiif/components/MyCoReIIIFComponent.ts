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

/// <reference path="../widgets/IviewIIIFProvider.ts" />

/// <reference path="IIIFSettings.ts" />

/// <reference path="../../base/components/MyCoReComponent.ts" />

namespace mycore.viewer.components {

    import ShowContentEvent = mycore.viewer.components.events.ShowContentEvent;
    export class MyCoReIIIFComponent extends MyCoReComponent {

        constructor(protected _settings:IIIFSettings, protected container:JQuery) {
            super(_settings, container);
        }

        private errorSync = Utils.synchronize<MyCoReIIIFComponent>([ (context:MyCoReIIIFComponent)=> {
            return context.lm != null && context.error;
        } ], (context:MyCoReIIIFComponent)=> {
            new mycore.viewer.widgets.modal.ViewerErrorModal(
                this._settings.mobile,
                context.lm.getTranslation("noManifestShort"),
                context.lm.getFormatedTranslation("noManifest", "<a href='mailto:"
                    + this._settings.adminMail + "'>" + this._settings.adminMail + "</a>"),
                this._settings.webApplicationBaseURL + "/modules/iview2/img/sad-emotion-egg.jpg",
                this.container[ 0 ]).show();
            context.trigger(new ShowContentEvent(this, jQuery(), mycore.viewer.widgets.layout.IviewBorderLayout.DIRECTION_WEST, 0));
        });

        private structFileAndLanguageSync = Utils.synchronize<MyCoReIIIFComponent>([
            (context:MyCoReIIIFComponent)=> context.mm != null,
            (context:MyCoReIIIFComponent)=> context.lm != null
        ], (context:MyCoReIIIFComponent)=> {
            this.structFileLoaded(this.mm.model);
        });

        public init() {
            var settings = this._settings;
            if (settings.doctype == "manifest") {

                var that = this;
                this._structFileLoaded = false;
                var tilePathBuilder = (imageUrl: string, width: number, height: number) => {
                    let scaleFactor = this.getScaleFactor(width, height);
                    return imageUrl + "/full/" + Math.floor(width/scaleFactor) + "," + Math.floor(height/scaleFactor) + "/0/default.jpg";
                };

                var manifestPromise = mycore.viewer.widgets.iiif.IviewIIIFProvider.loadModel(this._settings.manifestURL, tilePathBuilder);
                manifestPromise.then((resolved:{ model: model.StructureModel; document: Document }) => {
                    var model = resolved.model;
                    this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));

                    if (model == null) {
                        this.error = true;
                        this.errorSync(this);
                        return;
                    }

                    this.mm = resolved;

                    this.structFileAndLanguageSync(this);
                });


                manifestPromise.onreject(()=>{
                    this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
                    this.error = true;
                    this.errorSync(this);
                });

                this.trigger(new events.ComponentInitializedEvent(this));
            }
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                var languageModelLoadedEvent = <events.LanguageModelLoadedEvent>e;
                this.lm = languageModelLoadedEvent.languageModel;
                this.errorSync(this);
                this.structFileAndLanguageSync(this);
            }

            return;
        }

        private getScaleFactor(width: number, height: number) {
            let largestScaling = Math.min(256 / width , 256 / height); //TODO make smallest size dynamic
            return Math.pow(2, Math.ceil(Math.log(largestScaling) / Math.log(1/2)));
        }

    }
}

addViewerComponent(mycore.viewer.components.MyCoReIIIFComponent);
