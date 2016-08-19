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
/// <reference path="../widgets/canvas/input/TouchEventHandler.ts" />
/// <reference path="../widgets/canvas/input/TouchSession.ts" />
/// <reference path="../widgets/canvas/input/DesktopInputEventDelegator.ts" />
/// <reference path="../widgets/canvas/input/DesktopInputEventHandler.ts" />
/// <reference path="../widgets/canvas/input/MouseSession.ts" />
/// <reference path="../components/events/ViewportInitializedEvent.ts" />
/// <reference path="../components/events/MarkerInitializedEvent.ts" />


module mycore.viewer.components {

    import RequestTextContentEvent = mycore.viewer.components.events.RequestTextContentEvent;
    /**
     * canvas.overview.enabled:boolean      if true the overview will be shown in the lower right corner
     */
    export class MyCoReImageScrollComponent extends ViewerComponent implements widgets.canvas.TouchEventHandler, widgets.canvas.DesktopInputEventHandler {

        constructor(private _settings:MyCoReViewerSettings, private _container:JQuery) {
            super();
        }


        public init() {
            this.changeImage(this._settings.filePath, false);
            this.trigger(new events.ComponentInitializedEvent(this));
            this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.ShowContentEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
            this.trigger(new events.ViewportInitializedEvent(this, this._pageController.viewport));
            this.trigger(new events.MarkerInitializedEvent(this, this._pageController.getMarker()));
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
                propertyChanged : (old:ViewerProperty<Position2D>, newPosition:ViewerProperty<Position2D>) => {
                    this.update();
                }
            });


            this._pageController.viewport.scaleProperty.addObserver({
                propertyChanged : (old:ViewerProperty<number>, newScale:ViewerProperty<number>) => {
                }
            });


