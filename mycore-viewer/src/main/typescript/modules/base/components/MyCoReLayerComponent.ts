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


import {ViewerComponent} from "./ViewerComponent";
import {MyCoReViewerSettings} from "../MyCoReViewerSettings";
import {MyCoReMap, Utils, ViewerError, ViewerUserSettingStore} from "../Utils";
import {StructureModel} from "./model/StructureModel";
import {LanguageModel} from "./model/LanguageModel";
import {MyCoReBasicToolbarModel} from "./model/MyCoReBasicToolbarModel";
import {ToolbarDropdownButton} from "../widgets/toolbar/model/ToolbarDropdownButton";
import {Layer} from "./model/Layer";
import {LayerDisplayController} from "../widgets/layer/LayerDisplayController";
import {ImageChangedEvent} from "./events/ImageChangedEvent";
import {LanguageModelLoadedEvent} from "./events/LanguageModelLoadedEvent";
import {StructureModelLoadedEvent} from "./events/StructureModelLoadedEvent";
import {WaitForEvent} from "./events/WaitForEvent";
import {ShowContentEvent} from "./events/ShowContentEvent";
import {DropdownButtonPressedEvent} from "../widgets/toolbar/events/DropdownButtonPressedEvent";
import {ProvideToolbarModelEvent} from "./events/ProvideToolbarModelEvent";
import {ProvideLayerEvent} from "./events/ProvideLayerEvent";
import {ViewerEvent} from "../widgets/events/ViewerEvent";

export class MyCoReLayerComponent extends ViewerComponent {
    constructor(private _settings: MyCoReViewerSettings) {
        super();
        this.enabled = Utils.getVar(this._settings, "text.enabled", "true") === "true" && this._settings.mobile != true;
        this.showLayerOnStart = Utils.getVar(this._settings, "text.showOnStart", []);
    }

    private showLayerOnStart: Array<string>;
    private enabled: boolean;

    private toolbarButtonSync = Utils.synchronize<MyCoReLayerComponent>([me => me.toolbarButtonDisplayable(),
            me => me.dropDownButton == null]
        , me => me.initToolbarButton());
    private layerSync = Utils.synchronize<MyCoReLayerComponent>([me => me.toolbarButtonInitialized()], me => me.synchronizeLayers());

    private structureModel: StructureModel = null;
    private languageModel: LanguageModel = null;
    private toolbarModel: MyCoReBasicToolbarModel = null;

    private dropDownButton: ToolbarDropdownButton = null;

    private layerList: Array<Layer>;
    private layerIdLayerMap: MyCoReMap<string, Layer>;
    private layerDisplay: LayerDisplayController;
    private static SIDEBAR_LAYER_SIZE = "SIDEBAR_LAYER_SIZE";


    private currentHref: string;
    private container: JQuery;

    private static LAYER_DROPDOWN_ID = "toolbar.LayerButton";
    private sidebarLabel: JQuery = jQuery("<span>Ebenen</span>");

