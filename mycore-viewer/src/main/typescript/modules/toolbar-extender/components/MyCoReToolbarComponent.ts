namespace mycore.viewer.components {

    import ToolbarButton = mycore.viewer.widgets.toolbar.ToolbarButton;
    import ToolbarGroup = mycore.viewer.widgets.toolbar.ToolbarGroup;

    export interface ToolbarExtenderEntry {
        id: string;
        type: string; /* button, group */
        label?: string;
        icon?: string;
        href?: string;
        tooltip?: string;
        action?: ()=>void;
        inGroup?: string;
    }

    export interface ToolbarExtenderSettings extends MyCoReViewerSettings {
        toolbar: ToolbarExtenderEntry[];
    }

    export class MyCoReToolbarExtenderComponent extends ViewerComponent {
        private idEntryMapping = new MyCoReMap<string, ToolbarExtenderEntry>();
        private idButtonMapping = new MyCoReMap<string, ToolbarButton>();
        private idGroupMapping = new MyCoReMap<string, ToolbarGroup>();
        constructor(private _settings: ToolbarExtenderSettings) {
            super();
        }

        private toolbarModel: model.MyCoReBasicToolbarModel = null;
        private languageModel: model.LanguageModel = null;
        private toolbarButtonSync = Utils.synchronize<MyCoReToolbarExtenderComponent>([ me=>me.languageModel != null,
                me=>me.toolbarModel != null ]
            , me=>me.initLanguage());


        public init() {
            if ("toolbar" in this._settings) {
                this._settings.toolbar.forEach((settingsEntry: ToolbarExtenderEntry)=> {
                    if (settingsEntry.type == "group") {
                        this.idGroupMapping.set(settingsEntry.id, new ToolbarGroup(settingsEntry.id));

                    } else if (settingsEntry.type == "button") {
                        this.idButtonMapping.set(settingsEntry.id, new ToolbarButton(settingsEntry.id, settingsEntry.label, settingsEntry.tooltip || settingsEntry.label, settingsEntry.icon));
                    }

                    this.idEntryMapping.set(settingsEntry.id, settingsEntry);
                });
            }

            this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
            this.trigger(new events.ComponentInitializedEvent(this));
        }

        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent>e;
                this.toolbarModel = ptme.model;

                this.idGroupMapping.forEach((k,v)=>{
                    ptme.model.addGroup(this.idGroupMapping.get(k));
                });

                this.idButtonMapping.forEach((k, v)=> {
                    var inGroup = this.idEntryMapping.get(k).inGroup;
                    let group: ToolbarGroup = ptme.model.getGroups().filter(g=> inGroup == g.name)[ 0 ];

                    if (typeof group == "undefined") {
                        console.log("Can not find group " + inGroup + "!");
                        return;
                    }

                    group.addComponent(v);
                });

                this.toolbarButtonSync(this);
            }

            if (e.type == widgets.toolbar.events.ButtonPressedEvent.TYPE) {
                let pressedID = <widgets.toolbar.events.ButtonPressedEvent>e;
                this.idEntryMapping.hasThen(pressedID.button.id, (entry)=> {
                    if ("action" in entry) {
                        entry.action.apply(this, e);
                    }

                    if ("href" in entry) {
                        window.location.href = entry.href;
                    }
                });
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                this.languageModel = (<events.LanguageModelLoadedEvent>e).languageModel;
                this.toolbarButtonSync(this);
            }


        }

        public get handlesEvents(): string[] {
            return [ events.ProvideToolbarModelEvent.TYPE, widgets.toolbar.events.ButtonPressedEvent.TYPE, events.LanguageModelLoadedEvent.TYPE ];
        }

        public initLanguage() {
            this.idButtonMapping.forEach((k, v)=> {
                v.label = this.languageModel.getTranslation(v.label);
                v.tooltip = this.languageModel.getTranslation(v.tooltip);
            });
        }
    }
}

addViewerComponent(mycore.viewer.components.MyCoReToolbarExtenderComponent);
