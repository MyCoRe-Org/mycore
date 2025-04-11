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
import {StructureImage} from "../../base/components/model/StructureImage";
import {StructureModel} from "../../base/components/model/StructureModel";
import {LanguageModel} from "../../base/components/model/LanguageModel";
import {MyCoReBasicToolbarModel} from "../../base/components/model/MyCoReBasicToolbarModel";
import {ToolbarDropdownButton} from "../../base/widgets/toolbar/model/ToolbarDropdownButton";
import {
  getElementHeight,
  getElementOuterHeight,
  MyCoReMap,
  Position2D,
  Size2D,
  Utils,
  viewerClearTextSelection,
  ViewerProperty
} from "../../base/Utils";
import {AltoChange, AltoWordChange} from "../widgets/alto/AltoChange";
import {MetsSettings} from "./MetsSettings";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {ProvideToolbarModelEvent} from "../../base/components/events/ProvideToolbarModelEvent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";
import {StructureModelLoadedEvent} from "../../base/components/events/StructureModelLoadedEvent";
import {LanguageModelLoadedEvent} from "../../base/components/events/LanguageModelLoadedEvent";
import {DropdownButtonPressedEvent} from "../../base/widgets/toolbar/events/DropdownButtonPressedEvent";
import {PageLoadedEvent} from "../../base/components/events/PageLoadedEvent";
import {TileImagePage} from "../../base/widgets/canvas/TileImagePage";
import {ShowContentEvent} from "../../base/components/events/ShowContentEvent";
import {RequestStateEvent} from "../../base/components/events/RequestStateEvent";
import {RedrawEvent} from "../../base/components/events/RedrawEvent";
import {ImageChangedEvent} from "../../base/components/events/ImageChangedEvent";
import {TextEditEvent} from "../../base/components/events/TextEditEvent";
import {AltoEditorWidget} from "../widgets/alto/AltoEditorWidget";
import {ImageSelectedEvent} from "../../base/components/events/ImageSelectedEvent";
import {ViewerInfoModal} from "../../base/widgets/modal/ViewerInfoModal";
import {UpdateURLEvent} from "../../base/components/events/UpdateURLEvent";
import {ViewerErrorModal} from "../../base/widgets/modal/ViewerErrorModal";
import {ViewerConfirmModal} from "../../base/widgets/modal/ViewerConfirmModal";
import {AddCanvasPageLayerEvent} from "../../base/components/events/AddCanvasPageLayerEvent";
import {RequestDesktopInputEvent} from "../../base/components/events/RequestDesktopInputEvent";
import {DesktopInputAdapter} from "../../base/widgets/canvas/input/DesktopInputListener";
import {CanvasPageLayer} from "../../base/widgets/canvas/CanvasPageLayer";

export class MyCoReAltoEditorComponent extends ViewerComponent {
  private _structureImages: Array<StructureImage>;
  private _structureModel: StructureModel;
  private _altoPresent: boolean;
  private _languageModel: LanguageModel;
  private _toolbarModel: MyCoReBasicToolbarModel;
  private _sidebarControllDropdownButton: ToolbarDropdownButton;
  private _altoDropdownChildItem: { id: string; label: string };
  private container: HTMLElement;
  private containerTitle: HTMLElement;
  public editorWidget: AltoEditorWidget;
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
      this.container = document.createElement("div");
      this.containerTitle = document.createElement("span");
      this.containerTitle.innerText = "ALTO-Editor";

