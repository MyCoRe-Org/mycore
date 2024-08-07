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
import {ChapterTreeInputHandler} from "../widgets/chapter/ChapterTreeInputHandler";
import {MyCoReMap, Utils} from "../Utils";
import {MyCoReViewerSettings} from "../MyCoReViewerSettings";
import {IviewChapterTree} from "../widgets/chapter/IviewChapterTree";
import {ChapterTreeSettings, DefaultChapterTreeSettings} from "../widgets/chapter/ChapterTreeSettings";
import {StructureModel} from "./model/StructureModel";
import {StructureImage} from "./model/StructureImage";
import {WaitForEvent} from "./events/WaitForEvent";
import {StructureModelLoadedEvent} from "./events/StructureModelLoadedEvent";
import {LanguageModelLoadedEvent} from "./events/LanguageModelLoadedEvent";
import {ShowContentEvent} from "./events/ShowContentEvent";
import {ProvideToolbarModelEvent} from "./events/ProvideToolbarModelEvent";
import {ImageChangedEvent} from "./events/ImageChangedEvent";
import {DropdownButtonPressedEvent} from "../widgets/toolbar/events/DropdownButtonPressedEvent";
import {RequestStateEvent} from "./events/RequestStateEvent";
import {RestoreStateEvent} from "./events/RestoreStateEvent";
import {ViewerEvent} from "../widgets/events/ViewerEvent";
import {ComponentInitializedEvent} from "./events/ComponentInitializedEvent";
import {StructureChapter} from "./model/StructureChapter";
import {ImageSelectedEvent} from "./events/ImageSelectedEvent";
import {ChapterChangedEvent} from "./events/ChapterChangedEvent";