            this.trigger(new events.ShowContentEvent(this, componentContent, events.ShowContentEvent.DIRECTION_CENTER));
        }

        private initOverview(overviewEnabled:any|any|any|boolean) {
            if (overviewEnabled) {
                var overviewContainer = this._pageController._overview.container;
                var minVisibleSize = parseInt(Utils.getVar(this._settings, "canvas.overview.minVisibleSize", MyCoReImageScrollComponent.DEFAULT_CANVAS_OVERVIEW_MIN_VISIBLE_SIZE, (value)=> {
                    return !isNaN((<any>value) * 1) && parseInt(value, 10) > 1;
                }), 10);

                var iconChild = this._toggleButton.children(".glyphicon");
                if (this._container.width() < minVisibleSize) {
                    jQuery(overviewContainer).hide();
                    iconChild.addClass(MyCoReImageScrollComponent.OVERVIEW_VISIBLE_ICON);
                } else {
                    iconChild.addClass(MyCoReImageScrollComponent.OVERVIEW_INVISIBLE_ICON);
                }

                this._toggleButton.click(()=> {
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
            this._touchDelegators.push(new widgets.canvas.TouchInputDelegator(jQuery(this._imageView.container), this._pageController.viewport, this));
            this._mouseDelegators.push(new widgets.canvas.DesktopInputEventDelegator(jQuery(this._imageView.container), this._pageController.viewport, this));
            this._touchDelegators.push(new widgets.canvas.TouchInputDelegator(jQuery(this._altoView.container), this._pageController.viewport, this));
            this._mouseDelegators.push(new widgets.canvas.DesktopInputEventDelegator(jQuery(this._altoView.container), this._pageController.viewport, this));

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

                this._horizontalScrollbar.scrollHandler = this._verticalScrollbar.scrollHandler = ()=> {
                    this._pageLayout.scrollhandler();
                };

                this._pageController._overview = new widgets.canvas.Overview(this._pageController.viewport);
            }

            this._componentContent.append(this._imageView.container);
            this._imageView.container.addClass("mainView");
            this._imageView.container.css({left : "0px", right : "0px"});

            //this._componentContent.append(this._altoView.container);
            this._altoView.container.addClass("secondView");
            this._altoView.container.css({left : "50%", right : "0px"});
            this._altoView.container.css({
                "border-left" : "1px solid black"
            });

            this._pageController.views.push(this._imageView);
            //this._pageController.views.push(this._altoView);
            
            if(this._settings.doctype == 'pdf'){
                let textRenderer = new widgets.canvas.TextRenderer(this._pageController.viewport, this._pageController.getPageArea(), this._imageView, (page:model.AbstractPage, contentProvider:(textContent:model.TextContentModel)=>void)=>{
                    this.trigger(new RequestTextContentEvent(this,page.id,(id,model)=>{
                        contentProvider(model);
                    }))
                });
                this._pageController.textRenderer = textRenderer;
            }
        }

        private setViewMode(mode:string):void {
            var remove = (view:widgets.canvas.PageView)=> {
                let index = this._pageController.views.indexOf(view);
                if (index != -1) {
                    this._pageController.views.splice(index, 1);
                }
                view.container.detach();
            };


            var add = (view:widgets.canvas.PageView)=> {
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
                add(this._imageView)
            } else if (mode == 'mixedView') {
                this._imageView.container.css({"left" : "0px", "right" : "50%"});
                this._altoView.container.css({"left" : "50%", "right" : "0px"});
                add(this._altoView);
                add(this._imageView)
            } else if (mode == 'textView') {
                this._altoView.container.css({"left" : "0px", "right" : "0px"});
                remove(this._imageView);
                add(this._altoView);
            } else {
                console.warn("unknown view mode: " + mode);
            }

            this._pageController.update();
            this.updateToolbarLabel();

        }

        private _pageLayout:widgets.canvas.PageLayout = null;
        private _pageController:widgets.canvas.PageController = new mycore.viewer.widgets.canvas.PageController(true);
    

        private static ALTO_TEXT_HREF = "AltoHref";
        private static PDF_TEXT_HREF = "pdfText";
        private _hrefImageMap:MyCoReMap<string, model.StructureImage> = new MyCoReMap<string, model.StructureImage>();
        private _hrefPageMap = new MyCoReMap<string, model.AbstractPage>();
        private _orderImageMap = new MyCoReMap<number, model.StructureImage>();
        private _orderPageMap = new MyCoReMap<number, model.AbstractPage>();
        private _hrefPageLoadingMap = new MyCoReMap<string, boolean>();
        private _structureImages:Array<model.StructureImage> = null;
        private _currentImage:string;
        private _touchAdditionalScaleMove:MoveVector = null;
        private _horizontalScrollbar:widgets.canvas.Scrollbar;
        private _verticalScrollbar:widgets.canvas.Scrollbar;
        private _languageModel:model.LanguageModel = null;
        private _rotateButton:widgets.toolbar.ToolbarButton;
        private _layoutToolbarButton:widgets.toolbar.ToolbarDropdownButton;

        private _touchDelegators:Array<widgets.canvas.TouchInputDelegator> = new Array<widgets.canvas.TouchInputDelegator>();
        private _mouseDelegators:Array<widgets.canvas.DesktopInputEventDelegator> = new Array<widgets.canvas.DesktopInputEventDelegator>();

        private _permalinkState:MyCoReMap<string, string> = null;
        private _toggleButton:JQuery;
        private _imageView:widgets.canvas.PageView = new widgets.canvas.PageView(true, false);
        private _altoView:widgets.canvas.PageView = new widgets.canvas.PageView(false, true);
        private _componentContent:JQuery = jQuery("<div></div>");
        private _enableViewSelectButton;
        private _viewSelectButton:widgets.toolbar.ToolbarDropdownButton;
        private _viewMode:string = "imageView";


        private _layouts = new Array<widgets.canvas.PageLayout>();
        private _rotation:number = 0;
        private _sessionStartRotation:number = 0;

        private _layoutModel = {children : this._orderPageMap, pageCount : 1};

        private _pageLoader = (order:number) => {
            if (this._orderImageMap.has(order)) {
                this.loadPageIfNotPresent(this._orderImageMap.get(order).href, order);
            } else {
                this.loadPageIfNotPresent(this._settings.filePath, order);
            }
        };

        // Size of a 300DPI A4 page
        private static PAGE_WIDTH = 2480;
        private static PAGE_HEIGHT = 3508;
        private static DEFAULT_CANVAS_OVERVIEW_MIN_VISIBLE_SIZE = "800";
        private static OVERVIEW_VISIBLE_ICON = `glyphicon-triangle-top`;
        private static OVERVIEW_INVISIBLE_ICON = `glyphicon-triangle-bottom`;

        private changeImage(image:string, extern:boolean) {
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

        public get handlesEvents():string[] {
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

            return handleEvents;
        }

        private previousImage() {
            this._pageLayout.previous();
            this.update();
        }

        private nextImage() {
            this._pageLayout.next();
            this.update();
        }

        private changePageLayout(pageLayout:widgets.canvas.PageLayout) {
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

        private loadPageIfNotPresent(imageHref:string, order:number) {
            if (!this._hrefPageMap.has(imageHref) &&
                (!this._hrefPageLoadingMap.has(imageHref) || !this._hrefPageLoadingMap.get(imageHref))) {
                this._hrefPageLoadingMap.set(imageHref, true);

                var textHref:string = null;
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

            if(state.has("layout")){
                var layout = state.get("layout");
                var layoutObjects = this._layouts.filter(l=>l.getLabelKey() == layout);
                if(layoutObjects.length!=1){
                    console.log("no matching layout found!");
                } else {
                    this.changePageLayout(layoutObjects[0])

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


        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent>e;
                this._rotateButton = ptme.model._rotateButton;
                this._layoutToolbarButton = ptme.model._layoutDropdownButton;
                if (!this._settings.mobile) {
                    if ("addViewSelectButton" in ptme.model) {
                        this._enableViewSelectButton = ()=> {
                            (<any>ptme.model).addViewSelectButton();
                            this._viewSelectButton = (<any>ptme.model).viewSelect;
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
                    this._layouts.filter((e)=>e.getLabelKey() == dbpe.childId).forEach((layout)=> {
                        this.changePageLayout(layout);
                        this.updateToolbarLabel();
                    });
                } else if (dbpe.button.id == 'viewSelect') {
                    this.setViewMode(dbpe.childId);
                    this.updateToolbarLabel();
                }
            }


            if (e.type == mycore.viewer.components.events.ImageSelectedEvent.TYPE) {
                var imageSelectedEvent = <mycore.viewer.components.events.ImageSelectedEvent>e;
                this.changeImage(imageSelectedEvent.image.href, true);

            }

            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var structureModelLodedEvent = <events.StructureModelLoadedEvent>e;
                this._structureImages = structureModelLodedEvent.structureModel.imageList;
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

            if (e.type == events.RestoreStateEvent.TYPE) {
                var rste = <events.RestoreStateEvent>e;
                this._permalinkState = rste.restoredState;
            }

        }

        private addLayout(layout:widgets.canvas.PageLayout) {
            this._layouts.push(layout);
            layout.init(this._layoutModel, this._pageController, new Size2D(MyCoReImageScrollComponent.PAGE_WIDTH, MyCoReImageScrollComponent.PAGE_HEIGHT), this._horizontalScrollbar, this._verticalScrollbar, this._pageLoader);
            this.synchronizeLayoutToolbarButton();
        }

        private synchronizeLayoutToolbarButton() {
            var changed = false;
            this._layouts.forEach((layout)=> {
                var id = layout.getLabelKey();
                var childrenWithId = this._layoutToolbarButton.children.filter((c) =>  c.id == id);

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
                    this._layoutToolbarButton.children.forEach((e)=> {
                        e.label = this._languageModel.getTranslation("layout." + e.id);
                    });

                    this._layoutToolbarButton.children = this._layoutToolbarButton.children;
                    if (this._pageLayout != null) {
                        this._layoutToolbarButton.label = this._languageModel.getTranslation("layout." + this._pageLayout.getLabelKey());
                    }
                }

                if (this._viewSelectButton != null) {
                    this._viewSelectButton.label = this._languageModel.getTranslation("view." + this._viewMode);

                    this._viewSelectButton.children.forEach((child)=> {
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
                this._enableViewSelectButton();
            }
        }

        private initPageLayouts() {
            var key = this._settings.filePath;
            var position = this._pageLayout.getCurrentPositionInPage();
            var scale = this._pageController.viewport.scale;
            this._pageController.viewport.stopAnimation();
            this._mouseDelegators.forEach(delegator=>delegator.clearRunning());
            this._touchDelegators.forEach(delegator=>delegator.clearRunning());

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


        private _imageByHref(href:string) {
            return this._hrefImageMap.get(href);
        }

        public touchStart(session:widgets.canvas.TouchSession):void {
            this._touchAdditionalScaleMove = new MoveVector(0, 0);
            this._pageController.viewport.stopAnimation();
        }

        public touchMove(session:widgets.canvas.TouchSession):void {
            var viewPort = this._pageController.viewport;
            if (!session.touchLeft) {
                if (session.touches == 2 && session.startDistance > 150 && session.currentMove.distance > 150) {
                    var diff = session.currentMove.angle - session.startAngle;
                    var fullNewAngle = (360 * 2 + (this._sessionStartRotation + diff)) % 360;
                    var result = Math.round(fullNewAngle / 90) * 90;
                    result = (result == 360) ? 0 : result;
                    if (this._rotation != result) {
                        this._pageLayout.rotate(result);
                        this._rotation = result;
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

        public touchEnd(session:widgets.canvas.TouchSession):void {
            var viewPort = this._pageController.viewport;

            if (session.currentMove != null) {
                if (session.currentMove.velocity.x != 0 || session.currentMove.velocity.y != 0) {
                    var anim = new widgets.canvas.VelocityScrollAnimation(this._pageController.viewport, session.currentMove.velocity);
                    this._pageController.viewport.startAnimation(anim);
                }
            }

            if (session.lastSession != null) {
                if (session.startTime - session.lastSession.startTime < 200) {
                    var currentMiddle = session.startMiddle;
                    var newPosition = viewPort.getAbsolutePosition(currentMiddle);
                    viewPort.startAnimation(new widgets.canvas.ZoomAnimation(viewPort, 2, newPosition, 500));
                } else {
                    if (session.canvasStartPosition.equals(this._pageController.viewport.position)) {
                    }
                }
            }
            this._sessionStartRotation = this._rotation;
        }

        public mouseDown(session:widgets.canvas.MouseSession):void {
        }

        public mouseMove(session:widgets.canvas.MouseSession):void {
            var xMove = session.currentPositionInputElement.x - session.startPositionInputElement.x;
            var yMove = session.currentPositionInputElement.y - session.startPositionInputElement.y;
            var move = new MoveVector(-xMove, -yMove).rotate(this._pageController.viewport.rotation);

            this._pageController.viewport.position = session.startPositionViewport
                .scale(this._pageController.viewport.scale)
                .move(move)
                .scale(1 / this._pageController.viewport.scale);

        }

        public mouseUp(session:widgets.canvas.MouseSession):void {
            if (typeof session.lastMouseSession != "undefined" && session.lastMouseSession != null && session.downDate - session.lastMouseSession.downDate < 500) {
                if (Math.abs(session.lastMouseSession.startPositionInputElement.x - session.startPositionInputElement.x) < 10 &&
                    Math.abs(session.lastMouseSession.startPositionInputElement.y - session.startPositionInputElement.y) < 10) {
                    var vp = this._pageController.viewport;
                    var position = vp.getAbsolutePosition(session.currentPositionInputElement);
                    vp.startAnimation(new widgets.canvas.ZoomAnimation(vp, 2, position));
                }

            }
        }

        public scroll(e:{ deltaX: number; deltaY: number; orig: any; pos: Position2D; altKey?: boolean }) {
            var zoomParameter = (ViewerParameterMap.fromCurrentUrl().get("iview2.scroll") == "zoom");
            var zoom = (zoomParameter) ? !e.altKey : e.altKey;
            var vp = this._pageController.viewport;

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

        public keydown(e:JQueryKeyEventObject):void {
            switch (e.keyCode) {
                case 33:
                    this.previousImage();
                    break;
                case 34:
                    this.nextImage();
                    break;
                default :
            }
        }

        public keypress(e:JQueryKeyEventObject):void {

        }

        public keyup(e:JQueryKeyEventObject):void {
        }

    }

}