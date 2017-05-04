/// <reference path="../definitions/jquery.d.ts" />
/// <reference path="../Utils.ts" />
/// <reference path="ViewerComponent.ts" />
/// <reference path="../MyCoReViewerSettings.ts" />
/// <reference path="MyCoReToolbarComponent.ts" />
/// <reference path="../widgets/toolbar/events/ButtonPressedEvent.ts" />
/// <reference path="../widgets/events/ViewerEvent.ts" />
/// <reference path="../widgets/modal/ViewerPermalinkModalWindow.ts" />
/// <reference path="events/ComponentInitializedEvent.ts" />
/// <reference path="events/RequestStateEvent.ts" />
/// <reference path="events/RestoreStateEvent.ts" />
/// <reference path="events/ImageChangedEvent.ts" />
/// <reference path="events/WaitForEvent.ts" />
/// <reference path="events/LanguageModelLoadedEvent.ts" />
/// <reference path="events/RequestPermalinkEvent.ts" />
/// <reference path="model/LanguageModel.ts" />


namespace mycore.viewer.components {
    /**
     * permalink.updateHistory: boolean         if true the url will update on image change (default: true)
     * permalink.viewerLocationPattern :string  a patter wich will be used to build the location to the viewer. (default: {baseURL}rsc/viewer/{derivate}/{file})
     *
     */
    export class MyCoRePermalinkComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
            this._enabled = Utils.getVar(this._settings, "permalink.enabled", true);
        }

        private _enabled:boolean;
        private _modalWindow:widgets.modal.ViewerPermalinkModalWindow;
        private _currentState:ViewerParameterMap = new ViewerParameterMap();

        public init() {
            this.trigger(new events.ComponentInitializedEvent(this));

            if (this._enabled) {
                this._modalWindow = new mycore.viewer.widgets.modal.ViewerPermalinkModalWindow(this._settings.mobile);
                this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
                var that = this;
                var parameter = ViewerParameterMap.fromCurrentUrl();
                if (!parameter.isEmpty()) {
                    that.trigger(new events.RestoreStateEvent(that, parameter));
                }
            } else {
                this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
            }
        }


        public handle(e:mycore.viewer.widgets.events.ViewerEvent) {
            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent>e;
                var group = ptme.model.getGroup(ptme.model._actionControllGroup.name);

                if (typeof group != "undefined" && group != null) { // dont need to remove the component if the group is not added.
                    ptme.model.getGroup(ptme.model._actionControllGroup.name).removeComponent(ptme.model._shareButton);
                }
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                var languageModelLoadedEvent = <events.LanguageModelLoadedEvent>e;
                this._modalWindow.closeLabel = languageModelLoadedEvent.languageModel.getTranslation("permalink.close");
                this._modalWindow.title = languageModelLoadedEvent.languageModel.getTranslation("permalink.title");
            }

            if (e.type == widgets.toolbar.events.ButtonPressedEvent.TYPE) {
                var bpe = <widgets.toolbar.events.ButtonPressedEvent>e;
                if (bpe.button.id == "ShareButton") {
                    let state = new ViewerParameterMap();
                    this.trigger(new events.RequestStateEvent(this, state, true));
                    let permalink = this.buildPermalink(state);
                    this._modalWindow.permalink = permalink;
                    this._modalWindow.show();
                }
            }

            if (e.type == events.RequestPermalinkEvent.TYPE) {
                let rpe = <events.RequestPermalinkEvent> e;
                let state = new ViewerParameterMap();
                this.trigger(new events.RequestStateEvent(this, state, true));
                let permalink = this.buildPermalink(state);
                rpe.callback(permalink);
            }

            if (e.type == events.ImageChangedEvent.TYPE) {
                var ice = <events.ImageChangedEvent>e;
                if (typeof ice.image != "undefined") {
                    var updateHistory = Utils.getVar(this._settings, "permalink.updateHistory", true);
                    let state = new ViewerParameterMap();
                    this.trigger(new events.RequestStateEvent(this,state,false));
                    if (updateHistory) {
                        if (this._settings.doctype == "mets") {
                            window.history.replaceState({}, "", this.buildPermalink(state));
                        } else {
                            window.history.replaceState({}, "", this.buildPermalink(state));
                        }
                    }
                }
            }
        }

        private buildPermalink(state:ViewerParameterMap) {
            var file;
            if (this._settings.doctype == "pdf") {
                file = this._settings.filePath;
            } else {
                file = state.get("page");
                state.remove("page");
            }
            var baseURL = this.getBaseURL(file);
            state.remove("derivate");
            return baseURL + state.toParameterString();
        }

        private getBaseURL(file) {
            var pattern = Utils.getVar<string>(this._settings, "permalink.viewerLocationPattern", "{baseURL}/rsc/viewer/{derivate}/{file}", (p) => p != null);
            return ViewerFormatString(pattern, {
                baseURL : this._settings.webApplicationBaseURL,
                derivate : this._settings.derivate,
                file : file
            });
        }

        public get handlesEvents():string[] {
            var handles = new Array<string>();

            if (this._enabled) {
                handles.push(widgets.toolbar.events.ButtonPressedEvent.TYPE);
                handles.push(events.LanguageModelLoadedEvent.TYPE);
                handles.push(events.RequestPermalinkEvent.TYPE);
                handles.push(events.ImageChangedEvent.TYPE);
            } else {
                handles.push(events.ProvideToolbarModelEvent.TYPE);
            }

            return handles;
        }


    }
}
