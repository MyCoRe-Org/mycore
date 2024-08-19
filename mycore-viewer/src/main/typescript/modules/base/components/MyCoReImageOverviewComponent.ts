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
import {ThumbnailOverviewInputHandler} from "../widgets/thumbnail/ThumbnailOverviewInputHandler";
import {MyCoReViewerSettings} from "../MyCoReViewerSettings";
import {MyCoReMap, Utils} from "../Utils";
import {IviewThumbnailOverview} from "../widgets/thumbnail/IviewThumbnailOverview";
import {DefaultThumbnailOverviewSettings} from "../widgets/thumbnail/ThumbnailOverviewSettings";
import {StructureImage} from "./model/StructureImage";
import {LanguageModelLoadedEvent} from "./events/LanguageModelLoadedEvent";
import {StructureModelLoadedEvent} from "./events/StructureModelLoadedEvent";
import {WaitForEvent} from "./events/WaitForEvent";
import {ShowContentEvent} from "./events/ShowContentEvent";

import {DropdownButtonPressedEvent} from "../widgets/toolbar/events/DropdownButtonPressedEvent";
import {ViewerEvent} from "../widgets/events/ViewerEvent";
import {ComponentInitializedEvent} from "./events/ComponentInitializedEvent";
import {ProvideToolbarModelEvent} from "./events/ProvideToolbarModelEvent";
import {ImageChangedEvent} from "./events/ImageChangedEvent";
import {ThumbnailOverviewThumbnail} from "../widgets/thumbnail/ThumbnailOverviewThumbnail";
import {ImageSelectedEvent} from "./events/ImageSelectedEvent";


/**
 * imageOverview.enabled: boolean   should the image overview be enabled in toolbar dropwdown menu
 */
export class MyCoReImageOverviewComponent extends ViewerComponent implements ThumbnailOverviewInputHandler {

    constructor(private _settings: MyCoReViewerSettings) {
        super();
        this._enabled = Utils.getVar<boolean>(this._settings, "imageOverview.enabled", true);
    }

    private _enabled: boolean;
    private _container: JQuery;
    private _overview: IviewThumbnailOverview;
    private _overviewSettings: DefaultThumbnailOverviewSettings;
    private _sidebarLabel = jQuery("<span>Bildübersicht</span>");
    private _currentImageId: string = null;
    private _idMetsImageMap: MyCoReMap<string, StructureImage>;
    private _spinner: JQuery = null;

    public init() {
        if (this._enabled) {
            this._container = jQuery("<div></div>");
            this._idMetsImageMap = new MyCoReMap<string, StructureImage>();
            this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));
            this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));

            const showImageOverViewOnStart = Utils.getVar<string>(this._settings, "leftShowOnStart", "chapterOverview")
                == "imageOverview";
            if (this._settings.mobile == false && showImageOverViewOnStart) {
                this._spinner = jQuery(`<div class='spinner'><img src='${this._settings.webApplicationBaseURL}` +
                    `/modules/iview2/img/spinner.gif'></div>`);
                this._container.append(this._spinner);

                let direction = (this._settings.mobile)
                    ? ShowContentEvent.DIRECTION_CENTER : ShowContentEvent.DIRECTION_WEST;
                this.trigger(new ShowContentEvent(this, this._container, direction, 300, this._sidebarLabel));
            }
        } else {
            this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));
        }
    }

    public get content() {
        return this._container;
    }

    public get handlesEvents(): string[] {
        let handles = new Array<string>();

        if (this._enabled) {
            handles.push(StructureModelLoadedEvent.TYPE);
            handles.push(ImageChangedEvent.TYPE);
            handles.push(DropdownButtonPressedEvent.TYPE);
            handles.push(ShowContentEvent.TYPE);
            handles.push(LanguageModelLoadedEvent.TYPE);
        } else {
            handles.push(ProvideToolbarModelEvent.TYPE);
        }

        return handles;
    }

    /// TODO: jump to the right image
    public handle(e: ViewerEvent): void {
        if (e.type == ProvideToolbarModelEvent.TYPE) {
            const ptme = e as ProvideToolbarModelEvent;
            ptme.model._sidebarControllDropdownButton.children = ptme.model._sidebarControllDropdownButton.children.filter((my) => my.id != "imageOverview");
        }


        if (e.type == DropdownButtonPressedEvent.TYPE) {
            const dropdownButtonPressedEvent = e as DropdownButtonPressedEvent;

            if (dropdownButtonPressedEvent.childId == "imageOverview") {
                const direction = (this._settings.mobile) ? ShowContentEvent.DIRECTION_CENTER : ShowContentEvent.DIRECTION_WEST;
                this.trigger(new ShowContentEvent(this, this._container, direction, -1, this._sidebarLabel));
                this._overview.update(true);
                this._overview.jumpToThumbnail(this._currentImageId);
            }
        }

        if (e.type == StructureModelLoadedEvent.TYPE) {
            const imageList = (e as StructureModelLoadedEvent).structureModel._imageList;
            const basePath = this._settings.tileProviderPath + this._settings.derivate + "/";
            this._overviewSettings = new DefaultThumbnailOverviewSettings(this.prepareModel(imageList, basePath), this._container, this);
            this._overview = new IviewThumbnailOverview(this._overviewSettings);
            const startImage = (this._settings.filePath.indexOf("/") == 0) ? this._settings.filePath.substr(1) : this._settings.filePath;
            this._overview.jumpToThumbnail(startImage);
            this._overview.setThumbnailSelected(startImage);
            this._currentImageId = startImage;
            if (this._spinner != null) {
                this._spinner.detach();
            }
            this.trigger(new ComponentInitializedEvent(this));
        }

        if (e.type == ImageChangedEvent.TYPE) {
            const imageChangedEvent = e as ImageChangedEvent;
            if (typeof this._overview != "undefined") {
                this._overview.jumpToThumbnail(imageChangedEvent.image.id);
                this._overview.setThumbnailSelected(imageChangedEvent.image.id);
                this._currentImageId = imageChangedEvent.image.id;
            }
        }

        if (e.type == LanguageModelLoadedEvent.TYPE) {
            const lmle = <LanguageModelLoadedEvent>e;
            this._sidebarLabel.text(lmle.languageModel.getTranslation("sidebar.imageOverview"));
        }

        return;
    }

    public prepareModel(images: Array<StructureImage>, basePath: string): Array<ThumbnailOverviewThumbnail> {
        const result = new Array<ThumbnailOverviewThumbnail>();

        for (let imageIndex in images) {
            const image = images[imageIndex];
            const label = "" + (image.orderLabel || image.order);
            const id = image.id;

            this._idMetsImageMap.set(id, image);
            result.push({id: id, label: label, requestImgdataUrl: image.requestImgdataUrl});
        }


        return result;
    }


    public addedThumbnail(id: string, element: JQuery): void {
        jQuery(element).bind("click",  () => {
            this.trigger(new ImageSelectedEvent(this, this._idMetsImageMap.get(id)));
        });
    }
}

