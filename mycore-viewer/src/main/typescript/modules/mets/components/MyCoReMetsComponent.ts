/// <reference path="../widgets/IviewMetsProvider.ts" />
/// <reference path="../components/events/MetsLoadedEvent.ts" />

/// <reference path="MetsSettings.ts" />

namespace mycore.viewer.components {

    import ShowContentEvent = mycore.viewer.components.events.ShowContentEvent;
    export class MyCoReMetsComponent extends ViewerComponent {

        constructor(private _settings:MetsSettings, private container:JQuery) {
            super();
        }

        private errorSync = Utils.synchronize<MyCoReMetsComponent>([ (context:MyCoReMetsComponent)=> {
            return context.lm != null && context.error;
        } ], (context:MyCoReMetsComponent)=> {
            new mycore.viewer.widgets.modal.ViewerErrorModal(
                this._settings.mobile,
                context.lm.getTranslation("noMetsShort"),
                context.lm.getFormatedTranslation("noMets", "<a href='mailto:"
                    + this._settings.adminMail + "'>" + this._settings.adminMail + "</a>"),
                this._settings.webApplicationBaseURL + "/modules/iview2/img/sad-emotion-egg.jpg",
                this.container[ 0 ]).show();
            context.trigger(new ShowContentEvent(this, jQuery(), mycore.viewer.widgets.layout.IviewBorderLayout.DIRECTION_WEST, 0));
        });

        private metsAndLanguageSync = Utils.synchronize<MyCoReMetsComponent>([
            (context:MyCoReMetsComponent)=> context.mm != null,
            (context:MyCoReMetsComponent)=> context.lm != null
        ], (context:MyCoReMetsComponent)=> {
            this.metsLoaded(this.mm.model);
            this.trigger(new events.MetsLoadedEvent(this, this.mm));
        });

        private error = false;
        private lm:mycore.viewer.model.LanguageModel = null;
        private mm:{ model:model.StructureModel; document:Document } = null;

        public init() {
            var settings = this._settings;
            if (settings.doctype == "mets") {
                if ((settings.imageXmlPath.charAt(settings.imageXmlPath.length - 1) != '/')) {
                    settings.imageXmlPath = settings.imageXmlPath + "/";
                }

                if ((settings.tileProviderPath.charAt(settings.tileProviderPath.length - 1) != '/')) {
                    settings.tileProviderPath = settings.tileProviderPath + "/";
                }


                var that = this;
                this._metsLoaded = false;
                var tilePathBuilder = (image:string) => {
                    return that._settings.tileProviderPath.split(",")[0] + that._settings.derivate + "/" + image + "/0/0/0.jpg";
                };

                var metsPromise = mycore.viewer.widgets.mets.IviewMetsProvider.loadModel(this._settings.metsURL, tilePathBuilder);
                metsPromise.then((resolved:{ model: model.StructureModel; document: Document }) => {
                    var model = resolved.model;
                    this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));

                    if (model == null) {
                        this.error = true;
                        this.errorSync(this);
                        return;
                    }

                    this.mm = resolved;

                    this.metsAndLanguageSync(this);
                });


                metsPromise.onreject(()=>{
                    this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
                    this.error = true;
                    this.errorSync(this);
                });

                this.trigger(new events.ComponentInitializedEvent(this));
            }
        }

        private _metsLoaded:boolean;
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

        private metsLoaded(structureModel:model.StructureModel) {
            this.postProcessChapter(structureModel._rootChapter);

            var ev = new events.StructureModelLoadedEvent(this, structureModel);
            this.trigger(ev);
            this._metsLoaded = true;
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
                this.metsAndLanguageSync(this);
            }

            return;
        }

    }
}

addViewerComponent(mycore.viewer.components.MyCoReMetsComponent);