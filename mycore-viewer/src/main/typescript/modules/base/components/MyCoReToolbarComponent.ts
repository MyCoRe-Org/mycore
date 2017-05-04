/// <reference path="../definitions/jquery.d.ts" />
/// <reference path="../Utils.ts" />
/// <reference path="events/StructureModelLoadedEvent.ts" />
/// <reference path="model/StructureImage.ts" />
/// <reference path="model/MyCoReBasicToolbarModel.ts" />
/// <reference path="../widgets/events/ViewerEventManager.ts" />
/// <reference path="../widgets/events/ViewerEvent.ts" />
/// <reference path="../widgets/toolbar/IviewToolbar.ts" />
/// <reference path="../widgets/toolbar/model/ToolbarModel.ts" />
/// <reference path="../widgets/toolbar/model/ToolbarDropdownButton.ts" />
/// <reference path="../widgets/toolbar/events/DropdownButtonPressedEvent.ts" />
/// <reference path="../MyCoReViewerSettings.ts" />
/// <reference path="ViewerComponent.ts" />
/// <reference path="events/ComponentInitializedEvent.ts" />
/// <reference path="events/ImageSelectedEvent.ts" />
/// <reference path="events/ImageChangedEvent.ts" />
/// <reference path="events/CanvasTapedEvent.ts" />
/// <reference path="events/WaitForEvent.ts" />
/// <reference path="events/LanguageModelLoadedEvent.ts" />
/// <reference path="events/ProvideToolbarModelEvent.ts" />

namespace mycore.viewer.components {
    export class MyCoReToolbarComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings, private _container:JQuery) {
            super();
            this._toolbarModel = null;
        }

        private _sync = Utils.synchronize<MyCoReToolbarComponent>([(me:MyCoReToolbarComponent)=>me._toolbarModel != null && me._imageIdMap != null], (me:MyCoReToolbarComponent)=> {
           me.trigger(new events.WaitForEvent(this, events.ImageChangedEvent.TYPE));
        });

        public init() {
            this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {

            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                this._toolbarModel = (<events.ProvideToolbarModelEvent>e).model;
                this._toolbarController = new mycore.viewer.widgets.toolbar.IviewToolbar(this._container, this._settings.mobile, this._toolbarModel);

                var that = this;
                this._toolbarController.eventManager.bind(function (e:mycore.viewer.widgets.events.ViewerEvent) {
                    that.trigger(e);
                });

                this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
                this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
            }

            if (this._toolbarModel != null) {
                if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                    var languageModelLoadedEvent = <events.LanguageModelLoadedEvent>e;
                    this._toolbarModel.i18n(languageModelLoadedEvent.languageModel);
                    this._sync(this);
                }

                if (e.type == events.StructureModelLoadedEvent.TYPE) {
                    if (!this._settings.mobile) {
                        var smlEvent = <events.StructureModelLoadedEvent> e;
                        var imgList = smlEvent.structureModel._imageList;
                        var pageSelect:widgets.toolbar.ToolbarDropdownButton = <widgets.toolbar.ToolbarDropdownButton> this._toolbarModel.getGroup("ImageChangeControllGroup").getComponentById("PageSelect");

                        this._imageIdMap = new MyCoReMap<string, model.StructureImage>();
                        var childs = new Array<widgets.toolbar.ToolbarDropdownButtonChild>();
                        for (var imgIndex in imgList) {
                            var imgElement = imgList[imgIndex];
                            this._imageIdMap.set(imgElement.id, imgElement);
                            var toolbarDropDownElement = {
                                id : imgElement.id,
                                label : "[" + imgElement.order + "]" + (imgElement.orderLabel!=null ? " - " + imgElement.orderLabel : "")
                            };
                            childs.push(toolbarDropDownElement);
                        }

                        pageSelect.children = childs;
                    }

                    this._sync(this);
                    return;
                }

                if (e.type == mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
                    var dropdownButtonPressedEvent = <mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent> e;

                    if (dropdownButtonPressedEvent.button.id == "PageSelect") {
                        var id = dropdownButtonPressedEvent.childId;
                        var img = this._imageIdMap.get(id);
                        this.trigger(new events.ImageSelectedEvent(this, img));
                    }

                    return;
                }

                if (e.type == events.ImageChangedEvent.TYPE) {
                    var icEvent:events.ImageChangedEvent = <events.ImageChangedEvent>e;
                    if (icEvent.image != null) {
                        if (!this._settings.mobile) {
                            var select = this._toolbarController.getView("PageSelect").getElement();
                            //select.find("option[selected]").removeAttr("selected");
                            select.val(icEvent.image.id);
                        }
                    }
                    return;
                }

                if (e.type == events.CanvasTapedEvent.TYPE) {
                    this._toolbarController.getView(null).getElement().slideToggle();
                    return;
                }
            }
        }

        public  get handlesEvents():string[] {
            var handleEvents:Array<string> = new Array<string>();
            handleEvents.push(events.StructureModelLoadedEvent.TYPE);
            handleEvents.push(events.ProvideToolbarModelEvent.TYPE);
            handleEvents.push(mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE);
            handleEvents.push(events.ImageChangedEvent.TYPE);
            handleEvents.push(events.CanvasTapedEvent.TYPE);
            handleEvents.push(events.LanguageModelLoadedEvent.TYPE);
            return handleEvents;
        }

        public get toolbar() {
            return this._toolbarController;
        }

        private _toolbarModel:model.MyCoReBasicToolbarModel;
        private _toolbarController:widgets.toolbar.IviewToolbar;
        private _imageIdMap:MyCoReMap<string, model.StructureImage> = null;

    }

}