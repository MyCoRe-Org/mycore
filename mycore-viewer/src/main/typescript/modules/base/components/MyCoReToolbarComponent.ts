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
import {MyCoReMap, Utils} from "../Utils";
import {WaitForEvent} from "./events/WaitForEvent";
import {ImageChangedEvent} from "./events/ImageChangedEvent";
import {StructureImage} from "./model/StructureImage";
import {IviewToolbar} from "../widgets/toolbar/IviewToolbar";
import {MyCoReBasicToolbarModel} from "./model/MyCoReBasicToolbarModel";
import {LanguageModelLoadedEvent} from "./events/LanguageModelLoadedEvent";
import {CanvasTapedEvent} from "./events/CanvasTapedEvent";
import {ProvideToolbarModelEvent} from "./events/ProvideToolbarModelEvent";
import {DropdownButtonPressedEvent} from "../widgets/toolbar/events/DropdownButtonPressedEvent";
import {StructureModelLoadedEvent} from "./events/StructureModelLoadedEvent";
import {ImageSelectedEvent} from "./events/ImageSelectedEvent";
import {ToolbarDropdownButton, ToolbarDropdownButtonChild} from "../widgets/toolbar/model/ToolbarDropdownButton";
import {ViewerEvent} from "../widgets/events/ViewerEvent";

export class MyCoReToolbarComponent extends ViewerComponent {

    constructor(private _settings: MyCoReViewerSettings, private _container: JQuery) {
        super();
        this._toolbarModel = null;
    }

    private _sync = Utils.synchronize<MyCoReToolbarComponent>([
            (me: MyCoReToolbarComponent) => me._toolbarModel != null && me._imageIdMap != null
        ],
        (me: MyCoReToolbarComponent) => {
            me.trigger(new WaitForEvent(this, ImageChangedEvent.TYPE));
        });

    public init() {
        this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));
    }

    public handle(e: ViewerEvent): void {

        if (e.type == ProvideToolbarModelEvent.TYPE) {
            this._toolbarModel = (e as ProvideToolbarModelEvent).model;
            this._toolbarController = new IviewToolbar(this._container, this._settings.mobile, this._toolbarModel);

            this._toolbarController.eventManager.bind((e: ViewerEvent) => {
                this.trigger(e);
            });

            this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));
            this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));
        }

        if (this._toolbarModel != null) {
            if (e.type == LanguageModelLoadedEvent.TYPE) {
                const languageModelLoadedEvent = e as LanguageModelLoadedEvent;
                this._toolbarModel.i18n(languageModelLoadedEvent.languageModel);
                this._sync(this);
            }

            if (e.type == StructureModelLoadedEvent.TYPE) {
                if (!this._settings.mobile) {
                    const smlEvent = e as StructureModelLoadedEvent;
                    const imgList = smlEvent.structureModel._imageList;
                    const group = this._toolbarModel.getGroup('ImageChangeControllGroup');
                    if (group != null || typeof group !== 'undefined') {
                        const pageSelect: ToolbarDropdownButton =
                            group.getComponentById('PageSelect') as ToolbarDropdownButton;
                        this._imageIdMap = new MyCoReMap<string, StructureImage>();
                        const childs = new Array<ToolbarDropdownButtonChild>();
                        for (const imgElement of imgList) {
                            this._imageIdMap.set(imgElement.id, imgElement);
                            const toolbarDropDownElement = {
                                id: imgElement.id,
                                label: `[${imgElement.order}]${imgElement.orderLabel != null ? ' - ' + imgElement.orderLabel : ''}`
                            };
                            childs.push(toolbarDropDownElement);
                        }

                        pageSelect.children = childs;
                    }

                }

                this._sync(this);
                return;
            }

            if (e.type == DropdownButtonPressedEvent.TYPE) {
                const dropdownButtonPressedEvent = e as DropdownButtonPressedEvent;

                if (dropdownButtonPressedEvent.button.id == "PageSelect") {
                    const id = dropdownButtonPressedEvent.childId;
                    const img = this._imageIdMap.get(id);
                    this.trigger(new ImageSelectedEvent(this, img));
                }

                return;
            }

            if (e.type == ImageChangedEvent.TYPE) {
                const icEvent: ImageChangedEvent = e as ImageChangedEvent;
                if (icEvent.image != null) {
                    if (!this._settings.mobile) {
                        const select = this._toolbarController.getView("PageSelect").getElement();
                        //select.find("option[selected]").removeAttr("selected");
                        select.val(icEvent.image.id);
                    }
                }
                return;
            }

            if (e.type == CanvasTapedEvent.TYPE) {
                this._toolbarController.getView(null).getElement().slideToggle();
                return;
            }
        }
    }

    public get handlesEvents(): string[] {
        const handleEvents: Array<string> = new Array<string>();
        handleEvents.push(StructureModelLoadedEvent.TYPE);
        handleEvents.push(ProvideToolbarModelEvent.TYPE);
        handleEvents.push(DropdownButtonPressedEvent.TYPE);
        handleEvents.push(ImageChangedEvent.TYPE);
        handleEvents.push(CanvasTapedEvent.TYPE);
        handleEvents.push(LanguageModelLoadedEvent.TYPE);
        return handleEvents;
    }

    public get toolbar() {
        return this._toolbarController;
    }

    private _toolbarModel: MyCoReBasicToolbarModel;
    private _toolbarController: IviewToolbar;
    private _imageIdMap: MyCoReMap<string, StructureImage> = null;

}

