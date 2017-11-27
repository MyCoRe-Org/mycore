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

/// <reference path="../definitions/bootstrap.d.ts" />
/// <reference path="model/MyCoReViewerSearcher.ts" />
/// <reference path="events/ProvideViewerSearcherEvent.ts" />
/// <reference path="../widgets/canvas/SearchResultCanvasPageLayer.ts" />

namespace mycore.viewer.components {

    export class MyCoReSearchComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        private _container:JQuery;
        private _searchContainer:JQuery;
        private _sidebarLabel = jQuery("<span>Suche</span>");
        private _model:model.StructureModel = null;
        private  _valueToApply:string = null;
        private _containerVisible:boolean = false;
        private _indexPrepared:boolean = false;
        private _indexPreparing:boolean = false;
        private _textPresent:boolean = false;
        private _toolbarTextInput: widgets.toolbar.ToolbarTextInput = new widgets.toolbar.ToolbarTextInput("search", "", "");

        private _searcher:model.MyCoReViewerSearcher = null;
        private _imageHrefImageMap:MyCoReMap<string, model.StructureImage> = new MyCoReMap<string, model.StructureImage>();

        private _searchResultCanvasPageLayer:widgets.canvas.SearchResultCanvasPageLayer = new widgets.canvas.SearchResultCanvasPageLayer();

        private _containerVisibleModelLoadedSync = Utils.synchronize<MyCoReSearchComponent>([
                (_self) => _self._model != null,
                (_self)=> _self._containerVisible,
                (_self)=>_self._searcher != null,
                (_self)=> !this._indexPrepared && !this._indexPreparing
            ],
            (_self)=> _self._prepareIndex(_self._model));

        private _toolbarLoadedLanguageModelLoadedSync = Utils.synchronize<MyCoReSearchComponent>(
            [
                (_self)=> _self._tbModel != null,
                (_self)=>_self._languageModel != null,
                (_self) => _self._model != null,
                (_self) => _self._textPresent
            ],
            (_self:MyCoReSearchComponent)=> {
                this.trigger(new events.AddCanvasPageLayerEvent(this, 1, this._searchResultCanvasPageLayer));
                let searchLabel = _self._languageModel.getTranslation("sidebar.search");
                this._toolbarTextInput.placeHolder = _self._languageModel.getTranslation("search.placeHolder");
                _self._sidebarLabel.text(searchLabel);
                _self._tbModel._searchGroup.addComponent(this._toolbarTextInput);
                if (this._valueToApply != null) {
                    this._toolbarTextInput.value = this._valueToApply;
                    this._valueToApply = null;
                }
        });

        private _tbModel:model.MyCoReBasicToolbarModel = null;
        private _languageModel:model.LanguageModel = null;

        private _panel:JQuery = null;
        private _progressbar:JQuery = null;
        private _progressbarInner:JQuery = null;
        private _searchTextTimeout = -1;
        private _searchAreaReady = false;

        public get container() {
            return this._container;
        }

        private initSearchArea() {
            this._progressbar.parent().remove();
            this._progressbar.remove();
            this._container.css({"text-align": "left"});


            this._searchContainer = jQuery("<ul class='list-group textSearch'></ul>");
            this._searchContainer.appendTo(this.container);

            this._searchAreaReady = true;
            this._search(this._toolbarTextInput.value);
        }

        private _search(str:string) {
            if(str==""){
                let direction = (this._settings.mobile) ? events.ShowContentEvent.DIRECTION_CENTER : events.ShowContentEvent.DIRECTION_EAST;
                this.trigger(new events.ShowContentEvent(this, this._container, direction, 0, this._sidebarLabel));
            }

            this._searchContainer.children().remove();

            let textContents = [];

            this._searcher.search(str, (searchResults)=> {
                searchResults.forEach((results)=> {
                    results.arr.forEach(p => textContents.push(p));
                });

                let lastClicked:JQuery = null;
                searchResults.forEach((results) => {
                    if(results.arr.length <= 0) {
                        return;
                    }
                    let result = jQuery("<li class='list-group-item'></li>");
                    let link = jQuery("<a></a>").append(results.context.clone());
                    result.append(link);
                    this._searchContainer.append(result);
                    let altoTextContent = results.arr[0];
                    if (!this._imageHrefImageMap.has(altoTextContent.pageHref)) {
                        console.log("Could not find page " + altoTextContent.pageHref);
                        return;
                    }
                    let image = this._imageHrefImageMap.get(altoTextContent.pageHref);
                    link.click(() => {
                        if (lastClicked != null) {
                            lastClicked.removeClass("active");
                            this._searchResultCanvasPageLayer.clearSelected();
                        }
                        lastClicked = result;
                        result.addClass("active");
                        results.arr.forEach(context => {
                            let areaRect: Rect = Rect.fromXYWH(context.pos.x, context.pos.y, context.size.width, context.size.height);
                            this._searchResultCanvasPageLayer.select(context.pageHref, areaRect);
                        });
                        this.trigger(new events.ImageSelectedEvent(this, image));
                        this.trigger(new events.RedrawEvent(this));
                    });
                    let page = jQuery("<span class='childLabel'>" + (image.orderLabel || image.order) + "</span>");
                    result.append(page);
                });
            }, ()=> {
                this._searchResultCanvasPageLayer.clear();
                textContents.forEach(tc => {
                    let areaRect:Rect = Rect.fromXYWH(tc.pos.x, tc.pos.y, tc.size.width, tc.size.height);
                    this._searchResultCanvasPageLayer.add(tc.pageHref, areaRect);
                });
                this.trigger(new events.RedrawEvent(this));
            });
        }

