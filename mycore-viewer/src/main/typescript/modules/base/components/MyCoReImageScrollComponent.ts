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

import {ViewerComponent} from "./ViewerComponent";
import {MyCoReViewerSettings} from "../MyCoReViewerSettings";
import {ComponentInitializedEvent} from "./events/ComponentInitializedEvent";
import {WaitForEvent} from "./events/WaitForEvent";
import {StructureModelLoadedEvent} from "./events/StructureModelLoadedEvent";
import {ShowContentEvent} from "./events/ShowContentEvent";
import {ProvideToolbarModelEvent} from "./events/ProvideToolbarModelEvent";
import {ViewportInitializedEvent} from "./events/ViewportInitializedEvent";
import {
  getElementWidth,
  MoveVector,
  MyCoReMap,
  Position2D,
  Size2D,
  Utils,
  viewerClearTextSelection,
  ViewerParameterMap,
  ViewerProperty
} from "../Utils";
import {Scrollbar} from "../widgets/canvas/Scrollbar";
import {Overview} from "../widgets/canvas/Overview";
import {TextRenderer} from "../widgets/canvas/TextRenderer";
import {RequestTextContentEvent} from "./events/RequestTextContentEvent";
import {AbstractPage} from "./model/AbstractPage";
import {TextContentModel} from "./model/TextContent";
import {PageView} from "../widgets/canvas/PageView";
import {PageController} from "../widgets/canvas/PageController";

import {StructureImage} from "./model/StructureImage";
import {LanguageModel} from "./model/LanguageModel";
import {ToolbarButton} from "../widgets/toolbar/model/ToolbarButton";
import {ToolbarDropdownButton} from "../widgets/toolbar/model/ToolbarDropdownButton";

import {MyCoReBasicToolbarModel} from "./model/MyCoReBasicToolbarModel";
import {ImageChangedEvent} from "./events/ImageChangedEvent";
import {ImageSelectedEvent} from "./events/ImageSelectedEvent";
import {DropdownButtonPressedEvent} from "../widgets/toolbar/events/DropdownButtonPressedEvent";
import {ButtonPressedEvent} from "../widgets/toolbar/events/ButtonPressedEvent";
import {PageLayout} from "../widgets/canvas/PageLayout";
import {DesktopInputDelegator} from "../widgets/canvas/input/DesktopInputDelegator";
import {TouchInputDelegator} from "../widgets/canvas/input/TouchInputDelegator";
import {RequestStateEvent} from "./events/RequestStateEvent";
import {LanguageModelLoadedEvent} from "./events/LanguageModelLoadedEvent";
import {ProvidePageLayoutEvent} from "./events/ProvidePageLayoutEvent";
import {RequestDesktopInputEvent} from "./events/RequestDesktopInputEvent";
import {RequestTouchInputEvent} from "./events/RequestTouchInputEvent";
import {AddCanvasPageLayerEvent} from "./events/AddCanvasPageLayerEvent";
import {RedrawEvent} from "./events/RedrawEvent";
import {TextEditEvent} from "./events/TextEditEvent";
import {PageLayoutChangedEvent} from "./events/PageLayoutChangedEvent";
import {RequestPageEvent} from "./events/RequestPageEvent";
import {ViewerEvent} from "../widgets/events/ViewerEvent";
import {ZoomAnimation} from "../widgets/canvas/viewport/ZoomAnimation";
import {RestoreStateEvent} from "./events/RestoreStateEvent";
import {TouchInputAdapter, TouchInputListener} from "../widgets/canvas/input/TouchInputListener";
import {DesktopInputAdapter, DesktopInputListener} from "../widgets/canvas/input/DesktopInputListener";
import {TouchSession} from "../widgets/canvas/input/TouchSession";
import {VelocityScrollAnimation} from "../widgets/canvas/viewport/VelocityScrollAnimation";


/**
 * canvas.overview.enabled:boolean      if true the overview will be shown in the lower right corner
 */
export class MyCoReImageScrollComponent extends ViewerComponent {

  private _settings: MyCoReViewerSettings;

  constructor(_settings: MyCoReViewerSettings, private _container: HTMLElement) {
    super();
    this._settings = _settings;
  }