/**
     * Settings
     * chapter.enabled: boolean // enables the chapter in toolbar dropdown menu
     * chapter.showOnStart: boolean // should the chapter be opened on start (only desktop viewer)
     */
    export class MyCoReChapterComponent extends ViewerComponent implements ChapterTreeInputHandler {

        constructor(private _settings: MyCoReViewerSettings) {
            super();
            this._enabled = Utils.getVar(this._settings, "chapter.enabled", true);
        }


        private _enabled:boolean;
        private _chapterWidgetSettings: ChapterTreeSettings;
        private _chapterWidget: IviewChapterTree;
        private _spinner:JQuery = null;
        private _currentChapter:StructureChapter = null;

        private _structureModel: StructureModel;
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
                this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));
                this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));

                let oldProperty = Utils.getVar<boolean>(this._settings, "chapter.showOnStart",
                    window.innerWidth >= 1200);
                let showChapterOnStart = Utils.getVar<string>(this._settings,
                        "leftShowOnStart", oldProperty ? "chapterOverview" : "none") == "chapterOverview";

                if (this._settings.mobile == false && showChapterOnStart) {
                    this._spinner = jQuery(`<div class='spinner'><img src='${this._settings.webApplicationBaseURL}` +
                        `/modules/iview2/img/spinner.gif'></div>`);
                    this._container.append(this._spinner);
                    let direction = (this._settings.mobile) ? ShowContentEvent.DIRECTION_CENTER :
                        ShowContentEvent.DIRECTION_WEST;
                    this.trigger(new ShowContentEvent(this, this._container, direction, 300, this._sidebarLabel));
                }
            } else {
                this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));
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
                handles.push(StructureModelLoadedEvent.TYPE);
                handles.push(ImageChangedEvent.TYPE);
                handles.push(ShowContentEvent.TYPE);
                handles.push(DropdownButtonPressedEvent.TYPE);
                handles.push(LanguageModelLoadedEvent.TYPE);
                handles.push(RequestStateEvent.TYPE);
                handles.push(RestoreStateEvent.TYPE);
                handles.push(ChapterChangedEvent.TYPE);
            } else {
                handles.push(ProvideToolbarModelEvent.TYPE);
            }

            return handles;
        }

        public handle(e: ViewerEvent): void {
            if (e.type === ProvideToolbarModelEvent.TYPE) {
                const ptme = e as ProvideToolbarModelEvent;
                ptme.model._sidebarControllDropdownButton.children = ptme.model._sidebarControllDropdownButton
                    .children
                    .filter((my) => my.id !== 'chapterOverview');
            }

            if (e.type === DropdownButtonPressedEvent.TYPE) {
                const dropdownButtonPressedEvent = e as DropdownButtonPressedEvent;

                if (dropdownButtonPressedEvent.childId == "chapterOverview") {
                    const direction = (this._settings.mobile) ? ShowContentEvent.DIRECTION_CENTER : ShowContentEvent.DIRECTION_WEST;
                    this.trigger(new ShowContentEvent(this, this._container, direction, -1, this._sidebarLabel));
                    this.updateContainerSize();
                }
            }


            if (e.type === StructureModelLoadedEvent.TYPE) {
                let structureModelLoadedEvent = e as StructureModelLoadedEvent;
                let model = structureModelLoadedEvent.structureModel._rootChapter;

                this._structureModel = structureModelLoadedEvent.structureModel;

                this._structureModel._imageList.forEach((img) => {
                    this._idImageMap.set(img.id, img);
                    if ('orderLabel' in img && img.orderLabel !== null) {
                        this._autoPagination = false;
                    }
                });

                let chapterLabelMap = this.createChapterLabelMap(this._structureModel);
                this._chapterWidgetSettings = new DefaultChapterTreeSettings(this._container, chapterLabelMap, model, this._settings.mobile, this);
                this._chapterWidget = new IviewChapterTree(this._chapterWidgetSettings);
                this._initialized = true;
                this.trigger(new ComponentInitializedEvent(this));
                if (this._spinner != null) {
                    this._spinner.detach();
                }
                if (this._chapterToActivate != null) {
                    this.setChapter(this._chapterToActivate);
                }
            }


            if (e.type === ImageChangedEvent.TYPE) {
                if (typeof this._structureModel === "undefined" || this._structureModel._imageToChapterMap.isEmpty()) {
                    return;
                }
                const imageChangedEvent = e as ImageChangedEvent;
                if (imageChangedEvent.image != null && this._initialized) {
                    if (this._chapterWidget.getSelectedChapter() == null ||
                        this._structureModel.chapterToImageMap.get(this._chapterWidget.getSelectedChapter().id) != imageChangedEvent.image) {
                        let newChapter = this._structureModel._imageToChapterMap.get(imageChangedEvent.image.id);
                        if (newChapter != null) {
                            this._chapterWidget.setChapterExpanded(newChapter as StructureChapter, true);
                            this._chapterWidget.jumpToChapter(newChapter as StructureChapter);
                        }
                    }
                }
            }

            if (e.type === LanguageModelLoadedEvent.TYPE) {
                const lmle = e as LanguageModelLoadedEvent;
                this._sidebarLabel.text(lmle.languageModel.getTranslation("sidebar.chapterOverview"));
            }

            if (e.type === RequestStateEvent.TYPE) {
                const rse = e as RequestStateEvent;
                if (this._currentChapter != null) {
                    rse.stateMap.set("logicalDiv", this.persistChapterToString(this._currentChapter));
                }
            }

            if (e.type === RestoreStateEvent.TYPE) {
                const rse = e as RestoreStateEvent;
                let activateChapter = (div) => {
                    if (this._initialized) {
                        this.setChapter(div);
                    } else {
                        this._chapterToActivate = div;
                    }
                };
                rse.restoredState.hasThen('logicalDiv', activateChapter);
                rse.restoredState.hasThen('div', activateChapter);

            }

            if (e.type === ChapterChangedEvent.TYPE) {
                let cce = e as ChapterChangedEvent;
                if (cce == null || cce.chapter == null) {
                    return;
                }
                this._chapterWidget.setChapterExpanded(cce.chapter as StructureChapter, true);
                this._chapterWidget.jumpToChapter(cce.chapter as StructureChapter);
            }
        }

        private persistChapterToString(chapter:StructureChapter):string{
            return chapter.id;
        }

        private createChapterLabelMap(model: StructureModel): MyCoReMap<string, string> {
            const chapterLabelMap = new MyCoReMap<string, string>();
            model.chapterToImageMap.forEach((k: string, v: StructureImage) => {
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

        private setChapter(id: string, jumpToFirstImageOfChapter:boolean = true, node?: JQuery, trigger=true) {
            if(this._currentChapter != null && this._currentChapter.id == id) {
                return;
            }
            let newSelectedChapter = this._chapterWidget.getChapterById(id);
            if(newSelectedChapter == null) {
                return;
            }
            let changeChapter = (firstImageOfChapter) => {
                this._currentChapter =newSelectedChapter as StructureChapter;

                this._chapterWidget.setChapterExpanded(newSelectedChapter as StructureChapter, true);
                this._chapterWidget.setChapterSelected(newSelectedChapter as StructureChapter);
                this._chapterWidget.jumpToChapter(newSelectedChapter as StructureChapter);

                if(trigger){
                    this.trigger(new ChapterChangedEvent(this, newSelectedChapter as StructureChapter));
                }
                if (typeof firstImageOfChapter !== "undefined" && firstImageOfChapter !== null && jumpToFirstImageOfChapter) {
                    this.trigger(new ImageSelectedEvent(this, firstImageOfChapter));
                }
            };

            if (this._structureModel._chapterToImageMap.has(id)) {
                let firstImageOfChapter = this._structureModel._chapterToImageMap.get(id);
                changeChapter(firstImageOfChapter);
            } else {
                let oldVal: null|string = null;
                if (typeof node != "undefined") {
                    oldVal = node.css("cursor");
                    node.css("cursor", "wait");
                }
                newSelectedChapter.resolveDestination((targetId) => {
                    if (typeof node != "undefined" && oldVal != null) {
                        node.css("cursor", oldVal);
                    }
                    changeChapter(targetId !== null ? this._idImageMap.get(targetId) : null);
                });
            }
        }

        registerExpander(expander: JQuery, id: string): void {
            expander.click(()=> {
                const chapterToChange = this._chapterWidget.getChapterById(id);
                this._chapterWidget.setChapterExpanded(chapterToChange, !this._chapterWidget.getChapterExpanded(chapterToChange));
            });
        }

        private _container: JQuery;

        public get container() {
            return this._container;
        }
    }

