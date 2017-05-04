/// <reference path="../definitions/jquery.d.ts" />
/// <reference path="../Utils.ts" />
/// <reference path="../MyCoReViewerSettings.ts" />
/// <reference path="../widgets/events/ViewerEvent.ts" />
/// <reference path="../widgets/toolbar/events/DropdownButtonPressedEvent.ts" />
/// <reference path="ViewerComponent.ts" />
/// <reference path="model/StructureImage.ts" />
/// <reference path="model/Layer.ts" />
/// <reference path="events/StructureModelLoadedEvent.ts" />
/// <reference path="events/ComponentInitializedEvent.ts" />
/// <reference path="events/WaitForEvent.ts" />
/// <reference path="events/ImageChangedEvent.ts" />
/// <reference path="events/ShowContentEvent.ts" />
/// <reference path="events/ProvideLayerEvent.ts" />
/// <reference path="../widgets/layer/LayerDisplayController.ts" />

namespace mycore.viewer.components {

    export class MyCoReLayerComponent extends ViewerComponent {
        constructor(private _settings:MyCoReViewerSettings) {
            super();
            this.enabled = Utils.getVar(this._settings, "text.enabled", false) && this._settings.mobile != true;
            this.showLayerOnStart = Utils.getVar(this._settings, "text.showOnStart", [ ]);
        }

        private showLayerOnStart:Array<string>;
        private enabled:boolean;

        private toolbarButtonSync = Utils.synchronize<MyCoReLayerComponent>([ me=>me.toolbarButtonDisplayable(),
                    me=>me.dropDownButton == null ]
            , me=>me.initToolbarButton());
        private layerSync = Utils.synchronize<MyCoReLayerComponent>([ me=>me.toolbarButtonInitialized() ], me=>me.synchronizeLayers());

        private structureModel:model.StructureModel = null;
        private languageModel:model.LanguageModel = null;
        private toolbarModel:model.MyCoReBasicToolbarModel = null;

        private dropDownButton:widgets.toolbar.ToolbarDropdownButton = null;

        private layerList:Array<model.Layer>;
        private layerIdLayerMap:MyCoReMap<string, model.Layer>;
        private layerDisplay:widgets.layer.LayerDisplayController;
        private static SIDEBAR_LAYER_SIZE = "SIDEBAR_LAYER_SIZE";


        private currentHref:string;
        private container:JQuery;

        private static LAYER_DROPDOWN_ID = "toolbar.LayerButton";
        private sidebarLabel:JQuery = jQuery("<span>Ebenen</span>");

        public init() {
            if (this.enabled) {
                this.container = jQuery("<div></div>");
                this.container.css({overflowY : "scroll", display: "block"});
                this.container.addClass("tei");
                this.container.addClass("layer-component");
                this.container.bind("iviewResize", ()=> {
                    this.updateContainerSize();
                });

                this.layerDisplay = new widgets.layer.LayerDisplayController(this.container, (id:string)=> {
                    return this.languageModel.getTranslation(id);
                });
                this.layerList = new Array<model.Layer>();
                this.layerIdLayerMap = new MyCoReMap<string, model.Layer>();
                this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
                this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
                this.trigger(new events.WaitForEvent(this, events.ImageChangedEvent.TYPE));
            }
        }


        private updateContainerSize() {
            this.container.css({"height" : this.container.parent().height() - this.sidebarLabel.parent().outerHeight() + "px"});
            var containerSize = this.container.width();
            var settingStore = ViewerUserSettingStore.getInstance();
            if (containerSize > 50) {
                settingStore.setValue(MyCoReLayerComponent.SIDEBAR_LAYER_SIZE, containerSize.toString());
            }
        }

        public get handlesEvents():string[] {
            var handles = new Array<string>();

            if (this.enabled) {
                handles.push(events.StructureModelLoadedEvent.TYPE);
                handles.push(events.ImageChangedEvent.TYPE);
                handles.push(events.ShowContentEvent.TYPE);
                handles.push(mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE);
                handles.push(events.LanguageModelLoadedEvent.TYPE);
                handles.push(events.ProvideToolbarModelEvent.TYPE);
                handles.push(events.ProvideLayerEvent.TYPE);
            }

            return handles;
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
                var dropdownButtonPressedEvent = <mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent> e;
                if (dropdownButtonPressedEvent.button.id == MyCoReLayerComponent.LAYER_DROPDOWN_ID) {
                    if (this.layerIdLayerMap.has(dropdownButtonPressedEvent.childId)) {
                        var transcriptionType = dropdownButtonPressedEvent.childId;
                        this.toggleTranscriptionContainer(transcriptionType);
                    } else {
                        throw new ViewerError("Invalid button child pressed!");
                    }
                }
            }

            if (e.type == events.ProvideLayerEvent.TYPE) {
                var ple = <events.ProvideLayerEvent>e;
                this.layerList.push(ple.layer);
                var layerType = ple.layer.getId();
                this.layerIdLayerMap.set(layerType, ple.layer);
                this.toolbarButtonSync(this);
                this.layerSync(this);

                if (this.showLayerOnStart.length > 0) {
                    let priority:number;
                    if ((priority = this.showLayerOnStart.indexOf(layerType)) != -1) {
                        if (this.layerDisplay.getLayer().length != 0) {
                            let activePriority:number;
                            if ((activePriority = this.showLayerOnStart.indexOf(this.layerDisplay.getLayer()[ 0 ].getId())) != -1) {
                                if (activePriority < priority) {
                                    return;
                                }
                            }
                        }
                        this.toggleTranscriptionContainer(layerType, true);
                    }
                }
            }

