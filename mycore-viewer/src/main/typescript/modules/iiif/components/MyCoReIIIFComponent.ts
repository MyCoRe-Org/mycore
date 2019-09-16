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

namespace mycore.viewer.components {

    import ShowContentEvent = mycore.viewer.components.events.ShowContentEvent;
    export class MyCoReIIIFComponent extends ViewerComponent {

        constructor(private _settings:IIIFSettings, private container:JQuery) {
            super();
        }

        private errorSync = Utils.synchronize<MyCoReIIIFComponent>([ (context:MyCoReIIIFComponent)=> {
            return context.lm != null && context.error;
        } ], (context:MyCoReIIIFComponent)=> {
            new mycore.viewer.widgets.modal.ViewerErrorModal(
                this._settings.mobile,
                context.lm.getTranslation("noMetsShort"),
                context.lm.getFormatedTranslation("noMets", "<a href='mailto:"
                    + this._settings.adminMail + "'>" + this._settings.adminMail + "</a>"),
                this._settings.webApplicationBaseURL + "/modules/iview2/img/sad-emotion-egg.jpg",
                this.container[ 0 ]).show();
            context.trigger(new ShowContentEvent(this, jQuery(), mycore.viewer.widgets.layout.IviewBorderLayout.DIRECTION_WEST, 0));
        });

        private manifestAndLanguageSync = Utils.synchronize<MyCoReIIIFComponent>([
            (context:MyCoReIIIFComponent)=> context.mm != null,
            (context:MyCoReIIIFComponent)=> context.lm != null
        ], (context:MyCoReIIIFComponent)=> {
            this.manifestLoaded(this.mm.model);
        });

        private error = false;
        private lm:mycore.viewer.model.LanguageModel = null;
        private mm:{ model:model.StructureModel; document:Document } = null;

        public init() {
            var settings = this._settings;
            if (settings.doctype == "manifest") {

                var that = this;
                this._manifestLoaded = false;
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

                    this.manifestAndLanguageSync(this);
                });


                manifestPromise.onreject(()=>{
                    this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
                    this.error = true;
                    this.errorSync(this);
                });

                this.trigger(new events.ComponentInitializedEvent(this));
            }
        }

        private _manifestLoaded:boolean;
        private _eventToTrigger:events.StructureModelLoadedEvent;

        private postProcessChapter(chapter:model.StructureChapter) {
            if (chapter.label == null || typeof chapter.label == "undefined" || chapter.label == "") {
                if (chapter.type != null && typeof chapter.type != "undefined" && chapter.type != "") {
                    let translationKey = this.buildTranslationKey(chapter.type || "");
                    if (this.lm.hasTranslation(translationKey)) {
                        (<any>chapter)._label = this.lm.getTranslation(translationKey);
                    }
                }
            }

            chapter.chapter.forEach((chapter)=> {
                this.postProcessChapter(chapter);
            })
        }

        private buildTranslationKey(type:string) {
            return "dfgStructureSet." + type.replace('- ', '');
        }

        private manifestLoaded(structureModel:model.StructureModel) {
            this.postProcessChapter(structureModel._rootChapter);

            var ev = new events.StructureModelLoadedEvent(this, structureModel);
            this.trigger(ev);
            this._manifestLoaded = true;
            this._eventToTrigger = ev;

            var href = this._settings.filePath;
            var currentImage:model.StructureImage = null;
            structureModel._imageList.forEach((image) => {
                if ("/" + image.href == href || image.href == href) {
                    currentImage = image;
                }
            });

            if (currentImage != null) {
                this.trigger(new events.ImageSelectedEvent(this, currentImage));
            }
        }

        public get handlesEvents():string[] {
            return [ events.LanguageModelLoadedEvent.TYPE ];
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                var languageModelLoadedEvent = <events.LanguageModelLoadedEvent>e;
                this.lm = languageModelLoadedEvent.languageModel;
                this.errorSync(this);
                this.manifestAndLanguageSync(this);
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
