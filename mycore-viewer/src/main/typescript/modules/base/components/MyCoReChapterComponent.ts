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
/// <reference path="../MyCoReViewerSettings.ts" />
/// <reference path="../widgets/events/ViewerEvent.ts" />
/// <reference path="../widgets/chapter/IviewChapterTree.ts" />
/// <reference path="../widgets/chapter/ChapterTreeSettings.ts" />
/// <reference path="../widgets/chapter/ChapterTreeInputHandler.ts" />
/// <reference path="../widgets/toolbar/events/DropdownButtonPressedEvent.ts" />
/// <reference path="ViewerComponent.ts" />
/// <reference path="model/StructureChapter.ts" />
/// <reference path="model/StructureImage.ts" />
/// <reference path="events/StructureModelLoadedEvent.ts" />
/// <reference path="events/ComponentInitializedEvent.ts" />
/// <reference path="events/WaitForEvent.ts" />
/// <reference path="events/ImageSelectedEvent.ts" />
/// <reference path="events/ImageChangedEvent.ts" />
/// <reference path="events/ChapterChangedEvent.ts" />
/// <reference path="events/ShowContentEvent.ts" />
/// <reference path="events/RequestStateEvent.ts" />

namespace mycore.viewer.components {

    import StructureImage = mycore.viewer.model.StructureImage;
    /**
     * Settings
     * chapter.enabled: boolean // enables the chapter in toolbar dropdown menu
     * chapter.showOnStart: boolean // should the chapter be opened on start (only desktop viewer)
     */
    export class MyCoReChapterComponent extends ViewerComponent implements widgets.chaptertree.ChapterTreeInputHandler {

        constructor(private _settings: MyCoReViewerSettings) {
            super();
            this._enabled = Utils.getVar(this._settings, "chapter.enabled", true);
        }


        private _enabled:boolean;
        private _chapterWidgetSettings: widgets.chaptertree.ChapterTreeSettings;
        private _chapterWidget: widgets.chaptertree.IviewChapterTree;
        private _spinner:JQuery = null;
        private _currentChapter:model.StructureChapter = null;

        private _structureModel: model.StructureModel;
        private _initialized: boolean = false;
        private _sidebarLabel = jQuery("<span>strukturübersicht</span>");
        private _chapterToActivate:string = null;
        private _autoPagination = true;
        private _idImageMap: MyCoReMap<string, StructureImage> = new MyCoReMap<string, StructureImage>();

        public init() {
            if (this._enabled) {
                this._container = jQuery("<div></div>");
                this._container.css({ overflowY: "scroll" });
                this._container.bind("iviewResize", ()=> {
                    this.updateContainerSize();
                });
                this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
                this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));

                let oldProperty = Utils.getVar<boolean>(this._settings, "chapter.showOnStart",
                    window.innerWidth >= 1200);
                let showChapterOnStart = Utils.getVar<string>(this._settings,
                        "leftShowOnStart", oldProperty ? "chapterOverview" : "none") == "chapterOverview";

