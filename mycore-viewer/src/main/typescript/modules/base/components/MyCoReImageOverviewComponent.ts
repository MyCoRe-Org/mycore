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
/// <reference path="../Utils.ts" />
/// <reference path="ViewerComponent.ts" />
/// <reference path="events/StructureModelLoadedEvent.ts" />
/// <reference path="events/ComponentInitializedEvent.ts" />
/// <reference path="events/WaitForEvent.ts" />
/// <reference path="events/ImageSelectedEvent.ts" />
/// <reference path="events/ImageChangedEvent.ts" />
/// <reference path="events/ShowContentEvent.ts" />
/// <reference path="model/StructureImage.ts" />
/// <reference path="../MyCoReViewerSettings.ts" />
/// <reference path="../widgets/events/ViewerEvent.ts" />
/// <reference path="../widgets/thumbnail/IviewThumbnailOverview.ts" />
/// <reference path="../widgets/thumbnail/ThumbnailOverviewSettings.ts" />
/// <reference path="../widgets/thumbnail/ThumbnailOverviewThumbnail.ts" />
/// <reference path="../widgets/thumbnail/ThumbnailOverviewInputHandler.ts" />
/// <reference path="../widgets/toolbar/events/DropdownButtonPressedEvent.ts" />

namespace mycore.viewer.components {

    /**
     * imageOverview.enabled: boolean   should the image overview be enabled in toolbar dropwdown menu
     */
    export class MyCoReImageOverviewComponent extends ViewerComponent implements widgets.thumbnail.ThumbnailOverviewInputHandler {

        constructor(private _settings: MyCoReViewerSettings) {
            super();
            this._enabled = Utils.getVar<boolean>(this._settings, "imageOverview.enabled", true);
        }

        private _enabled:boolean;
        private _container: JQuery;
        private _overview: widgets.thumbnail.IviewThumbnailOverview;
        private _overviewSettings: widgets.thumbnail.DefaultThumbnailOverviewSettings;
        private _sidebarLabel = jQuery("<span>Bildübersicht</span>");
        private _currentImageId:string = null;
        private _idMetsImageMap: MyCoReMap<string, model.StructureImage>;
        private _spinner: JQuery = null;

        public init() {
            if (this._enabled) {
                this._container = jQuery("<div></div>");
                this._idMetsImageMap = new MyCoReMap<string, model.StructureImage>();
                this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
                this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));

                let showImageOverViewOnStart = Utils.getVar<string>(this._settings, "leftShowOnStart", "chapterOverview")
                    == "imageOverview";
                if (this._settings.mobile == false && showImageOverViewOnStart) {
                    this._spinner = jQuery(`<div class='spinner'><img src='${this._settings.webApplicationBaseURL}` +
                        `/modules/iview2/img/spinner.gif'></div>`);
                    this._container.append(this._spinner);

                    let direction = (this._settings.mobile)
                        ? events.ShowContentEvent.DIRECTION_CENTER : events.ShowContentEvent.DIRECTION_WEST;
                    this.trigger(new events.ShowContentEvent(this, this._container, direction, 300, this._sidebarLabel));
                }
            } else {
                this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
            }
        }

        public get content() {
            return this._container;
        }

        public get handlesEvents(): string[] {
            let handles = new Array<string>();

            if (this._enabled) {
                handles.push(events.StructureModelLoadedEvent.TYPE);
                handles.push(events.ImageChangedEvent.TYPE);
                handles.push(mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE);
                handles.push(events.ShowContentEvent.TYPE);
                handles.push(events.LanguageModelLoadedEvent.TYPE);
            } else {
                handles.push(events.ProvideToolbarModelEvent.TYPE);
            }

            return handles;
        }

        /// TODO: jump to the right image
        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent>e;
                ptme.model._sidebarControllDropdownButton.children = ptme.model._sidebarControllDropdownButton.children.filter((my)=>my.id != "imageOverview");
            }


            if (e.type == mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
                var dropdownButtonPressedEvent = <mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent> e;

                if (dropdownButtonPressedEvent.childId == "imageOverview") {
                    var direction = (this._settings.mobile) ? events.ShowContentEvent.DIRECTION_CENTER : events.ShowContentEvent.DIRECTION_WEST;
                    this.trigger(new events.ShowContentEvent(this, this._container, direction, -1, this._sidebarLabel));
                    this._overview.update(true);
                    this._overview.jumpToThumbnail(this._currentImageId);
                }
            }

            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var imageList = (<events.StructureModelLoadedEvent>e).structureModel._imageList;
                var basePath = this._settings.tileProviderPath + this._settings.derivate + "/";
                this._overviewSettings = new mycore.viewer.widgets.thumbnail.DefaultThumbnailOverviewSettings(this.prepareModel(imageList, basePath), this._container, this);
                this._overview = new mycore.viewer.widgets.thumbnail.IviewThumbnailOverview(this._overviewSettings);
                var startImage = (this._settings.filePath.indexOf("/") == 0) ? this._settings.filePath.substr(1) : this._settings.filePath;
                this._overview.jumpToThumbnail(startImage);
                this._overview.setThumbnailSelected(startImage);
                this._currentImageId = startImage;
                if (this._spinner != null) {
                    this._spinner.detach();
                }
                this.trigger(new events.ComponentInitializedEvent(this));
            }

            if (e.type == events.ImageChangedEvent.TYPE) {
                var imageChangedEvent = <events.ImageChangedEvent>e;
                if (typeof this._overview != "undefined") {
                    this._overview.jumpToThumbnail(imageChangedEvent.image.id);
                    this._overview.setThumbnailSelected(imageChangedEvent.image.id);
                    this._currentImageId = imageChangedEvent.image.id;
                }
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                var lmle = <events.LanguageModelLoadedEvent>e;
                this._sidebarLabel.text(lmle.languageModel.getTranslation("sidebar.imageOverview"));
            }

            return;
        }

        public prepareModel(images: Array<model.StructureImage>, basePath: string): Array<mycore.viewer.widgets.thumbnail.ThumbnailOverviewThumbnail> {
            var result = new Array<mycore.viewer.widgets.thumbnail.ThumbnailOverviewThumbnail>();

            for (var imageIndex in images) {
                var image = images[imageIndex];
                var path;
                if (image.href.indexOf("data:") == -1) {
                    path = basePath + image.href + "/0/0/0.jpg";
                } else {
                    path = image.href;
                }
                var label = "" + (image.orderLabel || image.order);
                var id = image.id;

                this._idMetsImageMap.set(id, image);
                result.push({ id: id, label: label, href: path, requestImgdataUrl: image.requestImgdataUrl });
            }


            return result;
        }


        public addedThumbnail(id: string, element: JQuery): void {
            var that = this;
            jQuery(element).bind("click", function() {
                that.trigger(new events.ImageSelectedEvent(that, that._idMetsImageMap.get(id)));
            });
        }
    }
}
