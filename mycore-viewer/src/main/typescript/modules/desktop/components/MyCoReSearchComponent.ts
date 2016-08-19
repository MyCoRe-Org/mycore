/// <reference path="../definitions/bootstrap.d.ts" />
/// <reference path="model/MyCoReViewerSearcher.ts" />
/// <reference path="events/ProvideViewerSearcherEvent.ts" />

module mycore.viewer.components {
    import AreaInPage = mycore.viewer.widgets.canvas.AreaInPage;
    export class MyCoReSearchComponent extends ViewerComponent {
        private onchange;

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        private _container:JQuery;
        private _searchContainer:JQuery;
        private _sidebarLabel = jQuery("<span>Suche</span>");
        private _model:model.StructureModel = null;
        private _searchForm:JQuery = null;
        private  _valueToApply:string = null
        private _containerVisible:boolean = false;
        private _indexPrepared:boolean = false;
        private _indexPreparing:boolean = false;
        private _textPresent:boolean = false;

        private _searcher:model.MyCoReViewerSearcher = null;
        private _imageHrefImageMap:MyCoReMap<string, model.StructureImage> = new MyCoReMap<string, model.StructureImage>();

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
                let searchLabel = _self._languageModel.getTranslation("sidebar.search");
                _self._sidebarLabel.text(searchLabel);
                _self._tbModel._dropdownChildren.unshift({id : "search", label : searchLabel});
                _self._tbModel._sidebarControllDropdownButton.children = _self._tbModel._dropdownChildren;
        });

        private _tbModel:model.MyCoReBasicToolbarModel = null;
        private _languageModel:model.LanguageModel = null;

        private _panel:JQuery = null;
        private _progressbar:JQuery = null;
        private _progressbarInner:JQuery = null;
        private _searchTextTimeout = -1;

        public get container() {
            return this._container;
        }

        private addSearchForm() {
            this._progressbar.remove();
            this._container.css({"text-align": "left"});
            this._searchForm = jQuery("<input id='search' type='text' class='form-control' />");

            this._panel.append(this._searchForm);

            this._searchContainer = jQuery("<ul class='list-group textSearch'></ul>");
            this._searchContainer.appendTo(this.container);

            this.onchange = ()=> {
                if(this._searchTextTimeout != -1){
                    window.clearTimeout(this._searchTextTimeout);
                    this._searchTextTimeout = -1;
                }

                this._searchTextTimeout = window.setTimeout(()=>{
                    this._search(this._searchForm.val());
                },300);
            };

            this._searchForm.bind("keyup", this.onchange);

            if(this._valueToApply!=null){
                this._searchForm.val(this._valueToApply);
                this._valueToApply = null;
                this.onchange();
            }
        }

        private _search(str:string) {
            this._searchContainer.children().remove();

            var textContents = new Array<model.TextElement>();
            var wordMap = new MyCoReMap<model.TextElement, Array<string>>();

            this._searcher.search(str, (searchResults)=> {
                searchResults.forEach((e)=> {
                    textContents.push(e.obj);
                    wordMap.set(e.obj, e.matchWords);
                });

                searchResults.forEach((e) => {
                    var result = jQuery("<li class='list-group-item'></li>");
                    var link = jQuery("<a></a>").append(e.context);
                    result.append(link);


                    this._searchContainer.append(result);
                    if (this._imageHrefImageMap.has(e.obj.pageHref)) {
                        var image = this._imageHrefImageMap.get(e.obj.pageHref);
                        link.click(()=> {
                            this.trigger(new events.ImageSelectedEvent(this, image));
                        });
                        var page = jQuery("<span class='childLabel'>" + (image.orderLabel || image.order) + "</span>");
                        result.append(page);
                    } else {
                        console.log("Could not find page " + e.obj.pageHref);
                    }

                });
            }, ()=> {
                this._marker.clearAll();
                textContents.forEach(tc=> {
                    this._marker.markArea(new AreaInPage(tc.pageHref, tc.pos.x, tc.pos.y, tc.size.width, tc.size.height));
                });
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

            jQuery(document.body).keypress((e)=> {
                if (e.keyCode == 102 && this._textPresent && jQuery("input:focus").length == 0) {
                    e.preventDefault();
                    this.openSearch();
                }
            });

            this.trigger(new events.WaitForEvent(this, events.ProvideViewerSearcherEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.MarkerInitializedEvent.TYPE));
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
            handleEvents.push(events.MarkerInitializedEvent.TYPE);
            handleEvents.push(events.RequestStateEvent.TYPE);
            handleEvents.push(events.RestoreStateEvent.TYPE);
            return handleEvents;
        }

        private _marker:mycore.viewer.widgets.canvas.CanvasMarker;

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
                if (sce.containerDirection == events.ShowContentEvent.DIRECTION_WEST && sce.content == this._container) {
                    if (sce.size == 0) {
                        this._marker.clearAll();
                    } else if (this._searchForm != null && this._searchForm.val().length > 0) {
                        this._search(this._searchForm.val());
                    }

                }
            }

            if (e.type == events.ProvideViewerSearcherEvent.TYPE) {
                var pvse = <events.ProvideViewerSearcherEvent>e;
                this._searcher = pvse.searcher;
                this._containerVisibleModelLoadedSync(this);
            }

            if (e.type == events.MarkerInitializedEvent.TYPE) {
                let mie = <events.MarkerInitializedEvent>e;
                this._marker = mie.marker;
            }

            if (e.type == events.RequestStateEvent.TYPE) {
                let rse = <events.RequestStateEvent>e;
                if(this._searchForm!=null){
                    let searchText = this._searchForm.val();
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
                    if(this._searchForm!=null){
                        this._searchForm.val("q");
                        this.onchange();
                    } else {
                        this._valueToApply = q;
                    }
                }
            }



        }

        private openSearch() {
            var direction = (this._settings.mobile) ? events.ShowContentEvent.DIRECTION_CENTER : events.ShowContentEvent.DIRECTION_WEST;
            this.trigger(new events.ShowContentEvent(this, this._container, direction, -1, this._sidebarLabel));
            this.focusSearch();
            this.updateContainerSize();

            this._containerVisible = true;
            this._containerVisibleModelLoadedSync(this);
        }

        private focusSearch() {
            if (this._searchForm != null) {
                this._searchForm.focus();
            }
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
                    this.addSearchForm();
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