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

namespace mycore.viewer.components {

    import ShowContentEvent = mycore.viewer.components.events.ShowContentEvent;
    export class MyCoReComponent extends ViewerComponent {

        constructor(protected _settings:MyCoReViewerSettings, protected container:JQuery) {
            super();
        }

        protected error = false;
        protected lm:mycore.viewer.model.LanguageModel = null;
        protected mm:{ model:model.StructureModel; document:Document } = null;

        protected _structFileLoaded:boolean;
        protected _eventToTrigger:events.StructureModelLoadedEvent;

        protected postProcessChapter(chapter:model.StructureChapter) {
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

        protected buildTranslationKey(type:string) {
            return "dfgStructureSet." + type.replace('- ', '');
        }

        protected structFileLoaded(structureModel:model.StructureModel) {
            this.postProcessChapter(structureModel._rootChapter);

            var ev = new events.StructureModelLoadedEvent(this, structureModel);
            this.trigger(ev);
            this._structFileLoaded = true;
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
    }
}
