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

/// <reference path="../definitions/jquery.d.ts" />
/// <reference path="../../base/Utils.ts" />
/// <reference path="ViewerComponent.ts" />
/// <reference path="events/ComponentInitializedEvent.ts" />
/// <reference path="events/ImageSelectedEvent.ts" />
/// <reference path="events/ImageChangedEvent.ts" />
/// <reference path="events/StructureModelLoadedEvent.ts" />
/// <reference path="events/WaitForEvent.ts" />
/// <reference path="events/CanvasTapedEvent.ts" />
/// <reference path="events/RequestStateEvent.ts" />
/// <reference path="events/RequestTextContentEvent.ts" />
/// <reference path="events/RestoreStateEvent.ts" />
/// <reference path="events/ShowContentEvent.ts" />
/// <reference path="events/ProvidePageLayoutEvent.ts" />
/// <reference path="events/LanguageModelLoadedEvent.ts" />
/// <reference path="events/RequestPageEvent.ts" />
/// <reference path="events/PageLayoutChangedEvent.ts" />
/// <reference path="events/RequestDesktopInputEvent.ts" />
/// <reference path="events/RequestTouchInputEvent.ts" />
/// <reference path="events/AddCanvasPageLayerEvent.ts" />
/// <reference path="events/TextEditEvent.ts" />
/// <reference path="events/RedrawEvent.ts" />
/// <reference path="events/UpdateURLEvent.ts" />
/// <reference path="../MyCoReViewerSettings.ts" />
/// <reference path="../widgets/events/ViewerEvent.ts" />
/// <reference path="model/StructureImage.ts" />
/// <reference path="../widgets/events/ViewerEventManager.ts" />
/// <reference path="../widgets/canvas/PageController.ts" />
/// <reference path="../widgets/canvas/PageLayout.ts" />
/// <reference path="../widgets/canvas/SinglePageLayout.ts" />
/// <reference path="../widgets/canvas/viewport/ZoomAnimation.ts" />
/// <reference path="../widgets/canvas/viewport/VelocityScrollAnimation.ts" />‚
/// <reference path="../widgets/canvas/viewport/ViewportTools.ts" />
/// <reference path="model/AbstractPage.ts" />
/// <reference path="../widgets/canvas/input/DesktopInputDelegator.ts" />
/// <reference path="../widgets/canvas/input/DesktopInputListener.ts" />
/// <reference path="../widgets/canvas/input/TouchInputDelegator.ts" />
/// <reference path="../widgets/canvas/input/TouchInputListener.ts" />
/// <reference path="../widgets/canvas/input/TouchSession.ts" />
/// <reference path="../components/events/ViewportInitializedEvent.ts" />


namespace mycore.viewer.components {

    import RequestTextContentEvent = mycore.viewer.components.events.RequestTextContentEvent;
    /**
     * canvas.overview.enabled:boolean      if true the overview will be shown in the lower right corner
     */
    export class MyCoReImageScrollComponent extends ViewerComponent {

        constructor(private _settings: MyCoReViewerSettings, private _container: JQuery) {
            super();
        }

        public init() {
            this.changeImage(this._settings.filePath, false);
            this.trigger(new events.ComponentInitializedEvent(this));
            this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.ShowContentEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
            this.trigger(new events.ViewportInitializedEvent(this, this._pageController.viewport));
            var componentContent = this._componentContent;
            componentContent.css({position : "absolute", top : "0px", left : "0px", right : "15px", bottom : "15px"});
            this.initMainView();

            var overviewEnabled = Utils.getVar(this._settings, "canvas.overview.enabled", true);
            if (!this._settings.mobile) {
                if (overviewEnabled) {
                    componentContent.append(this._pageController._overview.container);
                }
                this._pageController._overview.initEventHandler();
                componentContent = componentContent.add(this._horizontalScrollbar.scrollbarElement);
                componentContent = componentContent.add(this._verticalScrollbar.scrollbarElement);
                componentContent = componentContent.add(this._toggleButton);
                this.initOverview(overviewEnabled);
            }


            jQuery(componentContent).bind("iviewResize", () => {
                this._horizontalScrollbar.resized();
                this._verticalScrollbar.resized();
                this._pageController.update();
            });

            this._pageController.viewport.positionProperty.addObserver({
                propertyChanged : (old: ViewerProperty<Position2D>, newPosition: ViewerProperty<Position2D>) => {
                    this.update();
                }
            });


            this._pageController.viewport.scaleProperty.addObserver({
                propertyChanged : (old: ViewerProperty<number>, newScale: ViewerProperty<number>) => {
                }
            });


            this.trigger(new events.ShowContentEvent(this, componentContent, events.ShowContentEvent.DIRECTION_CENTER));
        }