  public init() {
    if (!this.isImageDoctype()) {
      return;
    }

    this.changeImage(this._settings.filePath, false);
    this.trigger(new ComponentInitializedEvent(this));
    this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));
    this.trigger(new WaitForEvent(this, ShowContentEvent.TYPE));
    this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));
    this.trigger(new ViewportInitializedEvent(this, this._pageController.viewport));
    const viewParentElement = this._viewParentElement;
    viewParentElement.style.position = "absolute";
    viewParentElement.style.top = "0px";
    viewParentElement.style.left = "0px";
    viewParentElement.style.right = this._settings.mobile ? "0px" : "15px";
    viewParentElement.style.bottom = "15px";

    this.initMainView();

    const overviewEnabled = Utils.getVar(this._settings, "canvas.overview.enabled", true);
    if (!this._settings.mobile) {
      if (overviewEnabled) {
        this._viewParentElement.append(this._pageController._overview.container);
      }
      this._pageController._overview.initEventHandler();
      this._componentContent.push(this._horizontalScrollbar.scrollbarElement);
      this._componentContent.push(this._verticalScrollbar.scrollbarElement);
      this._componentContent.push(this._toggleButton);
      this.initOverview(overviewEnabled);
    }

    /* new code */
    viewParentElement.addEventListener("iviewResize", () => {
      this._horizontalScrollbar.resized();
      this._verticalScrollbar.resized();
      this._pageController.update();
    });

    this._pageController.viewport.positionProperty.addObserver({
      propertyChanged: (old: ViewerProperty<Position2D>, newPosition: ViewerProperty<Position2D>) => {
        this.update();
      }
    });


    this._pageController.viewport.scaleProperty.addObserver({
      propertyChanged: (old: ViewerProperty<number>, newScale: ViewerProperty<number>) => {
      }
    });


    this.trigger(new ShowContentEvent(this, this._componentContent, ShowContentEvent.DIRECTION_CENTER));
  }

  private isImageDoctype() {
    return this._settings.doctype === "mets" || this._settings.doctype === "pdf" || this._settings.doctype === "manifest";
  }

  private initOverview(overviewEnabled: any | boolean) {
    if (overviewEnabled) {
      const overviewContainer = this._pageController._overview.container;
      const minVisibleSize = parseInt(Utils.getVar(this._settings, "canvas.overview.minVisibleSize", MyCoReImageScrollComponent.DEFAULT_CANVAS_OVERVIEW_MIN_VISIBLE_SIZE, (value) => {
        return !isNaN((<any>value) * 1) && parseInt(value, 10) > 1;
      }), 10);

      const iconChildren = this._toggleButton.querySelectorAll('.fas');

      if (getElementWidth(this._container) < minVisibleSize) {
        overviewContainer.style.display = "none"
        iconChildren.forEach((iconChild) => {
          iconChild.classList.add(MyCoReImageScrollComponent.OVERVIEW_VISIBLE_ICON);
        });
      } else {
        iconChildren.forEach((iconChild) => {
          iconChild.classList.add(MyCoReImageScrollComponent.OVERVIEW_INVISIBLE_ICON);
        });
      }

      this._toggleButton.addEventListener('click', () => {
        if (overviewContainer.style.display != "none") {
          overviewContainer.style.display = "none";
          iconChildren.forEach((iconChild) => {
            iconChild.classList.replace(MyCoReImageScrollComponent.OVERVIEW_INVISIBLE_ICON, MyCoReImageScrollComponent.OVERVIEW_VISIBLE_ICON);
          })
        } else {
          overviewContainer.style.display = "block";
          iconChildren.forEach((iconChild) => {
            iconChild.classList.replace(MyCoReImageScrollComponent.OVERVIEW_VISIBLE_ICON, MyCoReImageScrollComponent.OVERVIEW_INVISIBLE_ICON);
          })
        }
      });
    } else {
      this._toggleButton.classList.add("disabled");
    }
  }

  private initMainView() {
    this.registerDesktopInputHandler(new DesktopInputHandler(this));
    this.registerTouchInputHandler(new TouchInputHandler(this));

    if (!this._settings.mobile) {

      this._horizontalScrollbar = new Scrollbar(true);
      this._verticalScrollbar = new Scrollbar(false);
      this._toggleButton = document.createElement(`div`);
      this._toggleButton.classList.add("overViewToggle");
      this._toggleButton.innerHTML = `<i class="fas"></i>`;

      this._pageController.viewport.sizeProperty.addObserver({
        propertyChanged: (_old, _new) => {
          this._horizontalScrollbar.update();
          this._verticalScrollbar.update();
        }
      });

      this._horizontalScrollbar.scrollHandler = this._verticalScrollbar.scrollHandler = () => {
        this._pageLayout.scrollhandler();
      };

      this._pageController._overview = new Overview(this._pageController.viewport);
    }

    this._viewParentElement.append(this._imageView.container);
    this._imageView.container.classList.add("mainView");
    this._imageView.container.style.left = "0px";
    this._imageView.container.style.right = "0px";
    this._viewParentElement.classList.add("grabbable");

    //this._componentContent.append(this._altoView.container);
    this._altoView.container.classList.add("secondView");
    this._altoView.container.style.left = "50%";
    this._altoView.container.style.right = "0px";
    this._altoView.container.style.borderLeft = "1px solid black";

    this._pageController.views.push(this._imageView);
    //this._pageController.views.push(this._altoView);

    if (this._settings.doctype == 'pdf') {
      let textRenderer = new TextRenderer(this._pageController.viewport,
        this._pageController.getPageArea(),
        this._imageView, (page: AbstractPage, contentProvider: (textContent: TextContentModel) => void) => {
          this.trigger(new RequestTextContentEvent(this, page.id, (id, model) => {
            contentProvider(model);
          }))
        }, (href) => {
          this.changeImage(href, true);
        });
      this._pageController.textRenderer = textRenderer;
    }
  }

  private setViewMode(mode: string): void {
    const remove = (view: PageView) => {
      let index = this._pageController.views.indexOf(view);
      if (index != -1) {
        this._pageController.views.splice(index, 1);
      }
      view.container.remove();
    };


    const add = (view: PageView) => {
      if (this._pageController.views.indexOf(view) == -1) {
        this._pageController.views.push(view);
      }
      if (view.container.parentElement != this._viewParentElement) {
        this._viewParentElement.append(view.container);
      }
    };

    this._viewMode = mode;
    if (mode == 'imageView') {
      this._imageView.container.style.left = "0px";
      this._imageView.container.style.right = "0px";
      remove(this._altoView);
      add(this._imageView);
      this.setSelectableButtonEnabled(false);
    } else if (mode == 'mixedView') {
      this._imageView.container.style.left = "0px";
      this._imageView.container.style.right = "50%";
      this._altoView.container.style.left = "50%";
      this._altoView.container.style.right = "0px";
      add(this._altoView);
      add(this._imageView);
      this.setSelectableButtonEnabled(true);
    } else if (mode == 'textView') {
      this._altoView.container.style.left = "0px";
      this._altoView.container.style.right = "0px";
      remove(this._imageView);
      add(this._altoView);
      this.setSelectableButtonEnabled(true);
    } else {
      console.warn("unknown view mode: " + mode);
    }

    this._pageController.update();
    this.updateToolbarLabel();

  }

  private _pageLayout: PageLayout = null;
  private _pageController: PageController = new PageController(true);


  private static ALTO_TEXT_HREF = "AltoHref";
  private static PDF_TEXT_HREF = "pdfText";
  private _hrefImageMap: MyCoReMap<string, StructureImage> = new MyCoReMap<string, StructureImage>();
  private _hrefPageMap = new MyCoReMap<string, AbstractPage>();
  private _orderImageMap = new MyCoReMap<number, StructureImage>();
  private _orderPageMap = new MyCoReMap<number, AbstractPage>();
  private _hrefPageLoadingMap = new MyCoReMap<string, boolean>();
  private _structureImages: Array<StructureImage> = null;
  private _currentImage: string;
  private _horizontalScrollbar: Scrollbar;
  private _verticalScrollbar: Scrollbar;
  private _languageModel: LanguageModel = null;
  private _rotateButton: ToolbarButton;
  private _layoutToolbarButton: ToolbarDropdownButton;

  private _desktopDelegators: Array<DesktopInputDelegator> = new Array<DesktopInputDelegator>();
  private _touchDelegators: Array<TouchInputDelegator> = new Array<TouchInputDelegator>();

  private _permalinkState: MyCoReMap<string, string> = null;
  private _toggleButton: HTMLElement;
  public _imageView: PageView = new PageView(true, false);
  private _altoView: PageView = new PageView(true, true);

  public _viewParentElement: HTMLElement = document.createElement("div");
  public _componentContent: HTMLElement[] = [ this._viewParentElement ];
  private _enableAltoSpecificButtons;
  private _selectionSwitchButton: ToolbarButton;
  private _viewSelectButton: ToolbarDropdownButton;
  private _viewMode: string = "imageView";

  private _toolbarModel: MyCoReBasicToolbarModel;
  private _layouts = new Array<PageLayout>();
  private _rotation: number = 0;
  private _startPage: number = null;

  private _layoutModel = { children: this._orderPageMap, hrefImageMap: this._hrefImageMap, pageCount: 1 };

  private _pageLoader = (order: number) => {
    if (this._orderImageMap.has(order)) {
      this.loadPageIfNotPresent(this._orderImageMap.get(order).href, order);
    } else {
      this.loadPageIfNotPresent(this._settings.filePath, order);
    }
  };

  // Size of a 300DPI A4 page
  private pageWidth = 2480;
  private pageHeight = 3508;
  private static DEFAULT_CANVAS_OVERVIEW_MIN_VISIBLE_SIZE = "800";
  private static OVERVIEW_VISIBLE_ICON = `fa-caret-up`;
  private static OVERVIEW_INVISIBLE_ICON = `fa-caret-down`;

  private changeImage(image: string, extern: boolean) {
    if (this._currentImage != image) {
      this._currentImage = image;
      let imageObj = this._hrefImageMap.get(image);

      if (extern) {
        this._pageLayout.jumpToPage(imageObj.order);
      }

      this.trigger(new ImageChangedEvent(this, imageObj));
    }

    if (this._settings.mobile) {
      this.trigger(new ShowContentEvent(this, this._componentContent, ShowContentEvent.DIRECTION_CENTER));
    }
  }

  private fitViewportOverPage() {
    this._pageLayout.fitToScreen();
  }

  private fitViewerportOverPageWidth() {
    this._pageLayout.fitToWidth();
  }

  public get handlesEvents(): string[] {
    const handleEvents = [];

    if (!this.isImageDoctype()) {
      return handleEvents;
    }

    handleEvents.push(ButtonPressedEvent.TYPE);
    handleEvents.push(DropdownButtonPressedEvent.TYPE);
    handleEvents.push(ImageSelectedEvent.TYPE);
    handleEvents.push(StructureModelLoadedEvent.TYPE);
    handleEvents.push(RequestStateEvent.TYPE);
    handleEvents.push(RestoreStateEvent.TYPE);
    handleEvents.push(ShowContentEvent.TYPE);
    handleEvents.push(LanguageModelLoadedEvent.TYPE);
    handleEvents.push(ProvideToolbarModelEvent.TYPE);
    handleEvents.push(ProvidePageLayoutEvent.TYPE);
    handleEvents.push(RequestDesktopInputEvent.TYPE);
    handleEvents.push(RequestTouchInputEvent.TYPE);
    handleEvents.push(AddCanvasPageLayerEvent.TYPE);
    handleEvents.push(RedrawEvent.TYPE);
    handleEvents.push(TextEditEvent.TYPE);

    return handleEvents;
  }

  public previousImage() {
    this._pageLayout.previous();
    this.update();
  }

  public nextImage() {
    this._pageLayout.next();
    this.update();
  }

  public getPageController() {
    return this._pageController;
  }

  public getPageLayout() {
    return this._pageLayout;
  }

  public getRotation() {
    return this._rotation;
  }

  public setRotation(rotation: number) {
    this._rotation = rotation;
  }

  private changePageLayout(pageLayout: PageLayout) {
    let page: number = NaN;
    if (this._pageLayout != null) {
      this._pageLayout.clear();
      page = this._pageLayout.getCurrentPage();
    }
    this._pageLayout = pageLayout;

    if (isNaN(page)) {
      page = 1;
      this._layoutModel.pageCount = 1;
    }
    this._pageLayout.jumpToPage(page);
    this._pageLayout.rotate(this._rotation);

    this.update();
    this.trigger(new PageLayoutChangedEvent(this, this._pageLayout));
  }

  private loadPageIfNotPresent(imageHref: string, order: number) {
    if (!this._hrefPageMap.has(imageHref) &&
      (!this._hrefPageLoadingMap.has(imageHref) || !this._hrefPageLoadingMap.get(imageHref))) {
      this._hrefPageLoadingMap.set(imageHref, true);

      let textHref: string = null;
      if (this._hrefImageMap.has(imageHref)) {
        const additionalHrefs = this._imageByHref(imageHref).additionalHrefs;
        if (additionalHrefs.has(MyCoReImageScrollComponent.ALTO_TEXT_HREF)) {
          textHref = additionalHrefs.get(MyCoReImageScrollComponent.ALTO_TEXT_HREF);
        } else if (additionalHrefs.has(MyCoReImageScrollComponent.PDF_TEXT_HREF)) {
          textHref = additionalHrefs.get(MyCoReImageScrollComponent.PDF_TEXT_HREF);
        }
      }

      this.trigger(new RequestPageEvent(this, imageHref, (href, page) => {
        this._hrefPageMap.set(href, page);

        if (this._hrefImageMap.has(href)) {
          this._orderPageMap.set(this._hrefImageMap.get(href).order, page);
        } else {
          this._orderPageMap.set(1, page);
        }

        this.update();

      }, textHref));
    }
  }

  private restorePermalink() {
    const state = this._permalinkState;

    if (state.has("layout")) {
      const layout = state.get("layout");
      const layoutObjects = this._layouts.filter(l => l.getLabelKey() == layout);
      if (layoutObjects.length != 1) {
        console.log("no matching layout found!");
      } else {
        this.changePageLayout(layoutObjects[0])

      }
    }

    if (state.has("page")) {
      const page = <number>+state.get("page");
      this._pageLayout.jumpToPage(page);
      this._startPage = page;
    }

    if (state.has("rotation")) {
      const rot = <number>+state.get("rotation");
      this._rotation = rot;
      this._pageLayout.rotate(rot);
    }

    if (state.has("scale")) {
      this._pageController.viewport.scale = <number>+state.get("scale");
    }

    if (state.has("x") && state.has("y")) {
      this._pageLayout.setCurrentPositionInPage(new Position2D(<number>+state.get("x"), <number>+state.get("y")));
    }

    this._permalinkState = null;
  }


  public handle(e: ViewerEvent): void {
    if (!this.isImageDoctype()) {
      return;
    }

    if (e.type == ProvideToolbarModelEvent.TYPE) {
      const ptme = e as ProvideToolbarModelEvent;
      this._rotateButton = ptme.model._rotateButton;
      this._layoutToolbarButton = ptme.model._layoutDropdownButton;
      this._toolbarModel = ptme.model;
      if (!this._settings.mobile) {
        if ("addViewSelectButton" in ptme.model || "addSelectionSwitchButton" in ptme.model) {
          this._enableAltoSpecificButtons = () => {
            if ("addViewSelectButton" in ptme.model) {
              (<any>ptme.model).addViewSelectButton();
              this._viewSelectButton = (<any>ptme.model).viewSelect;
            }
            if ("addSelectionSwitchButton" in ptme.model) {
              (<any>ptme.model).addSelectionSwitchButton();
              this._selectionSwitchButton = (<any>ptme.model).selectionSwitchButton;
            }

            this.updateToolbarLabel();
          };
        }
      }
    }

    if (e.type == LanguageModelLoadedEvent.TYPE) {
      const lmle = e as LanguageModelLoadedEvent;
      this._languageModel = lmle.languageModel;
      this.updateToolbarLabel();
    }

    if (e.type == ButtonPressedEvent.TYPE) {
      const buttonPressedEvent = e as ButtonPressedEvent;

      if (buttonPressedEvent.button.id == "PreviousImageButton") {
        this.previousImage();
      }

      if (buttonPressedEvent.button.id == "NextImageButton") {
        this.nextImage();
      }

      if (buttonPressedEvent.button.id == "ZoomInButton") {
        this._pageController.viewport.startAnimation(new ZoomAnimation(this._pageController.viewport, 2));
      }

      if (buttonPressedEvent.button.id == "ZoomOutButton") {
        this._pageController.viewport.startAnimation(new ZoomAnimation(this._pageController.viewport, 1 / 2));
      }

      if (buttonPressedEvent.button.id == "ZoomWidthButton") {
        this.fitViewerportOverPageWidth();
      }


      if (buttonPressedEvent.button.id == "ZoomFitButton") {
        this.fitViewportOverPage();
      }


      if (buttonPressedEvent.button.id == "RotateButton") {
        if (!buttonPressedEvent.button.disabled) {
          if (this._rotation == 270) {
            this._rotation = 0;
            this._pageLayout.rotate(0);
          } else {
            this._pageLayout.rotate(this._rotation += 90);
          }
        }
      }

      if (buttonPressedEvent.button.id == "selectionSwitchButton") {
        if (!buttonPressedEvent.button.active) {
          this.setAltoSelectable(true);
        } else {
          this.setAltoSelectable(false);
        }
      }
      return;
    }

    if (e.type == ProvidePageLayoutEvent.TYPE) {
      const pple = e as ProvidePageLayoutEvent;
      this.addLayout(pple.pageLayout);
      if (pple.isDefault) {
        this.changePageLayout(pple.pageLayout);
      }
      return;
    }

    if (e.type == DropdownButtonPressedEvent.TYPE) {
      const dbpe = e as DropdownButtonPressedEvent;

      if (dbpe.button.id == 'LayoutDropdownButton') {
        this._layouts.filter((e) => e.getLabelKey() == dbpe.childId).forEach((layout) => {
          this.changePageLayout(layout);
          this.updateToolbarLabel();
        });
      } else if (dbpe.button.id == 'viewSelect') {
        this.setViewMode(dbpe.childId);
        this.updateToolbarLabel();
      }
      return;
    }


    if (e.type == ImageSelectedEvent.TYPE) {
      let imageSelectedEvent = e as ImageSelectedEvent;
      this.changeImage(imageSelectedEvent.image.href, true);
      return;
    }

    if (e.type == StructureModelLoadedEvent.TYPE) {
      const structureModelLodedEvent = e as StructureModelLoadedEvent;
      const sm = structureModelLodedEvent.structureModel;
      this._structureImages = sm.imageList;

      if ("defaultPageDimension" in sm && sm.defaultPageDimension != null) {
        this.pageWidth = sm.defaultPageDimension.width;
        this.pageHeight = sm.defaultPageDimension.height;
      }

      this._structureModelLoaded();
      const modelStartPage = "startPage" in sm && this._startPage == null;
      this.changeImage(modelStartPage ? (<any>sm).startPage + "" : this._currentImage, modelStartPage);
      this.update();
    }

    if (e.type == RequestStateEvent.TYPE) {
      const requestStateEvent = e as RequestStateEvent;
      const state = requestStateEvent.stateMap;

      if (requestStateEvent.deepState) {
        const middle = this._pageLayout.getCurrentPositionInPage();
        state.set("x", middle.x.toString(10));
        state.set("y", middle.y.toString(10));
        state.set("scale", this._pageController.viewport.scale.toString(10));
        state.set("rotation", this._rotation.toString(10));
        state.set("layout", this._pageLayout.getLabelKey());
      }

      state.set("page", this._orderImageMap.get(this._pageLayout.getCurrentPage()).href);
      state.set("derivate", this._settings.derivate);
    }

    if (e.type == RequestDesktopInputEvent.TYPE) {
      const requestInputEvent = e as RequestDesktopInputEvent;
      this.registerDesktopInputHandler(requestInputEvent.listener);
    }

    if (e.type == RequestTouchInputEvent.TYPE) {
      let requestInputEvent = e as RequestTouchInputEvent;
      this.registerTouchInputHandler(requestInputEvent.listener);
    }

    if (e.type == RestoreStateEvent.TYPE) {
      const rste = e as RestoreStateEvent;
      this._permalinkState = rste.restoredState;
    }

    if (e.type == AddCanvasPageLayerEvent.TYPE) {
      const acple = e as AddCanvasPageLayerEvent;
      this._pageController.addCanvasPageLayer(acple.zIndex, acple.canvasPageLayer);
    }

    if (e.type == RedrawEvent.TYPE) {
      this._pageController.update();
    }

    if (e.type === TextEditEvent.TYPE) {
      const tee = e as TextEditEvent;
      if (tee.component !== this) {
        //this.setAltoSelectable(tee.edit);
        this.setAltoOnTop(tee.edit);
        if (tee.edit && this._viewMode == 'imageView') {
          this.setViewMode("mixedView")
        }
      }
    }

  }

  private setAltoOnTop(onTop: boolean) {
    this.setAltoSelectable(false);
    if (onTop) {
      this._altoView.container.classList.add("altoTop");
      this._selectionSwitchButton.disabled = true;
    } else {
      this._altoView.container.classList.remove("altoTop");
      this._selectionSwitchButton.disabled = false;
    }
  }

  public isAltoSelectable() {
    return this._selectionSwitchButton != null && this._selectionSwitchButton.active;
  }

  private setAltoSelectable(selectable: boolean) {
    this._selectionSwitchButton.active = selectable;
    const switchButton = document.querySelector("[data-id='selectionSwitchButton']") as HTMLElement;
    if (switchButton) {
      switchButton.blur();
    }
    this._altoView.container.classList.remove("altoTop");

    if (selectable) {
      this._altoView.container.classList.add("altoSelectable");
    } else {
      this._altoView.container.classList.remove("altoSelectable");
    }

    viewerClearTextSelection();
  }

  private setSelectableButtonEnabled(enabled: boolean) {
    if (enabled) {
      if (this._toolbarModel._actionControllGroup.getComponents().indexOf(this._selectionSwitchButton) == -1) {
        this._toolbarModel._actionControllGroup.addComponent(this._selectionSwitchButton);
      }
    } else {
      if (this._toolbarModel._actionControllGroup.getComponents().indexOf(this._selectionSwitchButton) != -1) {
        this._toolbarModel._actionControllGroup.removeComponent(this._selectionSwitchButton);
      }
    }
  }

  private addLayout(layout: PageLayout) {
    this._layouts.push(layout);
    layout.init(this._layoutModel, this._pageController, new Size2D(this.pageWidth, this.pageHeight), this._horizontalScrollbar, this._verticalScrollbar, this._pageLoader);
    this.synchronizeLayoutToolbarButton();
  }

  private synchronizeLayoutToolbarButton() {
    let changed = false;
    this._layouts.forEach((layout) => {
      const id = layout.getLabelKey();
      const childrenWithId = this._layoutToolbarButton.children.filter((c) => c.id == id);

      if (childrenWithId.length == 0) {
        this._layoutToolbarButton.children.push({
          id: id,
          label: id
        });
        changed = true;
      }
    });

    if (changed) {
      this._layoutToolbarButton.children = this._layoutToolbarButton.children; // needed to trigger a change
      this.updateToolbarLabel();
    }
  }

  private updateToolbarLabel() {
    if (this._languageModel != null) {
      if (this._layoutToolbarButton != null) { // dont need to translate the layoutbuttons if  there is only one layout
        this._layoutToolbarButton.children.forEach((e) => {
          e.label = this._languageModel.getTranslation("layout." + e.id);
        });

        this._layoutToolbarButton.children = this._layoutToolbarButton.children;
        if (this._pageLayout != null) {
          this._layoutToolbarButton.label = this._languageModel.getTranslation("layout." + this._pageLayout.getLabelKey());
        }
      }

      if (this._viewSelectButton != null) {
        this._viewSelectButton.label = this._languageModel.getTranslation("view." + this._viewMode);

        this._viewSelectButton.children.forEach((child) => {
          child.label = this._languageModel.getTranslation("view." + child.id);
        });
        this._viewSelectButton.children = this._viewSelectButton.children;
      }


    }
  }

  private update() {
    if (this._pageLayout == null) {
      return;
    }

    const newImageOrder = this._pageLayout.getCurrentPage();
    this._pageLayout.syncronizePages();
    if (!this._settings.mobile) {
      this._pageController._overview.overviewRect = this._pageLayout.getCurrentOverview();
    }

    if (typeof newImageOrder == "undefined" || !this._orderImageMap.has(newImageOrder)) {
      return;
    }

    this.changeImage(this._orderImageMap.get(newImageOrder).href, false);
  }

  private _structureModelLoaded() {
    let altoPresent = false;
    for (const image of this._structureImages) {
      this._hrefImageMap.set(image.href, image);
      this._orderImageMap.set(image.order, image);
      altoPresent = altoPresent || image.additionalHrefs.has("AltoHref");
    }


    if (this._orderPageMap.has(1)) {
      const firstPage = this._orderPageMap.get(1);
      this._orderPageMap.remove(1);
      const img = this._hrefImageMap.get(this._settings.filePath);
      this._orderPageMap.set(img.order, firstPage);
    }

    this.initPageLayouts();

    if (altoPresent && !this._settings.mobile) {
      // enable button
      this._enableAltoSpecificButtons();
    }
  }

  private initPageLayouts() {
    const key = this._settings.filePath;
    const position = this._pageLayout.getCurrentPositionInPage();
    const scale = this._pageController.viewport.scale;
    this._pageController.viewport.stopAnimation();
    this._desktopDelegators.forEach(delegator => delegator.clearRunning());
    this._touchDelegators.forEach(delegator => delegator.clearRunning());

    if (!this._settings.mobile) {
      this._horizontalScrollbar.clearRunning();
      this._verticalScrollbar.clearRunning();
    }

    this._pageLayout.clear();
    this._layoutModel.pageCount = this._structureImages.length;

    let order;
    if (this._hrefImageMap.has(key)) {
      order = this._hrefImageMap.get(key).order;
    } else {
      const url = ViewerParameterMap.fromCurrentUrl();
      if (url.has("page")) {
        order = parseInt(url.get("page"));
      } else {
        order = 1;

      }
    }

    if (this._pageLayout == null) {
      throw "no default page layout found";
    }
    this._pageLayout.jumpToPage(order);
    this._pageController.viewport.scale = scale;
    this._pageLayout.setCurrentPositionInPage(position);
    this._pageController._updateSizeIfChanged();

    if (Utils.getVar(this._settings, "canvas.startup.fitWidth", false)) {
      this._pageLayout.fitToWidth(true);
    } else {
      this._pageLayout.fitToScreen();
    }

    this.trigger(new WaitForEvent(this, RestoreStateEvent.TYPE));
    if (this._permalinkState != null) {
      this.restorePermalink();
    }
    const image = this._orderImageMap.get(this._pageLayout.getCurrentPage());
    this.trigger(new ImageChangedEvent(this, image));
  }


  private _imageByHref(href: string) {
    return this._hrefImageMap.get(href);
  }

  private registerDesktopInputHandler(listener: DesktopInputListener) {
    this._desktopDelegators.push(new DesktopInputDelegator(this._imageView.container, this._pageController.viewport, listener));
    this._desktopDelegators.push(new DesktopInputDelegator(this._altoView.container, this._pageController.viewport, listener));
  }

  private registerTouchInputHandler(listener: TouchInputListener) {
    this._touchDelegators.push(new TouchInputDelegator(this._imageView.container, this._pageController.viewport, listener));
    this._touchDelegators.push(new TouchInputDelegator(this._altoView.container, this._pageController.viewport, listener));
  }


}

