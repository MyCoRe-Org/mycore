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
import {MyCoReViewerSettings} from "../../base/MyCoReViewerSettings";
import {StructureModel} from "../../base/components/model/StructureModel";
import {ToolbarTextInput} from "../../base/widgets/toolbar/model/ToolbarTextInput";
import {MyCoReViewerSearcher} from "./model/MyCoReViewerSearcher";
import {getElementHeight, getElementOuterHeight, MyCoReMap, Rect, Utils, ViewerProperty} from "../../base/Utils";
import {StructureImage} from "../../base/components/model/StructureImage";
import {SearchResultCanvasPageLayer} from "../widgets/canvas/SearchResultCanvasPageLayer";
import {AddCanvasPageLayerEvent} from "../../base/components/events/AddCanvasPageLayerEvent";
import {MyCoReBasicToolbarModel} from "../../base/components/model/MyCoReBasicToolbarModel";
import {LanguageModel} from "../../base/components/model/LanguageModel";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {ProvideViewerSearcherEvent} from "./events/ProvideViewerSearcherEvent";
import {StructureModelLoadedEvent} from "../../base/components/events/StructureModelLoadedEvent";
import {ProvideToolbarModelEvent} from "../../base/components/events/ProvideToolbarModelEvent";
import {LanguageModelLoadedEvent} from "../../base/components/events/LanguageModelLoadedEvent";
import {RestoreStateEvent} from "../../base/components/events/RestoreStateEvent";
import {ShowContentEvent} from "../../base/components/events/ShowContentEvent";
import {RedrawEvent} from "../../base/components/events/RedrawEvent";
import {ImageSelectedEvent} from "../../base/components/events/ImageSelectedEvent";
import {DropdownButtonPressedEvent} from "../../base/widgets/toolbar/events/DropdownButtonPressedEvent";
import {RequestStateEvent} from "../../base/components/events/RequestStateEvent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";
import {RequestTextContentEvent} from "../../base/components/events/RequestTextContentEvent";


export class MyCoReSearchComponent extends ViewerComponent {

  constructor(private _settings: MyCoReViewerSettings) {
    super();
  }

  private _container: HTMLElement;
  private _searchContainer: HTMLElement;
  private _sidebarLabel = this.createSidebarLabel();
  private _model: StructureModel = null;
  private _valueToApply: string = null;
  private _containerVisible: boolean = false;
  private _indexPrepared: boolean = false;
  private _indexPreparing: boolean = false;
  private _textPresent: boolean = false;
  private _toolbarTextInput: ToolbarTextInput = new ToolbarTextInput("search", "", "");

  private _searcher: MyCoReViewerSearcher = null;
  private _imageHrefImageMap: MyCoReMap<string, StructureImage> = new MyCoReMap<string, StructureImage>();

  private _searchResultCanvasPageLayer: SearchResultCanvasPageLayer = new SearchResultCanvasPageLayer();

  private _containerVisibleModelLoadedSync = Utils.synchronize<MyCoReSearchComponent>([
    (_self) => _self._model != null,
    (_self) => _self._containerVisible,
    (_self) => _self._searcher != null,
    (_self) => !this._indexPrepared && !this._indexPreparing
  ],
    (_self) => _self._prepareIndex(_self._model));

  private _toolbarLoadedLanguageModelLoadedSync = Utils.synchronize<MyCoReSearchComponent>(
    [
      (_self) => _self._tbModel != null,
      (_self) => _self._languageModel != null,
      (_self) => _self._model != null,
      (_self) => _self._textPresent
    ],
    (_self: MyCoReSearchComponent) => {
      this.trigger(new AddCanvasPageLayerEvent(this, 1, this._searchResultCanvasPageLayer));
      let searchLabel = _self._languageModel.getTranslation("sidebar.search");
      this._toolbarTextInput.placeHolder = _self._languageModel.getTranslation("search.placeHolder");
      _self._sidebarLabel.innerText = searchLabel;
      _self._tbModel._searchGroup.addComponent(this._toolbarTextInput);
      if (this._valueToApply != null) {
        this._toolbarTextInput.value = this._valueToApply;
        this._valueToApply = null;
      }
    });

  private _tbModel: MyCoReBasicToolbarModel = null;
  private _languageModel: LanguageModel = null;

  private _panel: HTMLElement = null;
  private _progressbar: HTMLElement = null;
  private _progressbarInner: HTMLElement = null;
  private _searchTextTimeout = -1;
  private _searchAreaReady = false;

  public createSidebarLabel() {
    const sidebarLabelElement = document.createElement("span");
    sidebarLabelElement.innerText = "Suche";
    return sidebarLabelElement;
  }

  public get container() {
    return this._container;
  }