        private initOverview(overviewEnabled: any | any | any | boolean) {
            if (overviewEnabled) {
                var overviewContainer = this._pageController._overview.container;
                var minVisibleSize = parseInt(Utils.getVar(this._settings, "canvas.overview.minVisibleSize", MyCoReImageScrollComponent.DEFAULT_CANVAS_OVERVIEW_MIN_VISIBLE_SIZE, (value) => {
                    return !isNaN((<any>value) * 1) && parseInt(value, 10) > 1;
                }), 10);

                var iconChild = this._toggleButton.children(".glyphicon");
                if (this._container.width() < minVisibleSize) {
                    jQuery(overviewContainer).hide();
                    iconChild.addClass(MyCoReImageScrollComponent.OVERVIEW_VISIBLE_ICON);
                } else {
                    iconChild.addClass(MyCoReImageScrollComponent.OVERVIEW_INVISIBLE_ICON);
                }

                this._toggleButton.click(() => {
                    if (jQuery(overviewContainer).is(":visible")) {
                        jQuery(overviewContainer).hide();
                        iconChild.addClass(MyCoReImageScrollComponent.OVERVIEW_VISIBLE_ICON);
                        iconChild.removeClass(MyCoReImageScrollComponent.OVERVIEW_INVISIBLE_ICON);
                    } else {
                        jQuery(overviewContainer).show();
                        iconChild.addClass(MyCoReImageScrollComponent.OVERVIEW_INVISIBLE_ICON);
                        iconChild.removeClass(MyCoReImageScrollComponent.OVERVIEW_VISIBLE_ICON);
                    }
                });
            } else {
                this._toggleButton.addClass("disabled");
            }
        }

        private initMainView() {
            this.registerDesktopInputHandler(new DesktopInputHandler(this));
            this.registerTouchInputHandler(new TouchInputHandler(this));

            if (!this._settings.mobile) {

                this._horizontalScrollbar = new widgets.canvas.Scrollbar(true);
                this._verticalScrollbar = new widgets.canvas.Scrollbar(false);
                this._toggleButton = jQuery(`<div class='overViewToggle'><div class="glyphicon"></div></div>`);

                this._pageController.viewport.sizeProperty.addObserver({
                    propertyChanged : (_old, _new) => {
                        this._horizontalScrollbar.update();
                        this._verticalScrollbar.update();
                    }
                });

                this._horizontalScrollbar.scrollHandler = this._verticalScrollbar.scrollHandler = () => {
                    this._pageLayout.scrollhandler();
                };

                this._pageController._overview = new widgets.canvas.Overview(this._pageController.viewport);
            }

            this._componentContent.append(this._imageView.container);
            this._imageView.container.addClass("mainView");
            this._imageView.container.css({left : "0px", right : "0px"});
            this._componentContent.addClass("grabbable");

            //this._componentContent.append(this._altoView.container);
            this._altoView.container.addClass("secondView");
            this._altoView.container.css({left : "50%", right : "0px"});
            this._altoView.container.css({
                "border-left" : "1px solid black"
            });

            this._pageController.views.push(this._imageView);
            //this._pageController.views.push(this._altoView);

            if (this._settings.doctype == 'pdf') {
                let textRenderer = new widgets.canvas.TextRenderer(this._pageController.viewport, this._pageController.getPageArea(), this._imageView, (page: model.AbstractPage, contentProvider: (textContent: model.TextContentModel) => void) => {
                    this.trigger(new RequestTextContentEvent(this, page.id, (id, model) => {
                        contentProvider(model);
                    }))
                });
                this._pageController.textRenderer = textRenderer;
            }
        }

