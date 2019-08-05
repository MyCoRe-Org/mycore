/// <reference path="../widgets/EpubStructureChapter.ts" />
/// <reference path="../widgets/EpubStructureBuilder.ts" />

namespace mycore.viewer.components {

    declare function ePub(a: any, settings?: any): any;

    export class MyCoReEpubDisplayComponent extends ViewerComponent {

        private rendition: any;
        private zoomInPercent: number = 100;
        private currentHref: string;

        constructor(private epubSettings: MyCoReViewerSettings, private appContainer: JQuery) {
            super();
        }

        get handlesEvents() {
            const handlesEvents = [];
            if (this.epubSettings.doctype !== 'epub') {
                return handlesEvents;
            }

            handlesEvents.push(mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE);
            handlesEvents.push(mycore.viewer.components.events.ChapterChangedEvent.TYPE);
            handlesEvents.push(mycore.viewer.components.events.RequestStateEvent.TYPE);
            handlesEvents.push(mycore.viewer.components.events.RestoreStateEvent.TYPE);
            handlesEvents.push(mycore.viewer.components.events.ProvideToolbarModelEvent.TYPE);
            return handlesEvents;
        }

        handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (this.epubSettings.doctype !== 'epub') {
                return;
            }

            if (e.type === mycore.viewer.components.events.ChapterChangedEvent.TYPE) {
                const cce = <mycore.viewer.components.events.ChapterChangedEvent>e;
                this.handleChapterChangedEvent(cce);
                return;
            }

            if (e.type === mycore.viewer.components.events.ProvideToolbarModelEvent.TYPE) {
                const ptme = <mycore.viewer.components.events.ProvideToolbarModelEvent>e;
                this.handleProvideToolbarModelEvent(ptme);
                return;
            }

            if (e.type === mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE) {
                const buttonPressedEvent = <mycore.viewer.widgets.toolbar.events.ButtonPressedEvent>e;
                this.handleButtonPressedEvent(buttonPressedEvent);
                return;
            }

            if (e.type === mycore.viewer.components.events.RequestStateEvent.TYPE) {
                const rse = <mycore.viewer.components.events.RequestStateEvent>e;
                this.handleRequestStateEvent(rse);
                return;
            }

            if (e.type === mycore.viewer.components.events.RestoreStateEvent.TYPE) {
                const rse = <mycore.viewer.components.events.RestoreStateEvent>e;
                if (rse.restoredState.has('start')) {
                    this.rendition.display(rse.restoredState.get('start'));
                }
                return;
            }
        }

        public init() {
            if (this.epubSettings.doctype !== 'epub') {
                return;
            }

            this.trigger(new mycore.viewer.components.events.WaitForEvent(this,
                mycore.viewer.components.events.ProvideToolbarModelEvent.TYPE));

            const content = document.createElement('div');
            content.style.width = '100%';
            content.style.height = '100%';
            content.style.background = 'white';
            content.style.paddingLeft = '5%';

            this.trigger(new mycore.viewer.components.events.ShowContentEvent(this, jQuery(content),
                mycore.viewer.components.events.ShowContentEvent.DIRECTION_CENTER));

            const book = ePub((<any>this.epubSettings).epubPath, {
                openAs: 'directory',
                method: 'goToTheElse!'
            });

            this.rendition = book.renderTo(content, {
                manager: 'continuous',
                flow: 'scrolled',
                width: '100%',
                height: '100%'
            });
            this.rendition.display();

            const resizeBounce = new Debounce<boolean>(100, (b) => {
                this.rendition.resize();
            });
            jQuery(content).bind('iviewResize', () => {
                resizeBounce.call(false);
            });

            const idChapterMap = new MyCoReMap<string, mycore.viewer.model.StructureChapter>();

            book.loaded.navigation.then((toc) => {
                const children = [];
                const root = new widgets.epub.EpubStructureChapter(
                    null,
                    'root',
                    this.epubSettings.filePath,
                    children,
                    null);

                const builder = new widgets.epub.EpubStructureBuilder();
                toc.forEach((item) => {
                    children.push(builder.convertToChapter(item, root));
                });

                const model = new mycore.viewer.model.StructureModel(root, [], new MyCoReMap(), new MyCoReMap(), new MyCoReMap(), false);
                let addToMap: (chapter: mycore.viewer.model.StructureChapter) => void;
                addToMap = (chapter: mycore.viewer.model.StructureChapter) => {
                    idChapterMap.set(chapter.id, chapter);
                    chapter.chapter.forEach((child) => {
                        addToMap(child);
                    });
                };
                addToMap(root);
                this.trigger(new mycore.viewer.components.events.StructureModelLoadedEvent(this, model));
            });

            this.rendition.on('relocated', (section) => {
                //let navigationPoint = book.navigation.get(section.start.href);
                if (section.start.href !== this.currentHref) {
                    this.currentHref = section.start.href;
                    this.trigger(new mycore.viewer.components.events.ChapterChangedEvent(this, idChapterMap.get(section.start.href)));
                }
            });

            this.trigger(new mycore.viewer.components.events.WaitForEvent(this, mycore.viewer.components.events.RestoreStateEvent.TYPE));
        }

        private handleRequestStateEvent(rse: mycore.viewer.components.events.RequestStateEvent) {
            const location = this.rendition.currentLocation();
            rse.stateMap.set('start', location.start.cfi);
            rse.stateMap.set('end', location.end.cfi);
        }

        private handleButtonPressedEvent(buttonPressedEvent: mycore.viewer.widgets.toolbar.events.ButtonPressedEvent) {
            if (buttonPressedEvent.button.id === 'ZoomInButton') {
                this.zoomInPercent += 25;
                this.rendition.themes.fontSize(`${this.zoomInPercent}%`);
            }

            if (buttonPressedEvent.button.id === 'ZoomOutButton') {
                this.zoomInPercent -= 25;
                this.rendition.themes.fontSize(`${this.zoomInPercent}%`);

            }
        }

        private handleProvideToolbarModelEvent(ptme: mycore.viewer.components.events.ProvideToolbarModelEvent) {
            ptme.model._zoomControllGroup.removeComponent(ptme.model._rotateButton);
            ptme.model._zoomControllGroup.removeComponent(ptme.model._zoomFitButton);
            ptme.model._zoomControllGroup.removeComponent(ptme.model._zoomWidthButton);

            ptme.model.removeGroup(ptme.model._imageChangeControllGroup);

            const lcg = ptme.model.getGroup(ptme.model._layoutControllGroup.name);
            if (lcg !== null && typeof lcg !== 'undefined') {
                ptme.model.removeGroup(ptme.model._layoutControllGroup);
            }
        }

        private handleChapterChangedEvent(cce: mycore.viewer.components.events.ChapterChangedEvent) {
            const chapter = <widgets.epub.EpubStructureChapter>cce.chapter;
            this.currentHref = chapter.id;
            if (chapter.epubChapter !== null && cce.component !== this) {
                this.rendition.display(chapter.epubChapter.href);
            }
        }
    }

}

addViewerComponent(mycore.viewer.components.MyCoReEpubDisplayComponent);