    public init() {
        if (this.enabled) {
            this.container = jQuery("<div></div>");
            this.container.css({overflowY: "scroll", display: "block"});
            this.container.addClass("tei");
            this.container.addClass("layer-component");
            this.container.bind("iviewResize", () => {
                this.updateContainerSize();
            });

            this.layerDisplay = new LayerDisplayController(this.container, (id: string) => {
                return this.languageModel.getTranslation(id);
            });
            this.layerList = new Array<Layer>();
            this.layerIdLayerMap = new MyCoReMap<string, Layer>();
            this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));
            this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));
            this.trigger(new WaitForEvent(this, ImageChangedEvent.TYPE));
        }
    }


    private updateContainerSize() {
        this.container.css({"height": this.container.parent().height() - this.sidebarLabel.parent().outerHeight() + "px"});
        var containerSize = this.container.width();
        var settingStore = ViewerUserSettingStore.getInstance();
        if (containerSize > 50) {
            settingStore.setValue(MyCoReLayerComponent.SIDEBAR_LAYER_SIZE, containerSize.toString());
        }
    }

    public get handlesEvents(): string[] {
        var handles = new Array<string>();

        if (this.enabled) {
            handles.push(StructureModelLoadedEvent.TYPE);
            handles.push(ImageChangedEvent.TYPE);
            handles.push(ShowContentEvent.TYPE);
            handles.push(DropdownButtonPressedEvent.TYPE);
            handles.push(LanguageModelLoadedEvent.TYPE);
            handles.push(ProvideToolbarModelEvent.TYPE);
            handles.push(ProvideLayerEvent.TYPE);
        }

        return handles;
    }

    public handle(e: ViewerEvent): void {
        if (e.type == DropdownButtonPressedEvent.TYPE) {
            var dropdownButtonPressedEvent = e as DropdownButtonPressedEvent;
            if (dropdownButtonPressedEvent.button.id == MyCoReLayerComponent.LAYER_DROPDOWN_ID) {
                if (this.layerIdLayerMap.has(dropdownButtonPressedEvent.childId)) {
                    var transcriptionType = dropdownButtonPressedEvent.childId;
                    this.toggleTranscriptionContainer(transcriptionType);
                } else {
                    throw new ViewerError("Invalid button child pressed!");
                }
            }
        }

        if (e.type == ProvideLayerEvent.TYPE) {
            const ple = e as ProvideLayerEvent;
            this.layerList.push(ple.layer);
            const layerType = ple.layer.getId();
            this.layerIdLayerMap.set(layerType, ple.layer);
            this.toolbarButtonSync(this);
            this.layerSync(this);

            if (this.showLayerOnStart.length > 0) {
                let priority: number;
                if ((priority = this.showLayerOnStart.indexOf(layerType)) != -1) {
                    if (this.layerDisplay.getLayer().length != 0) {
                        let activePriority: number;
                        if ((activePriority = this.showLayerOnStart.indexOf(this.layerDisplay.getLayer()[0].getId())) != -1) {
                            if (activePriority < priority) {
                                return;
                            }
                        }
                    }
                    this.toggleTranscriptionContainer(layerType, true);
                }
            }
        }

        if (e.type == ShowContentEvent.TYPE) {
            const sce = e as ShowContentEvent;
            if (sce.size == 0 && sce.containerDirection == ShowContentEvent.DIRECTION_EAST) {
                if (this.dropDownButton != null) {
                    this.dropDownButton.children.forEach(child => {
                        delete child.icon;
                    });
                    this.dropDownButton.children = this.dropDownButton.children;
                    this.layerDisplay.getLayer().forEach(s => {
                        this.layerDisplay.removeLayer(s);
                    });
                }
            }
        }

        if (e.type == StructureModelLoadedEvent.TYPE) {
            const structureModelLoadedEvent = e as StructureModelLoadedEvent;
            this.structureModel = structureModelLoadedEvent.structureModel;
            this.toolbarButtonSync(this);
        }


        if (e.type == ImageChangedEvent.TYPE) {
            const imageChangedEvent = e as ImageChangedEvent;
            if (typeof this.structureModel !== "undefined" && typeof imageChangedEvent.image != "undefined" && imageChangedEvent != null) {
                this.currentHref = imageChangedEvent.image.href;
                this.layerDisplay.pageChanged(this.currentHref);
            }
        }

        if (e.type == LanguageModelLoadedEvent.TYPE) {
            this.languageModel = (e as LanguageModelLoadedEvent).languageModel;
            this.toolbarButtonSync(this);
        }

        if (e.type == ProvideToolbarModelEvent.TYPE) {
            const ptme = e as ProvideToolbarModelEvent;
            this.toolbarModel = ptme.model;
            this.toolbarButtonSync(this);
        }
    }

    private toggleTranscriptionContainer(transcriptionType: string, clear = false) {
        const layer = this.layerIdLayerMap.get(transcriptionType);
        if (this.layerDisplay.getLayer().indexOf(layer) == -1) {
            this.layerDisplay.addLayer(layer);
        } else {
            this.layerDisplay.removeLayer(layer);
        }

        if (clear) {
            this.layerDisplay.getLayer().forEach((activeLayer) => {
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
        const settingStore = ViewerUserSettingStore.getInstance();
        const hasValue = settingStore.hasValue(MyCoReLayerComponent.SIDEBAR_LAYER_SIZE);
        const layerSize = hasValue ? parseInt(settingStore.getValue(MyCoReLayerComponent.SIDEBAR_LAYER_SIZE), 10) : null;

        const direction = (this._settings.mobile) ? ShowContentEvent.DIRECTION_CENTER : ShowContentEvent.DIRECTION_EAST;
        this.trigger(new ShowContentEvent(this, this.container, direction, hasValue ? layerSize : 400, this.sidebarLabel));
        this.updateContainerSize();
    }

    private hideContainer() {
        const direction = (this._settings.mobile) ? ShowContentEvent.DIRECTION_CENTER : ShowContentEvent.DIRECTION_EAST;
        this.trigger(new ShowContentEvent(this, null, direction, 0, null));
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
    public toolbarButtonDisplayable(): boolean {
        return this.languageModel != null && this.toolbarModel != null && this.layerList.length > 0;
    }

    /**
     * Initialize the toolbar button and show it.
     */
    public initToolbarButton() {
        this.dropDownButton = new ToolbarDropdownButton(MyCoReLayerComponent.LAYER_DROPDOWN_ID,
            this.languageModel.getTranslation("toolbar.layerButton"), []);

        this.toolbarModel._actionControllGroup.addComponent(this.dropDownButton);
        this.layerSync(this);
    }

    public synchronizeLayers() {
        // find all layers which are present but not inserted in the drop down button.
        const onlyIfNotInserted = layerInList => this.dropDownButton.children.filter(layerInButton => {
            return layerInButton.id == layerInList.getId();
        }).length == 0;

        const newLayers = this.layerList.filter(onlyIfNotInserted);

        // insert all the found layers.
        newLayers.forEach(newLayer => {
            const childLabelTranslation = this.languageModel.getTranslation(newLayer.getLabel());
            const dropDownChild = {id: newLayer.getId(), label: childLabelTranslation};
            this.dropDownButton.children.push(dropDownChild);
        });

        this.dropDownButton.children.forEach((buttonChildren) => {

            const hasLayer = this.layerIdLayerMap.has(buttonChildren.id);
            if (hasLayer) {
                const isInserted = this.layerDisplay.getLayer().indexOf(this.layerIdLayerMap.get(buttonChildren.id)) != -1;
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