      this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));

      this.container.addEventListener("iviewResize", () => {
        this.updateContainerSize();
      });
    }
  }

  private editorEnabled() {
    return typeof this._settings.altoEditorPostURL !== "undefined" && this._settings.altoEditorPostURL != null;
  }

  public handle(e: ViewerEvent): void {
    if (e.type == StructureModelLoadedEvent.TYPE) {
      const structureModelLodedEvent = e as StructureModelLoadedEvent;
      this._structureModel = structureModelLodedEvent.structureModel;
      this._structureImages = this._structureModel.imageList;
      for (let imageIndex in this._structureImages) {
        let image = this._structureImages[imageIndex];
        let altoHref = image.additionalHrefs.get("AltoHref");
        this._altoPresent = this._altoPresent || altoHref != null;
        if (this._altoPresent) {
          if (altoHref == null) {
            console.warn("there is no alto.xml for " + image.href);
            continue;
          }
          this.altoHrefImageHrefMap.set(altoHref, image.href);
        }
      }
      this.everythingLoadedSynchronize(this);
    }

    if (e.type == LanguageModelLoadedEvent.TYPE) {
      const lmle = e as LanguageModelLoadedEvent;
      this._languageModel = lmle.languageModel;
      this.everythingLoadedSynchronize(this);
    }

    if (e.type == ProvideToolbarModelEvent.TYPE) {
      const ptme = e as ProvideToolbarModelEvent;
      this._toolbarModel = ptme.model;
      this._sidebarControllDropdownButton = ptme.model._sidebarControllDropdownButton;
      this.everythingLoadedSynchronize(this);
    }

    if (e.type == DropdownButtonPressedEvent.TYPE) {
      const dbpe = e as DropdownButtonPressedEvent;

      if (dbpe.childId === MyCoReAltoEditorComponent.DROP_DOWN_CHILD_ID) {
        this.openEditor();
      }
    }

    if (e.type == PageLoadedEvent.TYPE) {
      const ple = e as PageLoadedEvent;

      const altoContent = (ple.abstractPage as TileImagePage).getHTMLContent();
      if (altoContent.value != null) {
        this.updateHTML(ple.abstractPage.id, altoContent.value);
      } else {
        altoContent.addObserver({
          propertyChanged: (old: ViewerProperty<HTMLElement>, _new: ViewerProperty<HTMLElement>) => {
            this.updateHTML(ple.abstractPage.id, _new.value);
          }
        });
      }
    }

    if (e.type == ShowContentEvent.TYPE) {
      const sce = e as ShowContentEvent;
      if (sce.containerDirection == ShowContentEvent.DIRECTION_WEST) {
        if (sce.size == 0) {
          this.toggleEditWord(false);
        }
      }
    }

    if (e.type == RequestStateEvent.TYPE) {
      const requestStateEvent = e as RequestStateEvent;
      if ("altoChangePID" in this._settings && this._settings.altoChangePID != null) {
        requestStateEvent.stateMap.set("altoChangeID", this._settings.altoChangePID);
      }
    }

  }

  private openEditor() {
    this.trigger(new ShowContentEvent(this, this.container, ShowContentEvent.DIRECTION_WEST, 400, this.containerTitle));
  }

  private updateHTML(pageId: string, element: HTMLElement) {
    let structureImage = this._structureModel.imageHrefImageMap.get(pageId);
    let altoHref = structureImage.additionalHrefs.get("AltoHref");
    this.altoIDImageMap.set(element.getAttribute("data-id"), structureImage);
    this.imageHrefAltoContentMap.set(structureImage.href, element);
    this.imageHrefAltoHrefMap.set(structureImage.href, altoHref);
    this.syncChanges(element, altoHref);
    if (this.isEditing()) {
      this.applyConfidenceLevel(element);
    } else {
      this.removeConfidenceLevel(element);
    }
  }

  public mouseClick(position: Position2D, ev: MouseEvent) {
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
      vpos: vpos,
      hpos: hpos,
      width: width,
      height: height,
      id: this.currentAltoID
    });
    this.trigger(new RedrawEvent(this));
  }

  public keyDown(e: KeyboardEvent) {
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
    element.innerText = element.getAttribute("data-word");

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
    this.trigger(new RedrawEvent(this));
  }

  public get handlesEvents(): string[] {
    return this.editorEnabled() ? [
      StructureModelLoadedEvent.TYPE,
      LanguageModelLoadedEvent.TYPE,
      ProvideToolbarModelEvent.TYPE,
      DropdownButtonPressedEvent.TYPE,
      ImageChangedEvent.TYPE,
      PageLoadedEvent.TYPE,
      ShowContentEvent.TYPE,
      RequestStateEvent.TYPE
    ] : [];
  }

  private toggleEditWord(enable: boolean = null) {
    if (this.editorWidget == null) {
      return;
    }
    enable = enable == null ? !this.editorWidget.changeWordButton.classList.contains("active") : enable;
    let button = this.editorWidget.changeWordButton;
    if (enable) {
      button.classList.add("active");
      this.imageHrefAltoContentMap.values.forEach(html => {
        this.applyConfidenceLevel(html);
      });
    } else {
      button.classList.remove("active");
      if (this.currentEditWord != null) {
        this.endEdit(this.currentEditWord);
      }
      this.imageHrefAltoContentMap.values.forEach(html => {
        this.removeConfidenceLevel(html);
      });
    }
    this.trigger(new TextEditEvent(this, enable));
    this.trigger(new RedrawEvent(this));
  }

  public isEditing() {
    return this.editorWidget != null && this.editorWidget.changeWordButton.classList.contains("active");
  }

  private completeLoaded() {
    this._altoDropdownChildItem = {
      id: MyCoReAltoEditorComponent.DROP_DOWN_CHILD_ID,
      label: this._languageModel.getTranslation("altoEditor")
    };
    this._sidebarControllDropdownButton.children.push(this._altoDropdownChildItem);
    this._sidebarControllDropdownButton.children = this._sidebarControllDropdownButton.children;
    this.editorWidget = new AltoEditorWidget(this.container, this._languageModel);
    this.containerTitle.innerText = `${this._languageModel.getTranslation("altoEditor")}`;

    if (typeof this._settings.altoReviewer !== "undefined" && this._settings.altoReviewer != null && this._settings.altoReviewer) {
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

    this.editorWidget.changeWordButton.addEventListener('click', (ev) => {
      this.toggleEditWord();
    });

    this.editorWidget.addChangeClickedEventHandler((change) => {
      this.trigger(new ImageSelectedEvent(this, this._structureModel.imageHrefImageMap.get(this.altoHrefImageHrefMap.get(change.file))));

    });

    let submitSuccess = (result: { pid: string }) => {
      this._settings.altoChangePID = result.pid;
      let title = "altoChanges.save.successful.title";
      let msg = "altoChanges.save.successful.message";
      new ViewerInfoModal(this._settings.mobile, title, msg)
        .updateI18n(this._languageModel)
        .show();
    };

    let applySuccess = () => {
      this._settings.altoChangePID = null;
      this.trigger(new UpdateURLEvent(this));
      window.location.reload();
    };

    let errorSaveCallback = (jqXHR: any) => {
      console.log(jqXHR);
      let img = this._settings.webApplicationBaseURL + "/modules/iview2/img/sad-emotion-egg.jpg";
      let title = "altoChanges.save.failed.title";
      let msg = "altoChanges.save.failed.message";
      new ViewerErrorModal(this._settings.mobile, title, msg, img)
        .updateI18n(this._languageModel)
        .show();
    };

    let errorDeleteCallback = (jqXHR: any) => {
      console.log(jqXHR);
      let img = this._settings.webApplicationBaseURL + "/modules/iview2/img/sad-emotion-egg.jpg";
      let title = "altoChanges.delete.failed.title";
      let msg = "altoChanges.delete.failed.message";
      new ViewerErrorModal(this._settings.mobile, title, msg, img)
        .updateI18n(this._languageModel)
        .show();
    };

    this.editorWidget.addSubmitClickHandler(() => {
      this.submitChanges(submitSuccess, errorSaveCallback);
    });

    this.editorWidget.addApplyClickHandler(() => {
      let title = "altoChanges.applyChanges.title";
      let msg = "altoChanges.applyChanges.message";
      new ViewerConfirmModal(this._settings.mobile, title, msg, (confirm) => {
        if (!confirm) {
          return;
        }
        this.submitChanges((result: { pid: string }) => {
          this._settings.altoChangePID = result.pid;
          this.applyChanges(applySuccess, errorSaveCallback);
        }, errorSaveCallback);
      }).updateI18n(this._languageModel).show();
    });

    this.editorWidget.addDeleteClickHandler(() => {
      let title = "altoChanges.removeChanges.title";
      let msg = "altoChanges.removeChanges.message";
      new ViewerConfirmModal(this._settings.mobile, title, msg, (confirm) => {
        if (this._settings.altoChangePID) {
          let requestURL = this._settings.altoEditorPostURL;
          requestURL += "/delete/" + this._settings.altoChangePID;

          fetch(requestURL, {
            method: "POST", headers: {
              "Content-Type": "application/json"
            }
          }).then(response => {
            if (!response.ok) {
              throw new Error('Network response was not ok ' + response.statusText);
            }
            return response.json();
          }).then(data => {
            this._settings.altoChangePID = null;
            this.trigger(new UpdateURLEvent(this));
          }).catch(error => {
            errorDeleteCallback(error);
          });
        }
        this.editorWidget.getChanges().forEach((file, change) => {
          this.removeChange(change);
        });
      }).updateI18n(this._languageModel).show();
    });

    this.editorWidget.addChangeRemoveClickHandler((change) => {
      let title = "altoChanges.removeChange.title";
      let msg = "altoChanges.removeChange.message";
      new ViewerConfirmModal(this._settings.mobile, title, msg, (confirm) => {
        if (!confirm) {
          return;
        }
        this.removeChange(change);
      }).updateI18n(this._languageModel).show();
    });

    this.trigger(new AddCanvasPageLayerEvent(this, 2, this.highlightWordLayer));
    this.trigger(new RequestDesktopInputEvent(this, new EditAltoInputListener(this)));
    this.trigger(new WaitForEvent(this, PageLoadedEvent.TYPE));

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

        if (!searchResult) {
          console.log("Could not find change " + wordChange);
        } else {
          this.resetWordEdit(searchResult as HTMLElement);
        }

      }
    }
    this.trigger(new RedrawEvent(this));
    // if they are not in the map then they are not changed :D
  }

  private applyChanges(successCallback, errorCallback) {
    let requestURL = this._settings.altoEditorPostURL;
    requestURL += "/apply/" + this._settings.altoChangePID;

    fetch(requestURL, {
      method: "POST", headers: {
        "Content-Type": "application/json"
      }
    }).then(response => {
      if (!response.ok) {
        throw new Error('Network response was not ok ' + response.statusText);
      }
      return response.json();
    }).then(data => {
      successCallback(data);
    }).catch(error => {
      errorCallback(error);
    });
  }

  private submitChanges(successCallback: (result: { pid: string }) => any, errorCallback) {
    let changeSet = {
      "wordChanges": this.editorWidget.getChanges().values,
      "derivateID": this._settings.derivate
    };
    let requestURL = this._settings.altoEditorPostURL;
    if (typeof this._settings.altoChangePID !== "undefined" && this._settings.altoChangePID != null) {
      requestURL += "/update/" + this._settings.altoChangePID;
    } else {
      requestURL += "/store"
    }

    fetch(requestURL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(this.prepareData(changeSet))
    }).then(response => {
      if (!response.ok) {
        throw new Error('Network response was not ok ' + response.statusText);
      }
      return response.json();
    }).then(data => {
      successCallback(data);
    }).catch(error => {
      errorCallback(error);
    })
  }

  private prepareData(changeSet: { wordChanges: Array<AltoChange> }) {
    let copy = JSON.parse(JSON.stringify(changeSet));

    copy.wordChanges.forEach((val: AltoChange) => {
      if ("pageOrder" in val) {
        delete val.pageOrder;
      }
    });

    return JSON.stringify(copy);
  }

  private findChange(wordChange: AltoWordChange, altoContent: HTMLElement) {
    let find = `[data-hpos=${wordChange.hpos}][data-vpos=${wordChange.vpos}]` +
      `[data-width=${wordChange.width}][data-height=${wordChange.height}]`;
    let searchResult = altoContent.querySelector(find);
    return searchResult;
  }

  private updateContainerSize() {
    this.container.style.height = getElementHeight(this.container.parentElement) -
        getElementOuterHeight(this.containerTitle.parentElement) + "px";
    this.container.style.overflowY = "scroll";
  }


  public drag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D, e: MouseEvent) {
    if (e.target !== this.currentEditWord) {
      e.preventDefault();
    }
  }

  public mouseDown(position: Position2D, e: MouseEvent) {
    if (e.target !== this.currentEditWord) {
      e.preventDefault();
    }
  }

  private syncChanges(altoContent: HTMLElement, href: string) {
    let changesInFile = this.editorWidget.getChangesInFile(href);
    changesInFile.forEach(change => {
      if (change.type == AltoWordChange.TYPE) {
        let wordChange = <AltoWordChange>change;
        let elementToChange = this.findChange(wordChange, altoContent) as HTMLElement;
        if (elementToChange) {
          elementToChange.innerText = wordChange.to;
          elementToChange.classList.add("edited");
        } else {
          console.log("Could not find Change: " + change);
        }
      }

    });
  }

  private applyConfidenceLevel(altoContent: HTMLElement) {
    altoContent.querySelectorAll("[data-wc]:not([data-wc='1'])").forEach((element, i) => {
      let wc: number = parseFloat(element.getAttribute("data-wc"));
      if (wc < 0.9) {
        element.classList.add('unconfident');
      }
    });
  }

  private removeConfidenceLevel(altoContent: HTMLElement) {
    altoContent.querySelectorAll(".unconfident").forEach(el => el.classList.remove("unconfident"));
  }

}

export class EditAltoInputListener extends DesktopInputAdapter {

  constructor(private editAltoComponent: MyCoReAltoEditorComponent) {
    super();
  }

  public mouseDown(position: Position2D, e: MouseEvent): void {
    if (this.editAltoComponent.isEditing()) {
      this.editAltoComponent.mouseDown(position, e);
    }
  }

  public mouseUp(position: Position2D, e: MouseEvent) {
  }

  public mouseMove(position: Position2D, e: MouseEvent) {

  }

  public mouseClick(position: Position2D, e: MouseEvent) {
    if (this.editAltoComponent.isEditing()) {
      this.editAltoComponent.mouseClick(position, e);
    }
  }

  public mouseDoubleClick(position: Position2D, e: MouseEvent): void {
  }

  public keydown(e: KeyboardEvent): void {
    this.editAltoComponent.keyDown(e);
  }

  public mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
    e: MouseEvent): void {

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

export class HighligtAltoWordCanvasPageLayer implements CanvasPageLayer {

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