        public init() {
            this._container = jQuery("<div></div>");
            this._container.css({ overflowY: "scroll", "text-align": "center" });
            this._container.bind("iviewResize", ()=> {
                this.updateContainerSize();
            });

            this._panel = jQuery("<div class='panel search'></div>");
            this._container.append(this._panel);

            this._initProgressbar();
            this._panel.append(this._progressbar);


            this._toolbarTextInput.getProperty("value").addObserver({
                propertyChanged : (_old: ViewerProperty<string>, _new: ViewerProperty<string>)=> {

                    this.openSearch();

                    if (this._searchAreaReady) {
                        if (this._searchTextTimeout != -1) {
                            window.clearTimeout(this._searchTextTimeout);
                            this._searchTextTimeout = -1;
                        }

                        this._searchTextTimeout = window.setTimeout(()=> {
                            this._search(this._toolbarTextInput.value);
                        }, 300);
                    }

                }
            });

            this.trigger(new events.WaitForEvent(this, events.ProvideViewerSearcherEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.RestoreStateEvent.TYPE));
        }

        private updateContainerSize() {
            this._container.css({"height": (this._container.parent().height() - this._sidebarLabel.parent().outerHeight()) + "px"});
        }

        public get handlesEvents():string[] {
            var handleEvents = new Array<string>();
            handleEvents.push(events.StructureModelLoadedEvent.TYPE);
            handleEvents.push(mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE);
            handleEvents.push(events.ProvideToolbarModelEvent.TYPE);
            handleEvents.push(events.LanguageModelLoadedEvent.TYPE);
            handleEvents.push(events.ShowContentEvent.TYPE);
            handleEvents.push(events.ProvideViewerSearcherEvent.TYPE);
            handleEvents.push(events.RequestStateEvent.TYPE);
            handleEvents.push(events.RestoreStateEvent.TYPE);
            return handleEvents;
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var smle = <events.StructureModelLoadedEvent>e;
                this._model = smle.structureModel;
                this._textPresent = smle.structureModel._textContentPresent;
                this._toolbarLoadedLanguageModelLoadedSync(this);
                this._containerVisibleModelLoadedSync(this);
            }

            if (e.type == mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
                var dropdownButtonPressedEvent = <mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent> e;

                if (dropdownButtonPressedEvent.childId == "search") {
                    this.openSearch();
                }
            }

            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent>e;
                this._tbModel = ptme.model;
                this._toolbarLoadedLanguageModelLoadedSync(this);
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                var lmle = <events.LanguageModelLoadedEvent>e;
                this._languageModel = lmle.languageModel;
                this._toolbarLoadedLanguageModelLoadedSync(this);
            }

            if (e.type == events.ShowContentEvent.TYPE) {
                var sce = <events.ShowContentEvent> e;
                if (sce.containerDirection == events.ShowContentEvent.DIRECTION_EAST && sce.content == this._container) {
                    if (sce.size == 0) {
                        this._searchResultCanvasPageLayer.clear();
                        this.trigger(new events.RedrawEvent(this));
                    } else if (this._searchAreaReady && this._toolbarTextInput.value.length > 0) {
                        this._search(this._toolbarTextInput.value);
                    }

                }
            }

            if (e.type == events.ProvideViewerSearcherEvent.TYPE) {
                var pvse = <events.ProvideViewerSearcherEvent>e;
                this._searcher = pvse.searcher;
                this._containerVisibleModelLoadedSync(this);
            }

            if (e.type == events.RequestStateEvent.TYPE) {
                let rse = <events.RequestStateEvent>e;
                if (this._searchAreaReady != null) {
                    let searchText = this._toolbarTextInput.value;
                    if (searchText != null && searchText != "") {
                        rse.stateMap.set("q", searchText);
                    }
                }

            }

            if (e.type == events.RestoreStateEvent.TYPE) {
                let rse = <events.RestoreStateEvent>e;
                if(rse.restoredState.has("q")){
                    let q = rse.restoredState.get("q");
                    this.openSearch();
                    if (this._searchAreaReady != null) {
                        this._toolbarTextInput.value = q;
                    } else {
                        this._valueToApply = q;
                    }
                }
            }



        }

        private openSearch() {
            var direction = (this._settings.mobile) ? events.ShowContentEvent.DIRECTION_CENTER : events.ShowContentEvent.DIRECTION_EAST;
            this.trigger(new events.ShowContentEvent(this, this._container, direction, -1, this._sidebarLabel));
            this.updateContainerSize();

            this._containerVisible = true;
            this._containerVisibleModelLoadedSync(this);
        }


        private _prepareIndex(model:model.StructureModel) {
            this._model._imageList.forEach((image)=> {
                this._imageHrefImageMap.set(image.href, image);
            });

            this._indexPreparing = true;
            this._searcher.index(model, (id, callback)=> {
                // callback to receive ocr stuff
                this.trigger(new events.RequestTextContentEvent(this, id, callback));
            }, (x, ofY)=> {
                // Callback to update progressbar
                this._updateLabel(x, ofY);
                // and to complete indexing
                if (ofY == (x)) {
                    this._indexPrepared = true;
                    this.initSearchArea();
                }
            });
        }

        private _updateLabel(current:number, of:number) {
            this._progressbarInner.attr({"aria-valuenow": current, "aria-valuemin": 0, "aria-valuemax": of});
            this._progressbarInner.css({ width: ((current / of) * 100) + "%"});
        }

        private _initProgressbar() {
            this._progressbar = jQuery("<div></div>");
            this._progressbar.addClass("progress");

            this._progressbarInner = jQuery("<div></div>");
            this._progressbarInner.addClass("progress-bar progress-bar-info");
            this._progressbarInner.appendTo(this._progressbar);

            this._progressbarInner.attr({role: "progressbar"});
        }

    }
}

addViewerComponent(mycore.viewer.components.MyCoReSearchComponent);
