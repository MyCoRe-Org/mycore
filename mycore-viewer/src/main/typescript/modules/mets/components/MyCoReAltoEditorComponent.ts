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
    import AltoChange = mycore.viewer.widgets.alto.AltoChange;
    import PageLoadedEvent = mycore.viewer.components.events.PageLoadedEvent;

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
        public editorWidget: mycore.viewer.widgets.alto.AltoEditorWidget;
        private static DROP_DOWN_CHILD_ID = "altoButtonChild";
        private currentOrder: number;
        private currentAltoID: string;
        private highlightWordLayer: HighligtAltoWordCanvasPageLayer = new HighligtAltoWordCanvasPageLayer(this);
        private altoIDImageMap = new MyCoReMap<string, StructureImage>();
        private imageHrefAltoContentMap = new MyCoReMap<string, HTMLElement>();
        public altoHrefImageHrefMap = new MyCoReMap<string, string>();
        public imageHrefAltoHrefMap = new MyCoReMap<string, string>();

        private initialHtmlApplyList: Array<AltoChange> = new Array<AltoChange>();


        constructor(private _settings: MetsSettings, private _container: JQuery) {
            super();
        }

        private everythingLoadedSynchronize = Utils.synchronize<MyCoReAltoEditorComponent>(
            [
                (obj) => obj._toolbarModel != null,
                (obj) => obj._languageModel != null,
                (obj) => obj._structureModel != null,
                (obj) => obj._altoPresent
            ],
            (obj) => {
                obj.completeLoaded();
            }
        );

        public init() {
            if (this.editorEnabled()) {
                this.container = jQuery("<div></div>");
                this.containerTitle = jQuery("<span>ALTO-Editor</span>");

                this.trigger(new WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));

                this.container.bind("iviewResize", () => {
                    this.updateContainerSize();
                });
            }
        }

        private editorEnabled() {
            return typeof this._settings.altoEditorPostURL !== "undefined" && this._settings.altoEditorPostURL != null;
        }


        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                let structureModelLodedEvent = <events.StructureModelLoadedEvent>e;
                this._structureModel = structureModelLodedEvent.structureModel;
                this._structureImages = this._structureModel.imageList;
                for (let imageIndex in this._structureImages) {
                    let image = this._structureImages[ imageIndex ];
                    this._altoPresent = this._altoPresent || image.additionalHrefs.has("AltoHref");
                    if (this._altoPresent) {
                        this.altoHrefImageHrefMap.set(image.additionalHrefs.get("AltoHref"), image.href);
                    }
                }
                this.everythingLoadedSynchronize(this);
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
                    this.openEditor();
                }
            }


            if (e.type == events.PageLoadedEvent.TYPE) {
                let ple = < events.PageLoadedEvent>e;

                let altoContent = (<TileImagePage>ple.abstractPage).getHTMLContent();
                if (altoContent.value != null) {
                    let structureImage = this._structureModel.imageHrefImageMap.get(ple.abstractPage.id);
                    let altoHref = structureImage.additionalHrefs.get("AltoHref");
                    this.altoIDImageMap.set(altoContent.value.getAttribute("data-id"), structureImage);
                    this.syncChanges(altoContent.value, altoHref);
                    this.imageHrefAltoContentMap.set(structureImage.href, altoContent.value);
                    this.imageHrefAltoHrefMap.set(structureImage.href, altoHref);

                } else {
                    altoContent.addObserver({
                        propertyChanged : (old: ViewerProperty<HTMLElement>, _new: ViewerProperty<HTMLElement>) => {
                            let structureImage = this._structureModel.imageHrefImageMap.get(ple.abstractPage.id);
                            let altoHref = structureImage.additionalHrefs.get("AltoHref");
                            this.altoIDImageMap.set(_new.value.getAttribute("data-id"), structureImage);
                            this.syncChanges(_new.value, altoHref);
                            this.imageHrefAltoContentMap.set(structureImage.href, _new.value);
                            this.imageHrefAltoHrefMap.set(structureImage.href, altoHref);
                        }
                    });
                }
            }

            if (e.type == events.ShowContentEvent.TYPE) {
                let sce = <events.ShowContentEvent>e;
                if (sce.containerDirection == events.ShowContentEvent.DIRECTION_WEST) {
                    if (sce.size == 0) {
                        this.toggleEditWord(false);
                    }
                }
            }
        }

        private openEditor() {
            this.trigger(new ShowContentEvent(this, this.container, ShowContentEvent.DIRECTION_WEST, 400, this.containerTitle));
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


        private currentEditWord: HTMLElement = null;
        private beforeEditWord: string = null;

        private static ENTER_KEY = 13;
        private static ESC_KEY = 27;


        private editWord(element: HTMLElement,
                         vpos: number,
                         hpos: number,
                         width: number,
                         height: number) {

            if (this.currentEditWord != null) {
                this.applyEdit(this.currentEditWord, this.currentAltoID, this.currentOrder);
            }

            let findAltoID = (el: HTMLElement) => {
                return el.getAttribute("data-id") == null ? findAltoID(el.parentElement) : el.getAttribute("data-id");
            };

            let altoID = findAltoID(element);


            this.currentAltoID = altoID;
            this.currentOrder = this.altoIDImageMap.get(altoID).order;
            this.currentEditWord = element;
            this.beforeEditWord = element.innerText;
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
                id : this.currentAltoID
            });
            this.trigger(new events.RedrawEvent(this));
        }

        public keyDown(e: JQueryKeyEventObject) {
            if (this.currentEditWord != null) {
                if (e.keyCode == MyCoReAltoEditorComponent.ENTER_KEY) {
                    this.applyEdit(this.currentEditWord, this.currentAltoID, this.currentOrder);
                    return;
                }

                if (e.keyCode == MyCoReAltoEditorComponent.ESC_KEY) {
                    this.abortEdit(this.currentEditWord);
                    return;
                }

            }
        }

        private abortEdit(element: HTMLElement) {
            element.innerText = this.beforeEditWord;

            this.endEdit(element);
        }

        private resetWordEdit(element: HTMLElement) {
            if (this.currentEditWord == element) {
                this.abortEdit(element);
            }

            let word = element.getAttribute("data-word");
            element.innerText = word;

            if (element.classList.contains("edited")) {
                element.classList.remove("edited");
            }


        }

        private applyEdit(element: HTMLElement, altoID: string, order: number) {
            let vpos = parseInt(element.getAttribute("data-vpos")),
                hpos = parseInt(element.getAttribute("data-hpos")),
                width = parseInt(element.getAttribute("data-width")),
                height = parseInt(element.getAttribute("data-height")),
                newWord = element.innerHTML;

            let key = this.calculateChangeKey(altoID, vpos, hpos);

            let oldWord = element.getAttribute("data-word");

            if (!this.editorWidget.hasChange(key)) {
                if (oldWord !== newWord) {

                    let wordChange = new AltoWordChange(
                        altoID,
                        hpos,
                        vpos,
                        width,
                        height,
                        oldWord,
                        newWord,
                        order
                    );
                    this.editorWidget.addChange(key, wordChange);
                    element.classList.add("edited")
                }
            } else {
                let wordChange = <AltoWordChange>this.editorWidget.getChange(key);

                if (oldWord !== newWord) {
                    wordChange.to = newWord;
                    this.editorWidget.updateChange(wordChange);
                } else {
                    this.editorWidget.removeChange(wordChange);
                    if (element.classList.contains("edited")) {
                        element.classList.remove("edited");
                    }
                }
            }

            this.endEdit(element);
        }

        private calculateChangeKey(altoID: string, vpos: number, hpos: number) {
            return `${altoID}_${vpos}_${hpos}`;
        }

        private endEdit(element: HTMLElement) {
            this.currentEditWord = this.currentOrder = this.currentAltoID = this.beforeEditWord = null;
            element.setAttribute("contenteditable", "false");
            this.highlightWordLayer.setHighlightedWord(null);
            viewerClearTextSelection();
            this.trigger(new events.RedrawEvent(this));
        }

        public get handlesEvents(): string[] {
            return this.editorEnabled() ? [
                events.StructureModelLoadedEvent.TYPE,
                events.LanguageModelLoadedEvent.TYPE,
                events.ProvideToolbarModelEvent.TYPE,
                widgets.toolbar.events.DropdownButtonPressedEvent.TYPE,
                events.ImageChangedEvent.TYPE,
                events.PageLoadedEvent.TYPE,
                events.ShowContentEvent.TYPE
            ] : [];
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

        public isEditing() {
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
            this.containerTitle.text(`${this._languageModel.getTranslation("altoEditor")}`);

            if (typeof  this._settings.altoReviewer !== "undefined" && this._settings.altoReviewer != null && this._settings.altoReviewer) {
                this.editorWidget.enableApplyButton(true);
            }

            if (typeof this._settings.altoChanges != "undefined" && this._settings.altoChanges != null) {
                if ("wordChanges" in this._settings.altoChanges && this._settings.altoChanges.wordChanges instanceof Array) {
                    this._settings.altoChanges.wordChanges.forEach((change) => {
                        change.pageOrder = this._structureModel.imageHrefImageMap.get(this.altoHrefImageHrefMap.get(change.file)).order;
                        this.editorWidget.addChange(this.calculateChangeKey(change.file, change.hpos, change.vpos), change);
                        this.initialHtmlApplyList.push(change);
                    });
                }
            }

            this.editorWidget.changeWordButton.click((ev) => {
                this.toggleEditWord();
            });

            this.editorWidget.addChangeClickedEventHandler((change) => {
                this.trigger(new events.ImageSelectedEvent(this, this._structureModel.imageHrefImageMap.get(this.altoHrefImageHrefMap.get(change.file))));

            });

            let submitSuccess = (result: { pid: string }) => {
                this._settings.altoChangePID = result.pid;
            };

            let applySuccess = () => {
                window.location.reload(true);
            };

            this.editorWidget.addSubmitClickHandler(() => {
                this.submitChanges(submitSuccess);
            });

            this.editorWidget.addApplyClickHandler(() => {
                this.submitChanges((result: { pid: string }) => {
                    submitSuccess(result);
                    this.applyChanges(applySuccess);
                });
            });

            this.editorWidget.addDeleteClickHandler(() => {
                if (this._settings.altoChangePID) {
                    let requestURL = this._settings.altoEditorPostURL;
                    requestURL += "/delete/" + this._settings.altoChangePID;

                    jQuery.ajax(requestURL, {
                        contentType : "application/json",
                        type : "POST",
                        success : () => {
                            this._settings.altoChangePID = null;
                        }
                    });
                }
                this.editorWidget.getChanges().forEach((file, change) => {
                    this.removeChange(change);
                })
            });

            this.editorWidget.addChangeRemoveClickHandler((change) => {
                this.removeChange(change);
            });


            this.trigger(new events.AddCanvasPageLayerEvent(this, 2, this.highlightWordLayer));
            this.trigger(new events.RequestDesktopInputEvent(this, new EditAltoInputListener(this)));
            this.trigger(new events.WaitForEvent(this, PageLoadedEvent.TYPE));

            if (this._settings.leftShowOnStart === 'altoEditor') {
                this.openEditor();
            }

            this.updateContainerSize();
        }

        private removeChange(change) {
            this.editorWidget.removeChange(change);

            // revert changes made in html
            let imageHref = this.altoHrefImageHrefMap.get(change.file);
            if (this.imageHrefAltoContentMap.has(imageHref)) {
                let altoContent = this.imageHrefAltoContentMap.get(imageHref);
                if (change.type === AltoWordChange.TYPE) {
                    let wordChange = <AltoWordChange>change;
                    let searchResult = this.findChange(wordChange, altoContent);

                    if (searchResult.length == 0) {
                        console.log("Could not find change " + wordChange);
                    } else {
                        this.resetWordEdit(searchResult.get(0));
                    }

                }
            }
            this.trigger(new events.RedrawEvent(this));
            // if they are not in the map then they are not changed :D
        }

        private applyChanges(callback) {
            let requestURL = this._settings.altoEditorPostURL;
            requestURL += "/apply/" + this._settings.altoChangePID;

            jQuery.ajax(requestURL, {
                contentType : "application/json",
                type : "POST",
                success : callback
            });
        }

        private submitChanges(success: (result: { pid: string }) => any) {
            let changeSet = {
                "wordChanges" : this.editorWidget.getChanges().values,
                "derivateID" : this._settings.derivate
            };
            let requestURL = this._settings.altoEditorPostURL;
            if (typeof this._settings.altoChangePID !== "undefined" && this._settings.altoChangePID != null) {
                requestURL += "/update/" + this._settings.altoChangePID;
            } else {
                requestURL += "/store"
            }

            jQuery.ajax(requestURL, {
                data : this.prepareData(changeSet),
                contentType : "application/json",
                type : "POST",
                success : success
            });
        }

        private prepareData(changeSet: { wordChanges: Array<mycore.viewer.widgets.alto.AltoChange> }) {
            let copy = JSON.parse(JSON.stringify(changeSet));

            copy.wordChanges.forEach((val: AltoChange) => {
                if ("pageOrder" in val) {
                    delete val.pageOrder;
                }
            });

            return JSON.stringify(copy);
        }

        private findChange(wordChange: mycore.viewer.widgets.alto.AltoWordChange, altoContent: HTMLElement) {
            let find = `[data-hpos=${wordChange.hpos}][data-vpos=${wordChange.vpos}]` +
                `[data-width=${wordChange.width}][data-height=${wordChange.height}]`;
            let searchResult = jQuery(altoContent).find(find);
            return searchResult;
        }

        private updateContainerSize() {
            this.container.css({
                "height" : (this.container.parent().height() - this.containerTitle.parent().outerHeight()) + "px",
                "overflow-y" : "scroll"
            });
        }


        public drag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D, e: JQueryMouseEventObject) {
            if (e.target !== this.currentEditWord) {
                e.preventDefault();
            }
        }

        public mouseDown(position: Position2D, e: JQueryMouseEventObject) {
            if (e.target !== this.currentEditWord) {
                e.preventDefault();
            }
        }

        private syncChanges(altoContent: HTMLElement, href:string) {
            let changesInFile = this.editorWidget.getChangesInFile(href);
            changesInFile.forEach(change => {
                if (change.type == AltoWordChange.TYPE) {
                    let wordChange = <AltoWordChange> change;
                    let elementToChange = this.findChange(wordChange, altoContent);
                    if (elementToChange.length > 0) {
                        elementToChange[ 0 ].innerText = wordChange.to;
                        elementToChange.addClass("edited");
                    } else {
                        console.log("Could not find Change: " + change);
                    }
                }

            });
        }
    }

    export class EditAltoInputListener extends DesktopInputAdapter {

        constructor(private editAltoComponent: MyCoReAltoEditorComponent) {
            super();
        }

        public mouseDown(position: Position2D, e: JQueryMouseEventObject): void {
            if (this.editAltoComponent.isEditing()) {
                this.editAltoComponent.mouseDown(position, e);
            }
        }

        public mouseUp(position: Position2D, e: JQueryMouseEventObject) {
        }

        public mouseMove(position: Position2D, e: JQueryMouseEventObject) {

        }

        public mouseClick(position: Position2D, e: JQueryMouseEventObject) {
            if (this.editAltoComponent.isEditing()) {
                this.editAltoComponent.mouseClick(position, e);
            }
        }

        public mouseDoubleClick(position: Position2D, e: JQueryMouseEventObject): void {
        }

        public keydown(e: JQueryKeyEventObject): void {
            this.editAltoComponent.keyDown(e);
        }

        public mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
                         e: JQueryMouseEventObject): void {

            if (this.editAltoComponent.isEditing()) {
                this.editAltoComponent.drag(currentPosition, startPosition, startViewport,
                    e);
            }
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

        constructor(private component: MyCoReAltoEditorComponent) {

        }

        private static EDIT_HIGHLIGHT_COLOR = "#90EE90";
        private static EDITED_HIGHLIGHT_COLOR = "#ADD8E6";


        private highlightedWord: HighlightedWord = null;

        public getHighlightedWord() {
            return this.highlightedWord;
        }

        public setHighlightedWord(word: HighlightedWord) {
            this.highlightedWord = word;
        }

        public draw(ctx: CanvasRenderingContext2D, id: string, pageSize: Size2D, drawOnHtml: boolean): void {
            if (drawOnHtml) {
                return;
            }

            ctx.save();
            {
                if (this.highlightedWord != null && id == this.component.altoHrefImageHrefMap.get(this.highlightedWord.id)) {
                    this.strokeWord(ctx,
                        this.highlightedWord.hpos,
                        this.highlightedWord.vpos,
                        this.highlightedWord.width,
                        this.highlightedWord.height,
                        HighligtAltoWordCanvasPageLayer.EDIT_HIGHLIGHT_COLOR);
                }

                let file = this.component.imageHrefAltoHrefMap.get(id);
                if (typeof file !== "undefined") {

                    this.component.editorWidget.getChangesInFile(file)
                        .forEach((change) => {
                            if (change.type == AltoWordChange.TYPE) {
                                let wordChange = <AltoWordChange>change;
                                this.strokeWord(ctx,
                                    wordChange.hpos,
                                    wordChange.vpos,
                                    wordChange.width,
                                    wordChange.height,
                                    HighligtAltoWordCanvasPageLayer.EDITED_HIGHLIGHT_COLOR);
                            }
                        });
                }
            }

            ctx.restore();

        }

        private strokeWord(ctx: CanvasRenderingContext2D, hpos, vpos, wwidth, wheight, color) {
            let width = 5 * window.devicePixelRatio;

            let gap = width / 2 + 5 * devicePixelRatio;
            ctx.rect(hpos - gap,
                vpos - gap,
                wwidth + (gap * 2),
                wheight + (gap * 2));
            ctx.strokeStyle = color;
            ctx.lineWidth = width;
            ctx.stroke();
        }


    }
}

addViewerComponent(mycore.viewer.components.MyCoReAltoEditorComponent);