class TouchInputHandler extends TouchInputAdapter {

  private _touchAdditionalScaleMove: MoveVector = null;
  private _sessionStartRotation: number = 0;

  constructor(public component: MyCoReImageScrollComponent) {
    super();
  }

  public touchStart(session: TouchSession): void {
    this._touchAdditionalScaleMove = new MoveVector(0, 0);
    this.component.getPageController().viewport.stopAnimation();
  }

  public touchMove(session: TouchSession): void {
    const viewPort = this.component.getPageController().viewport;
    if (!session.touchLeft) {
      if (session.touches == 2 && session.startDistance > 150 && session.currentMove.distance > 150) {
        const diff = session.currentMove.angle - session.startAngle;
        const fullNewAngle = (360 * 2 + (this._sessionStartRotation + diff)) % 360;
        let result = Math.round(fullNewAngle / 90) * 90;
        result = (result == 360) ? 0 : result;
        if (this.component.getRotation() != result) {
          this.component.getPageLayout().rotate(result);
          this.component.setRotation(result);
        }
      }

      if (session.startDistance != 0 && session.currentMove.distance != 0 && session.touches > 1) {
        let lastDistance = 0;
        if (session.lastMove == null || session.lastMove.distance == 0) {
          lastDistance = session.startDistance;
        } else {
          lastDistance = session.lastMove.distance;
        }

        const relativeScale = session.currentMove.distance / lastDistance;
        const touchMiddle = viewPort.getAbsolutePosition(session.currentMove.middle);
        const positionTouchDifference = new MoveVector(viewPort.position.x - touchMiddle.x, viewPort.position.y - touchMiddle.y);
        const newPositionTouchDifference = positionTouchDifference.scale(relativeScale);
        const newPositionAfterScale = touchMiddle.move(newPositionTouchDifference);
        this._touchAdditionalScaleMove = this._touchAdditionalScaleMove.move(new MoveVector(viewPort.position.x - newPositionAfterScale.x, viewPort.position.y - newPositionAfterScale.y));
        viewPort.scale *= relativeScale;
      }

      const move = new MoveVector(-(session.currentMove.middle.x - session.startMiddle.x), -(session.currentMove.middle.y - session.startMiddle.y));
      const rotation = viewPort.rotation;
      const scale = viewPort.scale;
      viewPort.position = session.canvasStartPosition.copy().scale(scale).move(move.rotate(rotation)).scale(1 / scale).move(this._touchAdditionalScaleMove);
    }
  }

