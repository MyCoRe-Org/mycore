/// <reference path="../widgets/alto/AltoEditorWidget.ts" />
/// <reference path="../widgets/alto/AltoChange.ts" />
namespace mycore.viewer.components {

    import WaitForEvent = mycore.viewer.components.events.WaitForEvent;
    import ShowContentEvent = mycore.viewer.components.events.ShowContentEvent;
    import AltoEditorWidget = mycore.viewer.widgets.alto.AltoEditorWidget;
    import TileImagePage = mycore.viewer.widgets.canvas.TileImagePage;
    import AltoWordChange = mycore.viewer.widgets.alto.AltoWordChange;
    import DesktopInputListener = mycore.viewer.widgets.canvas.DesktopInputListener;
    import DesktopInputAdapter = mycore.viewer.widgets.canvas.DesktopInputAdapter;
    import AbstractPage = mycore.viewer.model.AbstractPage;
    import StructureImage = mycore.viewer.model.StructureImage;

    export class MyCoReAltoEditorComponent extends ViewerComponent {
        private _structureImages: Array<mycore.viewer.model.StructureImage>;
        private _structureModel: mycore.viewer.model.StructureModel;
        private _altoPresent: boolean;
        private _languageModel: mycore.viewer.model.LanguageModel;
        private _toolbarModel: mycore.viewer.model.MyCoReBasicToolbarModel;
        private _sidebarControllDropdownButton: mycore.viewer.widgets.toolbar.ToolbarDropdownButton;
        private _altoDropdownChildItem: { id: string; label: string };
        private container: JQuery;
        private containerTitle: JQuery;
        private editorWidget: mycore.viewer.widgets.alto.AltoEditorWidget;
        private static DROP_DOWN_CHILD_ID = "altoButtonChild";
        private currentOrder: number;
        private currentPageID: string;
        private highlightWordLayer: HighligtAltoWordCanvasPageLayer = new HighligtAltoWordCanvasPageLayer();
        private contentIDImageMap = new MyCoReMap<string, StructureImage>();

        constructor(private _settings: MyCoReViewerSettings, private _container: JQuery) {
            super();
            console.log(this._container);
        }

        private everythingLoadedSynchronize = Utils.synchronize<MyCoReAltoEditorComponent>(
            [
                (obj) => obj._toolbarModel != null,
                (obj) => obj._languageModel != null,
            ],
            (obj) => {
                obj.completeLoaded();
            }
        );

        public init() {
            this.container = jQuery("<div></div>");
            this.containerTitle = jQuery("<span>ALTO-Editor</span>");

            this.trigger(new WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));