            if(e.type == events.ShowContentEvent.TYPE) {
                var sce = <events.ShowContentEvent>e;
                if(sce.size == 0 && sce.containerDirection == events.ShowContentEvent.DIRECTION_EAST) {
                    if(this.dropDownButton!=null){
                        this.dropDownButton.children.forEach(child=> {
                            delete child.icon;
                        });
                        this.dropDownButton.children = this.dropDownButton.children;
                        this.layerDisplay.getLayer().forEach(s=> {
                            this.layerDisplay.removeLayer(s);
                        });
                    }
                }
            }

            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var structureModelLoadedEvent = <events.StructureModelLoadedEvent>e;
                this.structureModel = structureModelLoadedEvent.structureModel;
                this.toolbarButtonSync(this);
            }


            if (e.type == events.ImageChangedEvent.TYPE) {
                let imageChangedEvent = (<events.ImageChangedEvent> e);
                if (typeof this.structureModel !== "undefined" && typeof imageChangedEvent.image != "undefined" && imageChangedEvent != null) {
                    this.currentHref = imageChangedEvent.image.href;
                    this.layerDisplay.pageChanged(this.currentHref);
                }
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                this.languageModel = (<events.LanguageModelLoadedEvent>e).languageModel;
                this.toolbarButtonSync(this);
            }

            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent>e;
                this.toolbarModel = ptme.model;
                this.toolbarButtonSync(this);
            }
        }

        private toggleTranscriptionContainer(transcriptionType:string, clear = false) {
            var layer = this.layerIdLayerMap.get(transcriptionType);
            if (this.layerDisplay.getLayer().indexOf(layer) == -1) {
                this.layerDisplay.addLayer(layer);
            } else {
                this.layerDisplay.removeLayer(layer);
            }

            if (clear) {
                this.layerDisplay.getLayer().forEach((activeLayer)=> {
                    if (layer != activeLayer) {
                        this.layerDisplay.removeLayer(activeLayer);
                    }
                });
            }

            if (this.layerDisplay.getLayer().length > 0) {
                this.showContainer();
            } else {
                this.hideContainer();
            }
            this.synchronizeLayers();
        }

        private showContainer() {
            var settingStore = ViewerUserSettingStore.getInstance();
            var hasValue = settingStore.hasValue(MyCoReLayerComponent.SIDEBAR_LAYER_SIZE);
            var layerSize = hasValue ? parseInt(settingStore.getValue(MyCoReLayerComponent.SIDEBAR_LAYER_SIZE), 10) : null;

            var direction = (this._settings.mobile) ? events.ShowContentEvent.DIRECTION_CENTER : events.ShowContentEvent.DIRECTION_EAST;
            this.trigger(new events.ShowContentEvent(this, this.container, direction, hasValue ? layerSize : 400, this.sidebarLabel));
            this.updateContainerSize();
        }

        private hideContainer() {
            var direction = (this._settings.mobile) ? events.ShowContentEvent.DIRECTION_CENTER : events.ShowContentEvent.DIRECTION_EAST;
            this.trigger(new events.ShowContentEvent(this, null, direction, 0, null));
        }

        /**
         * Checks if the drop down button is initialized
         * @return {boolean}
         */
        public toolbarButtonInitialized() {
            return this.dropDownButton != null;
        }

        /**
         * Checks if everything is loaded to show the toolbar button.
         * @return {boolean}
         */
        public toolbarButtonDisplayable():boolean {
            return this.languageModel != null && this.toolbarModel != null && this.layerList.length > 0;
        }

        /**
         * Initialize the toolbar button and show it.
         */
        public initToolbarButton() {
            this.dropDownButton = new widgets.toolbar.ToolbarDropdownButton(MyCoReLayerComponent.LAYER_DROPDOWN_ID,
                this.languageModel.getTranslation("toolbar.layerButton"), []);

            this.toolbarModel._actionControllGroup.addComponent(this.dropDownButton);
            this.layerSync(this);
        }

        public synchronizeLayers() {
            // find all layers which are present but not inserted in the drop down button.
            var onlyIfNotInserted = layerInList=>this.dropDownButton.children.filter(layerInButton=> {
                return layerInButton.id == layerInList.getId();
            }).length == 0;

            var newLayers = this.layerList.filter(onlyIfNotInserted);

            // insert all the found layers.
            newLayers.forEach(newLayer=> {
                var childLabelTranslation = this.languageModel.getTranslation(newLayer.getLabel());
                var dropDownChild = {id : newLayer.getId(), label : childLabelTranslation};
                this.dropDownButton.children.push(dropDownChild);
            });

            this.dropDownButton.children.forEach((buttonChildren)=> {

                var hasLayer = this.layerIdLayerMap.has(buttonChildren.id);
                if (hasLayer) {
                    var isInserted = this.layerDisplay.getLayer().indexOf(this.layerIdLayerMap.get(buttonChildren.id)) != -1;
                    if (isInserted) {
                        buttonChildren.icon = "ok";
                    } else {
                        if ("icon" in buttonChildren) {
                            delete buttonChildren.icon;
                        }
                    }
                }
            });

            this.dropDownButton.children = this.dropDownButton.children;
        }

    }
}