  private initSearchArea() {
    this._progressbar.parentElement.remove();
    this._progressbar.remove();
    this._container.style.textAlign = "left";


    this._searchContainer = document.createElement("ul");
    this._searchContainer.classList.add("list-group", "textSearch");
    this.container.append(this._searchContainer);

    this._searchAreaReady = true;
    this._search(this._toolbarTextInput.value);
  }

  public init() {
    this._container = document.createElement("div");
    this._container.style.overflowY = "scroll";
    this._container.style.textAlign = "center";
    this._container.addEventListener("iviewResize", () => {
      this.updateContainerSize();
    });

    this._panel = document.createElement("div");
    this._panel.classList.add("card", "search");
    this._container.append(this._panel);

    this._initProgressbar();
    this._panel.append(this._progressbar);


    this._toolbarTextInput.getProperty("value").addObserver({
      propertyChanged: (_old: ViewerProperty<string>, _new: ViewerProperty<string>) => {

        this.openSearch();

        if (this._searchAreaReady) {
          if (this._searchTextTimeout != -1) {
            window.clearTimeout(this._searchTextTimeout);
            this._searchTextTimeout = -1;
          }

          this._searchTextTimeout = window.setTimeout(() => {
            this._search(this._toolbarTextInput.value);
          }, 300);
        }

      }
    });

    this.trigger(new WaitForEvent(this, ProvideViewerSearcherEvent.TYPE));
    this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));
    this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));
    this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));
    this.trigger(new WaitForEvent(this, RestoreStateEvent.TYPE));
  }

  private _search(str: string) {
    if (str == "") {
      let direction = (this._settings.mobile) ? ShowContentEvent.DIRECTION_CENTER : ShowContentEvent.DIRECTION_EAST;
      this.trigger(new ShowContentEvent(this, this._container, direction, 0, this._sidebarLabel));
    }

    for (let i = 0; i < this._searchContainer.children.length; i++) {
      this._searchContainer.children[i].remove();
    }

    let textContents = [];

    this._searcher.search(str, (searchResults) => {
      if (searchResults.length == 0) {
        if (<any>this._settings["embedded"] === "true") {
          this.hideContainer();
        }
      }

      searchResults.forEach((results) => {
        results.arr.forEach(p => textContents.push(p));
      });

      let lastClicked: HTMLElement = null;
      searchResults.forEach((results) => {
        if (results.arr.length <= 0) {
          return;
        }
        let result = document.createElement("li");
        result.classList.add("list-group-item");
        let link = document.createElement("a");
        link.append(results.context.cloneNode(true));
        result.append(link);
        this._searchContainer.append(result);
        let altoTextContent = results.arr[0];
        if (!this._imageHrefImageMap.has(altoTextContent.pageHref)) {
          console.log("Could not find page " + altoTextContent.pageHref);
          return;
        }
        let image = this._imageHrefImageMap.get(altoTextContent.pageHref);
        link.addEventListener('click', () => {
          if (lastClicked != null) {
            lastClicked.classList.remove("active");
            this._searchResultCanvasPageLayer.clearSelected();
          }
          lastClicked = result;
          result.classList.add("active");
          results.arr.forEach(context => {
            let areaRect: Rect = Rect.fromXYWH(context.pos.x, context.pos.y, context.size.width, context.size.height);
            this._searchResultCanvasPageLayer.select(context.pageHref, areaRect);
          });
          this.trigger(new ImageSelectedEvent(this, image));
          this.trigger(new RedrawEvent(this));
        });
        let page = document.createElement("span");
        page.classList.add("childLabel")
        page.innerText = (image.orderLabel || image.order) + "";
        result.append(page);
      });
    }, () => {
      this._searchResultCanvasPageLayer.clear();
      textContents.forEach(tc => {
        let areaRect: Rect = Rect.fromXYWH(tc.pos.x, tc.pos.y, tc.size.width, tc.size.height);
        this._searchResultCanvasPageLayer.add(tc.pageHref, areaRect);
      });
      this.trigger(new RedrawEvent(this));
    });
  }

  private hideContainer() {
    const direction = (this._settings.mobile) ? ShowContentEvent.DIRECTION_CENTER : ShowContentEvent.DIRECTION_EAST;
    this.trigger(new ShowContentEvent(this, this._container, direction, 0, this._sidebarLabel));
  }

  private updateContainerSize() {
    this._container.style.height = (getElementHeight(this._container.parentElement) - getElementOuterHeight(this._sidebarLabel.parentElement)) + "px";
  }

  public get handlesEvents(): string[] {
    const handleEvents = new Array<string>();
    handleEvents.push(StructureModelLoadedEvent.TYPE);
    handleEvents.push(DropdownButtonPressedEvent.TYPE);
    handleEvents.push(ProvideToolbarModelEvent.TYPE);
    handleEvents.push(LanguageModelLoadedEvent.TYPE);
    handleEvents.push(ShowContentEvent.TYPE);
    handleEvents.push(ProvideViewerSearcherEvent.TYPE);
    handleEvents.push(RequestStateEvent.TYPE);
    handleEvents.push(RestoreStateEvent.TYPE);
    return handleEvents;
  }

  public handle(e: ViewerEvent): void {
    if (e.type == StructureModelLoadedEvent.TYPE) {
      const smle = e as StructureModelLoadedEvent;
      this._model = smle.structureModel;
      this._textPresent = smle.structureModel._textContentPresent;
      this._toolbarLoadedLanguageModelLoadedSync(this);
      this._containerVisibleModelLoadedSync(this);
      return;
    }

    if (e.type == DropdownButtonPressedEvent.TYPE) {
      const dropdownButtonPressedEvent = e as DropdownButtonPressedEvent;

      if (dropdownButtonPressedEvent.childId == "search") {
        this.openSearch();
      }
      return;
    }

    if (e.type == ProvideToolbarModelEvent.TYPE) {
      const ptme = e as ProvideToolbarModelEvent;
      this._tbModel = ptme.model;
      this._toolbarLoadedLanguageModelLoadedSync(this);
      return;
    }

    if (e.type == LanguageModelLoadedEvent.TYPE) {
      const lmle = e as LanguageModelLoadedEvent;
      this._languageModel = lmle.languageModel;
      this._toolbarLoadedLanguageModelLoadedSync(this);
      return;
    }

    if (e.type == ShowContentEvent.TYPE) {
      const sce = e as ShowContentEvent;
      if (sce.containerDirection == ShowContentEvent.DIRECTION_EAST && sce.component !== this) {
        if (sce.size == 0) {
          this._searchResultCanvasPageLayer.clear();
          this.trigger(new RedrawEvent(this));
        } else if (this._searchAreaReady && this._toolbarTextInput.value.length > 0) {
          this._search(this._toolbarTextInput.value);
        }

      }
      return;
    }

    if (e.type == ProvideViewerSearcherEvent.TYPE) {
      const pvse = e as ProvideViewerSearcherEvent;
      this._searcher = pvse.searcher;
      this._containerVisibleModelLoadedSync(this);
      return;
    }

    if (e.type == RequestStateEvent.TYPE) {
      const rse = e as RequestStateEvent;
      if (this._searchAreaReady != null) {
        let searchText = this._toolbarTextInput.value;
        if (searchText != null && searchText != "") {
          rse.stateMap.set("q", searchText);
        }
      }
      return;
    }

    if (e.type == RestoreStateEvent.TYPE) {
      let rse = e as RestoreStateEvent;
      if (rse.restoredState.has("q")) {
        let q = rse.restoredState.get("q");
        this.openSearch();
        if (this._searchAreaReady != null) {
          this._toolbarTextInput.value = q;
        } else {
          this._valueToApply = q;
        }
      }
      return;
    }
  }

  private openSearch() {
    const direction = (this._settings.mobile) ? ShowContentEvent.DIRECTION_CENTER : ShowContentEvent.DIRECTION_EAST;
    this.trigger(new ShowContentEvent(this, this._container, direction, -1, this._sidebarLabel));
    this.updateContainerSize();

    this._containerVisible = true;
    this._containerVisibleModelLoadedSync(this);
  }


  private _prepareIndex(model: StructureModel) {
    this._model._imageList.forEach((image) => {
      this._imageHrefImageMap.set(image.href, image);
    });

    this._indexPreparing = true;
    this._searcher.index(model, (id, callback) => {
      // callback to receive ocr stuff
      this.trigger(new RequestTextContentEvent(this, id, callback));
    }, (x, ofY) => {
      // Callback to update progressbar
      this._updateLabel(x, ofY);
      // and to complete indexing
      if (ofY == (x)) {
        this._indexPrepared = true;
        this.initSearchArea();
      }
    });
  }

  private _updateLabel(current: number, of: number) {
    this._progressbarInner.setAttribute("aria-valuenow", current + "");
    this._progressbarInner.setAttribute("aria-valuemin", "0");
    this._progressbarInner.setAttribute("aria-valuemax", of + "");
    this._progressbarInner.style.width = ((current / of) * 100) + "%";
  }

  private _initProgressbar() {
    this._progressbar = document.createElement("div");
    this._progressbar.classList.add("progress");

    this._progressbarInner = document.createElement("div");
    this._progressbarInner.classList.add("progress-bar", "progress-bar-info");
    this._progressbar.append(this._progressbarInner);

    this._progressbarInner.setAttribute("role", "progressbar");
  }

}