                if (this._settings.mobile == false && showChapterOnStart) {
                    this._spinner = jQuery(`<div class='spinner'><img src='${this._settings.webApplicationBaseURL}` +
                        `/modules/iview2/img/spinner.gif'></div>`);
                    this._container.append(this._spinner);
                    let direction = (this._settings.mobile) ? events.ShowContentEvent.DIRECTION_CENTER :
                        events.ShowContentEvent.DIRECTION_WEST;
                    this.trigger(new events.ShowContentEvent(this, this._container, direction, 300, this._sidebarLabel));
                }
            } else {
                this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
            }
        }

        private updateContainerSize () {
            this._container.css({
                "height" : (this._container.parent().height() - this._sidebarLabel.parent().outerHeight()) + "px"
            });
        }

        public get handlesEvents(): string[] {
            let handles = new Array<string>();

            if (this._enabled) {
                handles.push(events.StructureModelLoadedEvent.TYPE);
                handles.push(events.ImageChangedEvent.TYPE);
                handles.push(events.ShowContentEvent.TYPE);
                handles.push(mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE);
                handles.push(events.LanguageModelLoadedEvent.TYPE);
                handles.push(events.RequestStateEvent.TYPE);
                handles.push(events.RestoreStateEvent.TYPE);
                handles.push(events.ChapterChangedEvent.TYPE);
            } else {
                handles.push(events.ProvideToolbarModelEvent.TYPE);
            }

            return handles;
        }

        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                let ptme = <events.ProvideToolbarModelEvent>e;
                ptme.model._sidebarControllDropdownButton.children = ptme.model._sidebarControllDropdownButton.children.filter((my)=>my.id != "chapterOverview");
            }

            if (e.type == mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
                var dropdownButtonPressedEvent = <mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent> e;

                if (dropdownButtonPressedEvent.childId == "chapterOverview") {
                    var direction = (this._settings.mobile) ? events.ShowContentEvent.DIRECTION_CENTER : events.ShowContentEvent.DIRECTION_WEST;
                    this.trigger(new events.ShowContentEvent(this, this._container, direction, -1, this._sidebarLabel));
                    this.updateContainerSize();
                }
            }


            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                let structureModelLoadedEvent = <events.StructureModelLoadedEvent>e;
                let model = structureModelLoadedEvent.structureModel._rootChapter;

                this._structureModel = structureModelLoadedEvent.structureModel;

                this._structureModel._imageList.forEach(img=>{
                    this._idImageMap.set(img.id, img);
                   if("orderLabel" in img && img.orderLabel != null){
                       this._autoPagination = false;
                   }
                });

                let chapterLabelMap = this.createChapterLabelMap(this._structureModel);
                this._chapterWidgetSettings = new mycore.viewer.widgets.chaptertree.DefaultChapterTreeSettings(this._container, chapterLabelMap, model, this._settings.mobile, this);
                this._chapterWidget = new mycore.viewer.widgets.chaptertree.IviewChapterTree(this._chapterWidgetSettings);
                this._initialized = true;
                this.trigger(new events.ComponentInitializedEvent(this));
                if (this._spinner != null) {
                    this._spinner.detach();
                }
                if(this._chapterToActivate!=null){
                    this.setChapter(this._chapterToActivate);
                }
            }


            if (e.type == events.ImageChangedEvent.TYPE) {
                if (typeof this._structureModel === "undefined" || this._structureModel._imageToChapterMap.isEmpty()) {
                    return;
                }
                var imageChangedEvent = <events.ImageChangedEvent> e;
                if (imageChangedEvent.image != null && this._initialized) {
                    if (this._chapterWidget.getSelectedChapter() == null ||
                        this._structureModel.chapterToImageMap.get(this._chapterWidget.getSelectedChapter().id) != imageChangedEvent.image) {
                        let newChapter = this._structureModel._imageToChapterMap.get(imageChangedEvent.image.id);
                        if (newChapter != null) {
                            this._chapterWidget.setChapterExpanded(<model.StructureChapter>newChapter, true);
                            this._chapterWidget.jumpToChapter(<model.StructureChapter>newChapter);
                        }
                    }
                }
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                let lmle = <events.LanguageModelLoadedEvent>e;
                this._sidebarLabel.text(lmle.languageModel.getTranslation("sidebar.chapterOverview"));
            }

            if(e.type == events.RequestStateEvent.TYPE){
                let rse = <events.RequestStateEvent> e;
                if(this._currentChapter!=null){
                    rse.stateMap.set("logicalDiv",this.persistChapterToString(this._currentChapter));
                }
            }

            if(e.type == events.RestoreStateEvent.TYPE){
                let rse = <events.RestoreStateEvent>e;
                let activateChapter = (div)=>{
                    if(this._initialized){
                        this.setChapter(div);
                    } else {
                        this._chapterToActivate = div;
                    }
                };
                rse.restoredState.hasThen("logicalDiv", activateChapter);
                rse.restoredState.hasThen("div", activateChapter);

            }

            if(e.type === events.ChapterChangedEvent.TYPE) {
                let cce = <events.ChapterChangedEvent>e;
                if (cce == null || cce.chapter == null) {
                    return;
                }
                this.setChapter(cce.chapter.id, false);
            }

        }

        private persistChapterToString(chapter:model.StructureChapter):string{
            return chapter.id;
        }

        private createChapterLabelMap(model: model.StructureModel): MyCoReMap<string, string> {
            var chapterLabelMap = new MyCoReMap<string, string>();
            model.chapterToImageMap.forEach((k: string, v: model.StructureImage) => {
                chapterLabelMap.set(k, v.orderLabel || (this._autoPagination ? v.order.toString(10) : ""));
            });
            return chapterLabelMap;
        }

        register(): void {
        }

        registerNode(node: JQuery, id: string): void {
            node.click(() => {
                this.setChapter(id, true, node);
            });
        }

        private setChapter(id: string, jumpToFirstImageOfChapter:boolean = true, node?: JQuery) {
            if(this._currentChapter != null && this._currentChapter.id == id) {
                return;
            }
            let newSelectedChapter = this._chapterWidget.getChapterById(id);
            if(newSelectedChapter == null) {
                return;
            }
            let changeChapter = (firstImageOfChapter) => {
                if (typeof firstImageOfChapter != "undefined" && firstImageOfChapter !== null) {
                    this._currentChapter = <model.StructureChapter>newSelectedChapter;

                    this._chapterWidget.setChapterExpanded(<model.StructureChapter>newSelectedChapter, true);
                    this._chapterWidget.setChapterSelected(<model.StructureChapter>newSelectedChapter);
                    this._chapterWidget.jumpToChapter(<model.StructureChapter>newSelectedChapter);

                    this.trigger(new events.ChapterChangedEvent(this, <model.StructureChapter>newSelectedChapter));
                    if(jumpToFirstImageOfChapter) {
                        this.trigger(new events.ImageSelectedEvent(this, firstImageOfChapter));
                    }
                }
            };

            if (this._structureModel._chapterToImageMap.has(id)) {
                let firstImageOfChapter = this._structureModel._chapterToImageMap.get(id);
                changeChapter(firstImageOfChapter);
            } else {
                if (typeof node != "undefined") {
                    var oldVal = node.css("cursor");
                    node.css("cursor", "wait");
                }
                newSelectedChapter.resolveDestination((targetId) => {
                    if (typeof node != "undefined") {
                        node.css("cursor", oldVal);
                    }
                    changeChapter(this._idImageMap.get(targetId));
                });
            }
        }

        registerExpander(expander: JQuery, id: string): void {
            var that = this;
            expander.click(function() {
                var chapterToChange = that._chapterWidget.getChapterById(id);
                that._chapterWidget.setChapterExpanded(chapterToChange, !that._chapterWidget.getChapterExpanded(chapterToChange));
            });
        }

        private _container: JQuery;

        public get container() {
            return this._container;
        }
    }
}
