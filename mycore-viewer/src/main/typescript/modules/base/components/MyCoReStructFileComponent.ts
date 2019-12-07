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

    export class MyCoReStructFileComponent extends ViewerComponent {

        constructor(protected settings: MyCoReViewerSettings, protected container: JQuery) {
            super();
        }

        protected errorSync: any = Utils.synchronize<MyCoReStructFileComponent>([ (context: MyCoReStructFileComponent) => {
            return context.lm != null && context.error;
        } ], (context: MyCoReStructFileComponent) => {
            new mycore.viewer.widgets.modal.ViewerErrorModal(
                this.settings.mobile,
                context.lm.getTranslation('noStructFileShort'),
                context.lm.getFormatedTranslation('noStructFile', '<a href="mailto:"'
                    + this.settings.adminMail + '>' + this.settings.adminMail + '</a>'),
                this.settings.webApplicationBaseURL + '/modules/iview2/img/sad-emotion-egg.jpg',
                this.container[ 0 ]).show();
            context.trigger(new ShowContentEvent(this, jQuery(), mycore.viewer.widgets.layout.IviewBorderLayout.DIRECTION_WEST, 0));
        });

        protected error: boolean = false;
        protected lm: mycore.viewer.model.LanguageModel = null;
        protected mm: { model: model.StructureModel; document: Document } = null;

        protected vStructFileLoaded: boolean;
        protected vEventToTrigger: events.StructureModelLoadedEvent;

        protected postProcessChapter(chapter: model.StructureChapter) {
            if (chapter.label === null || typeof chapter.label === 'undefined' || chapter.label === '') {
                if (chapter.type !== null && typeof chapter.type !== 'undefined' && chapter.type !== '') {
                    const translationKey = this.buildTranslationKey(chapter.type || '');
                    if (this.lm.hasTranslation(translationKey)) {
                        (<any>chapter)._label = this.lm.getTranslation(translationKey);
                    }
                }
            }

            chapter.chapter.forEach((chap) => {
                this.postProcessChapter(chap);
            });
        }

        protected buildTranslationKey(type: string) {
            return 'dfgStructureSet.' + type.replace('- ', '');
        }

        protected structFileLoaded(structureModel: model.StructureModel) {
            this.postProcessChapter(structureModel._rootChapter);

            const ev = new events.StructureModelLoadedEvent(this, structureModel);
            this.trigger(ev);
            this.vStructFileLoaded = true;
            this.vEventToTrigger = ev;

            const href = this.settings.filePath;
            let currentImage: model.StructureImage = null;
            structureModel._imageList.forEach((image) => {
                if ('/' + image.href === href || image.href === href) {
                    currentImage = image;
                }
            });

            if (currentImage != null) {
                this.trigger(new events.ImageSelectedEvent(this, currentImage));
            }
        }

        public get handlesEvents(): string[] {
            return [ events.LanguageModelLoadedEvent.TYPE ];
        }
    }
}