  public touchEnd(session: TouchSession): void {
    const viewPort = this.component.getPageController().viewport;

    if (session.currentMove != null) {
      if (session.currentMove.velocity.x != 0 || session.currentMove.velocity.y != 0) {
        const anim = new VelocityScrollAnimation(this.component.getPageController().viewport, session.currentMove.velocity);
        this.component.getPageController().viewport.startAnimation(anim);
      }
    }

    if (session.lastSession != null) {
      if (session.startTime - session.lastSession.startTime < 200) {
        const currentMiddle = session.startMiddle;
        const newPosition = viewPort.getAbsolutePosition(currentMiddle);
        viewPort.startAnimation(new ZoomAnimation(viewPort, 2, newPosition, 500));
      } else {
        if (session.canvasStartPosition.equals(this.component.getPageController().viewport.position)) {
        }
      }
    }
    this._sessionStartRotation = this.component.getRotation();
  }
}

class DesktopInputHandler extends DesktopInputAdapter {

  constructor(public component: MyCoReImageScrollComponent) {
    super();
  }

  public mouseDown(mousePosition, e): void {
    let container = this.component._viewParentElement;
    container.classList.remove("grabbable");
    container.classList.add("grab");
  }

  public mouseUp(mousePosition, e): void {
    let container = this.component._viewParentElement;
    container.classList.add("grabbable");
    container.classList.remove("grab");
  }