        private setViewMode(mode: string): void {
            var remove = (view: widgets.canvas.PageView) => {
                let index = this._pageController.views.indexOf(view);
                if (index != -1) {
                    this._pageController.views.splice(index, 1);
                }
                view.container.detach();
            };


            var add = (view: widgets.canvas.PageView) => {
                if (this._pageController.views.indexOf(view) == -1) {
                    this._pageController.views.push(view);
                }
                if (view.container.parent() != this._componentContent) {
                    this._componentContent.append(view.container);
                }
            };

            this._viewMode = mode;
            if (mode == 'imageView') {
                this._imageView.container.css({"left" : "0px", "right" : "0px"});
                remove(this._altoView);
                add(this._imageView);
                this.setSelectableButtonEnabled(false);
            } else if (mode == 'mixedView') {
                this._imageView.container.css({"left" : "0px", "right" : "50%"});
                this._altoView.container.css({"left" : "50%", "right" : "0px"});
                add(this._altoView);
                add(this._imageView);
                this.setSelectableButtonEnabled(true);
            } else if (mode == 'textView') {
                this._altoView.container.css({"left" : "0px", "right" : "0px"});
                remove(this._imageView);
                add(this._altoView);
                this.setSelectableButtonEnabled(true);
            } else {
                console.warn("unknown view mode: " + mode);
            }

            this._pageController.update();
            this.updateToolbarLabel();

        }

        private _pageLayout: widgets.canvas.PageLayout = null;
        private _pageController: widgets.canvas.PageController = new mycore.viewer.widgets.canvas.PageController(true);


        private static ALTO_TEXT_HREF = "AltoHref";
        private static PDF_TEXT_HREF = "pdfText";
        private _hrefImageMap: MyCoReMap<string, model.StructureImage> = new MyCoReMap<string, model.StructureImage>();
        private _hrefPageMap = new MyCoReMap<string, model.AbstractPage>();
        private _orderImageMap = new MyCoReMap<number, model.StructureImage>();
        private _orderPageMap = new MyCoReMap<number, model.AbstractPage>();
        private _hrefPageLoadingMap = new MyCoReMap<string, boolean>();
        private _structureImages: Array<model.StructureImage> = null;
        private _currentImage: string;
        private _horizontalScrollbar: widgets.canvas.Scrollbar;
        private _verticalScrollbar: widgets.canvas.Scrollbar;
        private _languageModel: model.LanguageModel = null;
        private _rotateButton: widgets.toolbar.ToolbarButton;
        private _layoutToolbarButton: widgets.toolbar.ToolbarDropdownButton;

        private _desktopDelegators: Array<widgets.canvas.DesktopInputDelegator> = new Array<widgets.canvas.DesktopInputDelegator>();
        private _touchDelegators: Array<widgets.canvas.TouchInputDelegator> = new Array<widgets.canvas.TouchInputDelegator>();

        private _permalinkState: MyCoReMap<string, string> = null;
        private _toggleButton: JQuery;
        public _imageView: widgets.canvas.PageView = new widgets.canvas.PageView(true, false);
        private _altoView: widgets.canvas.PageView = new widgets.canvas.PageView(true, true);
        public _componentContent: JQuery = jQuery("<div></div>");
        private _enableAltoSpecificButtons;
        private _selectionSwitchButton: mycore.viewer.widgets.toolbar.ToolbarButton;
        private _viewSelectButton: widgets.toolbar.ToolbarDropdownButton;
        private _viewMode: string = "imageView";