            this.container.bind("iviewResize", () => {
                this.updateContainerSize();
            });

        }


        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                let structureModelLodedEvent = <events.StructureModelLoadedEvent>e;
                this._structureModel = structureModelLodedEvent.structureModel;
                this._structureImages = this._structureModel.imageList;
                for (let imageIndex in this._structureImages) {
                    let image = this._structureImages[ imageIndex ];
                    this._altoPresent = this._altoPresent || image.additionalHrefs.has("AltoHref");
                }
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                let lmle = <events.LanguageModelLoadedEvent>e;
                this._languageModel = lmle.languageModel;
                this.everythingLoadedSynchronize(this);
            }

            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                let ptme = <events.ProvideToolbarModelEvent>e;
                this._toolbarModel = ptme.model;
                this._sidebarControllDropdownButton = ptme.model._sidebarControllDropdownButton;
                this.everythingLoadedSynchronize(this);
            }

            if (e.type == widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
                let dbpe = <widgets.toolbar.events.DropdownButtonPressedEvent> e;

                if (dbpe.childId === MyCoReAltoEditorComponent.DROP_DOWN_CHILD_ID) {
                    this.trigger(new ShowContentEvent(this, this.container, ShowContentEvent.DIRECTION_WEST, 400, this.containerTitle));

                } else {
                    // disable all tools here!
                }

            }

            if (e.type == events.ImageChangedEvent.TYPE) {
                let ice = < events.ImageChangedEvent>e;

                if (typeof ice.image != "undefined") {
                    this.trigger(new events.RequestPageEvent(this, ice.image.href, (pageId: string, abstractPage: model.AbstractPage) => {
                        let altoContent = (<TileImagePage>abstractPage).getHTMLContent();
                        if (altoContent.value != null) {
                            this.contentIDImageMap.set(altoContent.value.getAttribute("data-id"), ice.image);
                        } else {
                            altoContent.addObserver({
                                propertyChanged : (old: ViewerProperty<HTMLElement>, _new: ViewerProperty<HTMLElement>) => {
                                    this.contentIDImageMap.set(_new.value.getAttribute("data-id"), ice.image);
                                }
                            });
                        }

                    }));

                }
            }
        }

        public mouseClick(position: Position2D, ev: JQueryMouseEventObject) {

            if (this.isEditing()) {
                let element = <HTMLElement>ev.target;
                let vpos = parseInt(element.getAttribute("data-vpos")),
                    hpos = parseInt(element.getAttribute("data-hpos")),
                    width = parseInt(element.getAttribute("data-width")),
                    height = parseInt(element.getAttribute("data-height"));

                if (!isNaN(vpos) && !isNaN(hpos)) {
                    if (this.currentEditWord !== element) {
                        this.editWord(element, vpos, hpos, width, height);
                        let range = document.createRange();
                        range.selectNodeContents(this.currentEditWord);
                        let selection = window.getSelection();
                        selection.removeAllRanges();
                        selection.addRange(range);
                    }
                    return;
                }
            }
            ev.preventDefault();
        }


        private changes: MyCoReMap<string, AltoWordChange> = new MyCoReMap<string, AltoWordChange>();
        private currentEditWord: HTMLElement = null;

        private static ENTER_KEY = 13;
        private static ESC_KEY = 27;


        private editWord(element: HTMLElement,
                         vpos: number,
                         hpos: number,
                         width: number,
                         height: number) {

            if (this.currentEditWord != null) {
                this.applyEdit(this.currentEditWord, this.currentPageID, this.currentOrder);
            }

            let findPageID = (el: HTMLElement) => {
                return el.getAttribute("data-id") == null ? findPageID(el.parentElement) : el.getAttribute("data-id");
            };

            let pageId = findPageID(element);


            this.currentPageID = pageId;
            this.currentOrder = this.contentIDImageMap.get(pageId).order;
            this.currentEditWord = element;
            this.currentEditWord.setAttribute("contenteditable", "true");
            /*this.currentEditWord.onblur = () => {
             if (this.currentEditWord != null) {
             this.applyEdit(this.currentEditWord, pageId, order);
             }
             };*/

            this.highlightWordLayer.setHighlightedWord({
                vpos : vpos,
                hpos : hpos,
                width : width,
                height : height,
                id : this.currentPageID
            });
            this.trigger(new events.RedrawEvent(this));
        }

        keyDown(e: JQueryKeyEventObject) {
            if (this.currentEditWord != null) {
                if (e.keyCode == MyCoReAltoEditorComponent.ENTER_KEY) {
                    this.applyEdit(this.currentEditWord, this.currentPageID, this.currentOrder);
                    return;
                }

                if (e.keyCode == MyCoReAltoEditorComponent.ESC_KEY) {
                    this.abortEdit(this.currentEditWord);
                    return;
                }

            }
        }

        private abortEdit(element: HTMLElement) {
            let word = element.getAttribute("data-word");

            element.innerText = word;

            this.endEdit(element);
        }

        private applyEdit(element: HTMLElement, pageId: string, order: number) {


            let vpos = parseInt(element.getAttribute("data-vpos")),
                hpos = parseInt(element.getAttribute("data-hpos")),
                newWord = element.innerHTML;

            let key = `${pageId}_${vpos}_${hpos}`;

            let oldWord = element.getAttribute("data-word");

            if (!this.changes.has(key)) {
                if (oldWord !== newWord) {

                    let wordChange = new AltoWordChange(
                        pageId,
                        hpos,
                        vpos,
                        oldWord,
                        newWord,
                        order
                    );
                    this.changes.set(key, wordChange);
                    this.editorWidget.addChange(wordChange);
                    element.classList.add("edited")
                }
            } else {
                let wordChange = this.changes.get(key);

                if (oldWord !== newWord) {
                    wordChange.to = newWord;
                    this.editorWidget.updateChange(wordChange);
                } else {
                    this.changes.remove(key);
                    this.editorWidget.removeChange(wordChange);
                    if (element.classList.contains("edited")) {
                        element.classList.remove("edited");
                    }
                }
            }

            this.endEdit(element);
        }

        private endEdit(element: HTMLElement) {
            this.currentEditWord = this.currentOrder = this.currentPageID = null;
            element.setAttribute("contenteditable", "false");
            this.highlightWordLayer.setHighlightedWord(null);
            viewerClearTextSelection();
            this.trigger(new events.RedrawEvent(this));
        }

        public get handlesEvents(): string[] {
            return [
                events.StructureModelLoadedEvent.TYPE,
                events.LanguageModelLoadedEvent.TYPE,
                events.ProvideToolbarModelEvent.TYPE,
                widgets.toolbar.events.DropdownButtonPressedEvent.TYPE,
                events.ImageChangedEvent.TYPE
            ];
        }

        private toggleEditWord(enable: boolean = !this.editorWidget.changeWordButton.hasClass("active")) {
            let button = this.editorWidget.changeWordButton;
            if (enable) {
                button.addClass("active");
            } else {
                button.removeClass("active");
                if (this.currentEditWord != null) {
                    this.endEdit(this.currentEditWord);
                }
            }
            this.trigger(new events.TextEditEvent(this, enable));
            this.trigger(new events.RedrawEvent(this));
        }

        private isEditing() {
            return this.editorWidget.changeWordButton.hasClass("active");
        }


        private completeLoaded() {
            this._altoDropdownChildItem = {
                id : MyCoReAltoEditorComponent.DROP_DOWN_CHILD_ID,
                label : this._languageModel.getTranslation("altoEditor")
            };
            this._sidebarControllDropdownButton.children.push(this._altoDropdownChildItem);
            this._sidebarControllDropdownButton.children = this._sidebarControllDropdownButton.children;
            this.editorWidget = new AltoEditorWidget(this.container, this._languageModel);
            this.containerTitle.text(`${this._languageModel.getTranslation("containerTitle")}`);

            this.editorWidget.changeWordButton.click((ev) => {
                this.toggleEditWord();
            });

            this.editorWidget.addChangeClickedEventHandler((change) => {
                this.trigger(new events.ImageSelectedEvent(this, this._structureModel.imageHrefImageMap.get(change.file)));

            });

            this.trigger(new events.AddCanvasPageLayerEvent(this, 2, this.highlightWordLayer));
            this.trigger(new events.RequestDesktopInputEvent(this, new EditAltoInputListener(this)));


            this.updateContainerSize();
        }

        private updateContainerSize() {
            this.container.css({
                "height" : (this.container.parent().height() - this.containerTitle.parent().outerHeight()) + "px",
                "overflow-y" : "scroll"
            });
        }


        drag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D, e: JQueryMouseEventObject) {
            if (e.target !== this.currentEditWord) {
                e.preventDefault();
            }
        }

        mouseDown(position: Position2D, e: JQueryMouseEventObject) {
            if (e.target !== this.currentEditWord) {
                e.preventDefault();
            }
        }
    }

    export class EditAltoInputListener extends DesktopInputAdapter {

        constructor(private editAltoComponent: MyCoReAltoEditorComponent) {
            super();
        }

        mouseDown(position: Position2D, e: JQueryMouseEventObject): void {
            this.editAltoComponent.mouseDown(position, e);
        }

        mouseUp(position: Position2D, e: JQueryMouseEventObject) {
        }

        mouseMove(position: Position2D, e: JQueryMouseEventObject) {


        }

        mouseClick(position: Position2D, e: JQueryMouseEventObject) {
            this.editAltoComponent.mouseClick(position, e);
        }

        mouseDoubleClick(position: Position2D, e: JQueryMouseEventObject): void {
        }

        keydown(e: JQueryKeyEventObject): void {
            this.editAltoComponent.keyDown(e);
        }

        mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
                  e: JQueryMouseEventObject): void {

            this.editAltoComponent.drag(currentPosition, startPosition, startViewport,
                e);
        }
    }

    export interface HighlightedWord {
        hpos: number;
        vpos: number;
        width: number;
        height: number;
        id: string;
    }

    export class HighligtAltoWordCanvasPageLayer implements widgets.canvas.CanvasPageLayer {

        private static EDIT_HIGHLIGHT_COLOR = "#90EE90";

        private highlightedWord: HighlightedWord = null;

        public getHighlightedWord() {
            return this.highlightedWord;
        }

        public setHighlightedWord(word: HighlightedWord) {
            this.highlightedWord = word;
        }

        draw(ctx: CanvasRenderingContext2D, id: string, pageSize: Size2D, drawOnHtml: boolean): void {
            if (drawOnHtml) {
                return;
            }

            if (this.highlightedWord != null && id == this.highlightedWord.id) {
                ctx.save();
                {
                    let width = 5 * window.devicePixelRatio;

                    let gap = width / 2 + 5 * devicePixelRatio;
                    ctx.rect(this.highlightedWord.hpos - gap,
                        this.highlightedWord.vpos - gap,
                        this.highlightedWord.width + (gap * 2),
                        this.highlightedWord.height + (gap * 2));
                    ctx.strokeStyle = HighligtAltoWordCanvasPageLayer.EDIT_HIGHLIGHT_COLOR;
                    ctx.lineWidth = width;
                    ctx.stroke();
                }
                ctx.restore();
            }
        }


    }
}

addViewerComponent(mycore.viewer.components.MyCoReAltoEditorComponent);