  public mouseDoubleClick(mousePosition: Position2D): void {
    const vp = this.component.getPageController().viewport;
    const position: Position2D = vp.getAbsolutePosition(mousePosition);
    vp.startAnimation(new ZoomAnimation(vp, 2, position));
  }

  public mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
    event: MouseEvent): void {
    const isAltoSelectable = this.component.isAltoSelectable();
    const isImageCanvas = event.target === this.component._imageView.drawCanvas ||
      event.target === this.component._imageView.markCanvas;
    if (!isImageCanvas && isAltoSelectable) {
      return;
    }
    const xMove = currentPosition.x - startPosition.x;
    const yMove = currentPosition.y - startPosition.y;
    const move = new MoveVector(-xMove, -yMove).rotate(this.component.getPageController().viewport.rotation);
    this.component.getPageController().viewport.position = startViewport
      .scale(this.component.getPageController().viewport.scale)
      .move(move)
      .scale(1 / this.component.getPageController().viewport.scale);
  }

  public scroll(e: {
    deltaX: number;
    deltaY: number;
    orig: any;
    pos: Position2D;
    altKey?: boolean,
    ctrlKey?: boolean
  }) {
    const zoomParameter = (ViewerParameterMap.fromCurrentUrl().get("iview2.scroll") == "zoom");
    const zoom = (zoomParameter) ? !e.altKey || e.ctrlKey : e.altKey || e.ctrlKey;
    const vp = this.component.getPageController().viewport;

    if (zoom) {
      const relative = Math.pow(0.95, (e.deltaY / 10));
      if (typeof vp.currentAnimation != "undefined" && vp.currentAnimation != null) {
        if (vp.currentAnimation instanceof ZoomAnimation) {
          (vp.currentAnimation as ZoomAnimation).merge(relative);
        } else {
          console.log("dont know howto merge animations");
        }

      } else {
        const position = vp.getAbsolutePosition(e.pos);
        vp.startAnimation(new ZoomAnimation(vp, relative, position));
      }
    } else {
      vp.position = new Position2D(vp.position.x + (e.deltaX / vp.scale), vp.position.y + (e.deltaY / vp.scale));
    }

  }

  public keydown(e: KeyboardEvent): void {
    switch (e.keyCode) {
      case 33:
        this.component.previousImage();
        break;
      case 34:
        this.component.nextImage();
        break;
      default:
    }
  }

}