        private _toolbarModel: mycore.viewer.model.MyCoReBasicToolbarModel;
        private _layouts = new Array<widgets.canvas.PageLayout>();
        private _rotation: number = 0;

        private _layoutModel = {children : this._orderPageMap, hrefImageMap : this._hrefImageMap, pageCount : 1};

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
        private static OVERVIEW_VISIBLE_ICON = `glyphicon-triangle-top`;
        private static OVERVIEW_INVISIBLE_ICON = `glyphicon-triangle-bottom`;

        private changeImage(image: string, extern: boolean) {
            if (this._currentImage != image) {
                this._currentImage = image;
                let imageObj = this._hrefImageMap.get(image);

                if (extern) {
                    this._pageLayout.jumpToPage(imageObj.order);
                }

                this.trigger(new events.ImageChangedEvent(this, imageObj));
            }

            if (this._settings.mobile) {
                this.trigger(new events.ShowContentEvent(this, jQuery(this._componentContent), events.ShowContentEvent.DIRECTION_CENTER));
            }
        }

        private fitViewportOverPage() {
            this._pageLayout.fitToScreen();
        }

        private fitViewerportOverPageWidth() {
            this._pageLayout.fitToWidth();
        }

        public get handlesEvents(): string[] {
            var handleEvents = [];

            handleEvents.push(mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE);
            handleEvents.push(mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE);
            handleEvents.push(mycore.viewer.components.events.ImageSelectedEvent.TYPE);
            handleEvents.push(events.StructureModelLoadedEvent.TYPE);
            handleEvents.push(events.RequestStateEvent.TYPE);
            handleEvents.push(events.RestoreStateEvent.TYPE);
            handleEvents.push(events.ShowContentEvent.TYPE);
            handleEvents.push(events.LanguageModelLoadedEvent.TYPE);
            handleEvents.push(events.ProvideToolbarModelEvent.TYPE);
            handleEvents.push(events.ProvidePageLayoutEvent.TYPE);
            handleEvents.push(events.RequestDesktopInputEvent.TYPE);
            handleEvents.push(events.RequestTouchInputEvent.TYPE);
            handleEvents.push(events.AddCanvasPageLayerEvent.TYPE);
            handleEvents.push(events.RedrawEvent.TYPE);
            handleEvents.push(events.TextEditEvent.TYPE);

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

        private changePageLayout(pageLayout: widgets.canvas.PageLayout) {
            if (this._pageLayout != null) {
                this._pageLayout.clear();
                var page = this._pageLayout.getCurrentPage();
            }
            this._pageLayout = pageLayout;

            if (isNaN(page)) {
                var page = 1;
                this._layoutModel.pageCount = 1;
            }
            this._pageLayout.jumpToPage(page);
            this._pageLayout.rotate(this._rotation);

            this.update();
            this.trigger(new events.PageLayoutChangedEvent(this, this._pageLayout));
        }

        private loadPageIfNotPresent(imageHref: string, order: number) {
            if (!this._hrefPageMap.has(imageHref) &&
                (!this._hrefPageLoadingMap.has(imageHref) || !this._hrefPageLoadingMap.get(imageHref))) {
                this._hrefPageLoadingMap.set(imageHref, true);

                var textHref: string = null;
                if (this._hrefImageMap.has(imageHref)) {
                    var additionalHrefs = this._imageByHref(imageHref).additionalHrefs;
                    if (additionalHrefs.has(MyCoReImageScrollComponent.ALTO_TEXT_HREF)) {
                        textHref = additionalHrefs.get(MyCoReImageScrollComponent.ALTO_TEXT_HREF);
                    } else if (additionalHrefs.has(MyCoReImageScrollComponent.PDF_TEXT_HREF)) {
                        textHref = additionalHrefs.get(MyCoReImageScrollComponent.PDF_TEXT_HREF);
                    }
                }

                this.trigger(new events.RequestPageEvent(this, imageHref, (href, page) => {
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
            var state = this._permalinkState;

            if (state.has("layout")) {
                var layout = state.get("layout");
                var layoutObjects = this._layouts.filter(l => l.getLabelKey() == layout);
                if (layoutObjects.length != 1) {
                    console.log("no matching layout found!");
                } else {
                    this.changePageLayout(layoutObjects[ 0 ])

                }
            }

            if (state.has("page")) {
                var page = <number>+state.get("page");
                this._pageLayout.jumpToPage(page);
            }

            if (state.has("rotation")) {
                var rot = <number>+state.get("rotation");
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


        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent>e;
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

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                var lmle = <events.LanguageModelLoadedEvent>e;
                this._languageModel = lmle.languageModel;
                this.updateToolbarLabel();
            }

            if (e.type == mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE) {
                var buttonPressedEvent = <mycore.viewer.widgets.toolbar.events.ButtonPressedEvent> e;

                if (buttonPressedEvent.button.id == "PreviousImageButton") {
                    this.previousImage();
                }

                if (buttonPressedEvent.button.id == "NextImageButton") {
                    this.nextImage();
                }

                if (buttonPressedEvent.button.id == "ZoomInButton") {
                    this._pageController.viewport.startAnimation(new widgets.canvas.ZoomAnimation(this._pageController.viewport, 2));
                }

                if (buttonPressedEvent.button.id == "ZoomOutButton") {
                    this._pageController.viewport.startAnimation(new widgets.canvas.ZoomAnimation(this._pageController.viewport, 1 / 2));
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

            }

            if (e.type == events.ProvidePageLayoutEvent.TYPE) {
                var pple = <events.ProvidePageLayoutEvent> e;
                this.addLayout(pple.pageLayout);
                if (pple.isDefault) {
                    this.changePageLayout(pple.pageLayout);
                }
            }

            if (e.type == widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
                var dbpe = <widgets.toolbar.events.DropdownButtonPressedEvent> e;

                if (dbpe.button.id == 'LayoutDropdownButton') {
                    this._layouts.filter((e) => e.getLabelKey() == dbpe.childId).forEach((layout) => {
                        this.changePageLayout(layout);
                        this.updateToolbarLabel();
                    });
                } else if (dbpe.button.id == 'viewSelect') {
                    this.setViewMode(dbpe.childId);
                    this.updateToolbarLabel();
                }
            }


            if (e.type == mycore.viewer.components.events.ImageSelectedEvent.TYPE) {
                let imageSelectedEvent = <mycore.viewer.components.events.ImageSelectedEvent>e;
                this.changeImage(imageSelectedEvent.image.href, true);
            }

            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var structureModelLodedEvent = <events.StructureModelLoadedEvent>e;
                this._structureImages = structureModelLodedEvent.structureModel.imageList;

                if ("defaultPageDimension" in structureModelLodedEvent.structureModel) {
                    this.pageWidth = structureModelLodedEvent.structureModel.defaultPageDimension.width;
                    this.pageHeight = structureModelLodedEvent.structureModel.defaultPageDimension.height;
                }

                this._structureModelLoaded();
                this.changeImage(this._currentImage, false);
                this.update();
            }

            if (e.type == events.RequestStateEvent.TYPE) {
                var requestStateEvent = (<events.RequestStateEvent>e);
                var state = requestStateEvent.stateMap;

                if (requestStateEvent.deepState) {
                    var middle = this._pageLayout.getCurrentPositionInPage();
                    state.set("x", middle.x.toString(10));
                    state.set("y", middle.y.toString(10));
                    state.set("scale", this._pageController.viewport.scale.toString(10));
                    state.set("rotation", this._rotation.toString(10));
                    state.set("layout", this._pageLayout.getLabelKey());
                }

                state.set("page", this._orderImageMap.get(this._pageLayout.getCurrentPage()).href);
                state.set("derivate", this._settings.derivate);
            }

            if (e.type == events.RequestDesktopInputEvent.TYPE) {
                let requestInputEvent = e as events.RequestDesktopInputEvent;
                this.registerDesktopInputHandler(requestInputEvent.listener);
            }

            if (e.type == events.RequestTouchInputEvent.TYPE) {
                let requestInputEvent = e as events.RequestTouchInputEvent;
                this.registerTouchInputHandler(requestInputEvent.listener);
            }

            if (e.type == events.RestoreStateEvent.TYPE) {
                var rste = <events.RestoreStateEvent>e;
                this._permalinkState = rste.restoredState;
            }

            if (e.type == events.AddCanvasPageLayerEvent.TYPE) {
                var acple = <events.AddCanvasPageLayerEvent>e;
                this._pageController.addCanvasPageLayer(acple.zIndex, acple.canvasPageLayer);
            }

            if (e.type == events.RedrawEvent.TYPE) {
                this._pageController.update();
            }

            if (e.type === events.TextEditEvent.TYPE) {
                let tee = <events.TextEditEvent> e;
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
                this._altoView.container.addClass("altoTop");
                this._selectionSwitchButton.disabled = true;
            } else {
                this._altoView.container.removeClass("altoTop");
                this._selectionSwitchButton.disabled = false;
            }
        }

        public isAltoSelectable() {
            return this._selectionSwitchButton != null && this._selectionSwitchButton.active;
        }

        private setAltoSelectable(selectable: boolean) {
            this._selectionSwitchButton.active = selectable;
            jQuery("[data-id='selectionSwitchButton']").blur();
            this._altoView.container.removeClass("altoTop");

            if (selectable) {
                this._altoView.container.addClass("altoSelectable");
            } else {
                this._altoView.container.removeClass("altoSelectable");
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

        private addLayout(layout: widgets.canvas.PageLayout) {
            this._layouts.push(layout);
            layout.init(this._layoutModel, this._pageController, new Size2D(this.pageWidth, this.pageHeight), this._horizontalScrollbar, this._verticalScrollbar, this._pageLoader);
            this.synchronizeLayoutToolbarButton();
        }

        private synchronizeLayoutToolbarButton() {
            var changed = false;
            this._layouts.forEach((layout) => {
                var id = layout.getLabelKey();
                var childrenWithId = this._layoutToolbarButton.children.filter((c) => c.id == id);

                if (childrenWithId.length == 0) {
                    this._layoutToolbarButton.children.push({
                        id : id,
                        label : id
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

            var newImageOrder = this._pageLayout.getCurrentPage();
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
            for (var imageIndex in this._structureImages) {
                var image = this._structureImages[ imageIndex ];
                this._hrefImageMap.set(image.href, image);
                this._orderImageMap.set(image.order, image);
                altoPresent = altoPresent || image.additionalHrefs.has("AltoHref");
            }


            if (this._orderPageMap.has(1)) {
                var firstPage = this._orderPageMap.get(1);
                this._orderPageMap.remove(1);
                var img = this._hrefImageMap.get(this._settings.filePath);
                this._orderPageMap.set(img.order, firstPage);
            }

            this.initPageLayouts();

            if (altoPresent && !this._settings.mobile) {
                // enable button
                this._enableAltoSpecificButtons();
            }
        }

        private initPageLayouts() {
            var key = this._settings.filePath;
            var position = this._pageLayout.getCurrentPositionInPage();
            var scale = this._pageController.viewport.scale;
            this._pageController.viewport.stopAnimation();
            this._desktopDelegators.forEach(delegator => delegator.clearRunning());
            this._touchDelegators.forEach(delegator => delegator.clearRunning());

            if (!this._settings.mobile) {
                this._horizontalScrollbar.clearRunning();
                this._verticalScrollbar.clearRunning();
            }

            this._pageLayout.clear();
            this._layoutModel.pageCount = this._structureImages.length;

            var order;
            if (this._hrefImageMap.has(key)) {
                order = this._hrefImageMap.get(key).order;
            } else {
                var url = ViewerParameterMap.fromCurrentUrl();
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

            this.trigger(new events.WaitForEvent(this, events.RestoreStateEvent.TYPE));
            if (this._permalinkState != null) {
                this.restorePermalink();
            }
            var image = this._orderImageMap.get(this._pageLayout.getCurrentPage());
            this.trigger(new events.ImageChangedEvent(this, image));
        }


        private _imageByHref(href: string) {
            return this._hrefImageMap.get(href);
        }

        private registerDesktopInputHandler(listener: widgets.canvas.DesktopInputListener) {
            this._desktopDelegators.push(new widgets.canvas.DesktopInputDelegator(jQuery(this._imageView.container), this._pageController.viewport, listener));
            this._desktopDelegators.push(new widgets.canvas.DesktopInputDelegator(jQuery(this._altoView.container), this._pageController.viewport, listener));
        }

        private registerTouchInputHandler(listener: widgets.canvas.TouchInputListener) {
            this._touchDelegators.push(new widgets.canvas.TouchInputDelegator(jQuery(this._imageView.container), this._pageController.viewport, listener));
            this._touchDelegators.push(new widgets.canvas.TouchInputDelegator(jQuery(this._altoView.container), this._pageController.viewport, listener));
        }


    }

    class TouchInputHandler extends widgets.canvas.TouchInputAdapter {

        private _touchAdditionalScaleMove: MoveVector = null;
        private _sessionStartRotation: number = 0;

        constructor(public component: MyCoReImageScrollComponent) {
            super();
        }

        public touchStart(session: widgets.canvas.TouchSession): void {
            this._touchAdditionalScaleMove = new MoveVector(0, 0);
            this.component.getPageController().viewport.stopAnimation();
        }

        public touchMove(session: widgets.canvas.TouchSession): void {
            var viewPort = this.component.getPageController().viewport;
            if (!session.touchLeft) {
                if (session.touches == 2 && session.startDistance > 150 && session.currentMove.distance > 150) {
                    var diff = session.currentMove.angle - session.startAngle;
                    var fullNewAngle = (360 * 2 + (this._sessionStartRotation + diff)) % 360;
                    var result = Math.round(fullNewAngle / 90) * 90;
                    result = (result == 360) ? 0 : result;
                    if (this.component.getRotation() != result) {
                        this.component.getPageLayout().rotate(result);
                        this.component.setRotation(result);
                    }
                }

                if (session.startDistance != 0 && session.currentMove.distance != 0 && session.touches > 1) {
                    var lastDistance = 0;
                    if (session.lastMove == null || session.lastMove.distance == 0) {
                        lastDistance = session.startDistance;
                    } else {
                        lastDistance = session.lastMove.distance;
                    }

                    var relativeScale = session.currentMove.distance / lastDistance;
                    var touchMiddle = viewPort.getAbsolutePosition(session.currentMove.middle);
                    var positionTouchDifference = new MoveVector(viewPort.position.x - touchMiddle.x, viewPort.position.y - touchMiddle.y);
                    var newPositionTouchDifference = positionTouchDifference.scale(relativeScale);
                    var newPositionAfterScale = touchMiddle.move(newPositionTouchDifference);
                    this._touchAdditionalScaleMove = this._touchAdditionalScaleMove.move(new MoveVector(viewPort.position.x - newPositionAfterScale.x, viewPort.position.y - newPositionAfterScale.y));
                    viewPort.scale *= relativeScale;
                }

                var move = new MoveVector(-(session.currentMove.middle.x - session.startMiddle.x), -(session.currentMove.middle.y - session.startMiddle.y));
                var rotation = viewPort.rotation;
                var scale = viewPort.scale;
                viewPort.position = session.canvasStartPosition.copy().scale(scale).move(move.rotate(rotation)).scale(1 / scale).move(this._touchAdditionalScaleMove);
            }
        }

        public touchEnd(session: widgets.canvas.TouchSession): void {
            var viewPort = this.component.getPageController().viewport;

            if (session.currentMove != null) {
                if (session.currentMove.velocity.x != 0 || session.currentMove.velocity.y != 0) {
                    var anim = new widgets.canvas.VelocityScrollAnimation(this.component.getPageController().viewport, session.currentMove.velocity);
                    this.component.getPageController().viewport.startAnimation(anim);
                }
            }

            if (session.lastSession != null) {
                if (session.startTime - session.lastSession.startTime < 200) {
                    var currentMiddle = session.startMiddle;
                    var newPosition = viewPort.getAbsolutePosition(currentMiddle);
                    viewPort.startAnimation(new widgets.canvas.ZoomAnimation(viewPort, 2, newPosition, 500));
                } else {
                    if (session.canvasStartPosition.equals(this.component.getPageController().viewport.position)) {
                    }
                }
            }
            this._sessionStartRotation = this.component.getRotation();
        }
    }

    class DesktopInputHandler extends widgets.canvas.DesktopInputAdapter {

        constructor(public component: MyCoReImageScrollComponent) {
            super();
        }

        public mouseDown(mousePosition, e): void {

            let container = this.component._componentContent;
            container.addClass("grab");
            container.removeClass("grabbable");
        }

        public mouseUp(mousePosition, e): void {
            let container = this.component._componentContent;
            container.addClass("grabbable");
            container.removeClass("grab");
        }

        public mouseDoubleClick(mousePosition: Position2D): void {
            var vp = this.component.getPageController().viewport;
            var position: Position2D = vp.getAbsolutePosition(mousePosition);
            vp.startAnimation(new widgets.canvas.ZoomAnimation(vp, 2, position));
        }

        public mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D): void {
            if (!this.component.isAltoSelectable()) {
                var xMove = currentPosition.x - startPosition.x;
                var yMove = currentPosition.y - startPosition.y;
                var move = new MoveVector(-xMove, -yMove).rotate(this.component.getPageController().viewport.rotation);
                this.component.getPageController().viewport.position = startViewport
                    .scale(this.component.getPageController().viewport.scale)
                    .move(move)
                    .scale(1 / this.component.getPageController().viewport.scale);
            }
        }

        public scroll(e: { deltaX: number; deltaY: number; orig: any; pos: Position2D; altKey?: boolean, ctrlKey?: boolean }) {
            var zoomParameter = (ViewerParameterMap.fromCurrentUrl().get("iview2.scroll") == "zoom");
            var zoom = (zoomParameter) ? !e.altKey || e.ctrlKey : e.altKey || e.ctrlKey;
            var vp = this.component.getPageController().viewport;

            if (zoom) {
                var relative = Math.pow(0.95, (e.deltaY / 10));
                if (typeof vp.currentAnimation != "undefined" && vp.currentAnimation != null) {
                    if (vp.currentAnimation instanceof widgets.canvas.ZoomAnimation) {
                        (<widgets.canvas.ZoomAnimation>vp.currentAnimation).merge(relative);
                    } else {
                        console.log("dont know howto merge animations");
                    }

                } else {
                    var position = vp.getAbsolutePosition(e.pos);
                    vp.startAnimation(new widgets.canvas.ZoomAnimation(vp, relative, position));
                }
            } else {
                vp.position = new Position2D(vp.position.x + (e.deltaX / vp.scale), vp.position.y + (e.deltaY / vp.scale));
            }

        }

        public keydown(e: JQueryKeyEventObject): void {
            switch (e.keyCode) {
                case 33:
                    this.component.previousImage();
                    break;
                case 34:
                    this.component.nextImage();
                    break;
                default :
            }
        }

    }

}
