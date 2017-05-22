/// <reference path="../widgets/alto/AltoEditorWidget.ts" />

namespace mycore.viewer.components {

    import WaitForEvent = mycore.viewer.components.events.WaitForEvent;
    import ShowContentEvent = mycore.viewer.components.events.ShowContentEvent;
    import AltoEditorWidget = mycore.viewer.widgets.alto.AltoEditorWidget;
    import TileImagePage = mycore.viewer.widgets.canvas.TileImagePage;
    export class MyCoReAltoEditorComponent extends ViewerComponent {
        private _structureImages: Array<mycore.viewer.model.StructureImage>;
        private _structureModel: mycore.viewer.model.StructureModel;
        private _altoPresent: boolean;
        private _languageModel: mycore.viewer.model.LanguageModel;
        private _toolbarModel: mycore.viewer.model.MyCoReBasicToolbarModel;
        private _sidebarControllDropdownButton: mycore.viewer.widgets.toolbar.ToolbarDropdownButton;
        private _altoDropdownChildItem: { id: string; label: string };
        private container: JQuery;
        private containerTitle: JQuery;
        private editorWidget: mycore.viewer.widgets.alto.AltoEditorWidget;
        private static DROP_DOWN_CHILD_ID = "altoButtonChild";


        constructor(private _settings: MyCoReViewerSettings, private _container: JQuery) {
            super();
            console.log(this._container);
        }

        private everythingLoadedSynchronize = Utils.synchronize<MyCoReAltoEditorComponent>(
            [
                (obj) => obj._toolbarModel!= null,
                (obj) => obj._languageModel != null,
            ],
            (obj) => {
                obj.completeLoaded();
            }
        );

        public init() {
            this.container = jQuery("<div></div>");
            this.containerTitle = jQuery("<span>ALTO-Editor</span>");

            this.trigger(new WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
        }


        private _containerTitle = this.containerTitle;

        public handle(e: mycore.viewer.widgets.events.ViewerEvent): void {
            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                let structureModelLodedEvent = <events.StructureModelLoadedEvent>e;
                this._structureModel = structureModelLodedEvent.structureModel;
                this._structureImages = this._structureModel.imageList;
                for (let imageIndex in this._structureImages) {
                    let image = this._structureImages[ imageIndex ];
                    this._altoPresent = this._altoPresent || image.additionalHrefs.has("AltoHref");
                }
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                let lmle = <events.LanguageModelLoadedEvent>e;
                this._languageModel = lmle.languageModel;
                this.everythingLoadedSynchronize(this);
            }

            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                let ptme = <events.ProvideToolbarModelEvent>e;
                this._toolbarModel = ptme.model;
                this._sidebarControllDropdownButton = ptme.model._sidebarControllDropdownButton;
                this.everythingLoadedSynchronize(this);
            }

            if (e.type == widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
                let dbpe = <widgets.toolbar.events.DropdownButtonPressedEvent> e;

                if (dbpe.childId === MyCoReAltoEditorComponent.DROP_DOWN_CHILD_ID) {
                    this.trigger(new ShowContentEvent(this, this.container, ShowContentEvent.DIRECTION_WEST, 400, this._containerTitle));

                } else {
                    // disable all tools here!
                }

            }

            if(e.type ==  events.ImageChangedEvent.TYPE){
                let ice = < events.ImageChangedEvent>e;

                if(typeof ice.image != "undefined"){
                    this.trigger(new events.RequestPageEvent(this, ice.image.href, (pageId:string, abstractPage:model.AbstractPage)=>{
                        let altoContent = (<TileImagePage>abstractPage).getHTMLContent();
                        console.log(altoContent);
                    }));
                }
            }


        }

        public get handlesEvents(): string[] {
            return [
                events.StructureModelLoadedEvent.TYPE,
                events.LanguageModelLoadedEvent.TYPE,
                events.ProvideToolbarModelEvent.TYPE,
                widgets.toolbar.events.DropdownButtonPressedEvent.TYPE,
                events.ImageChangedEvent.TYPE
            ];
        }



        private completeLoaded() {
            this._altoDropdownChildItem = {
                id : MyCoReAltoEditorComponent.DROP_DOWN_CHILD_ID,
                label : this._languageModel.getTranslation("altoEditor")
            };
            this._sidebarControllDropdownButton.children.push(this._altoDropdownChildItem);
            this._sidebarControllDropdownButton.children = this._sidebarControllDropdownButton.children;
            this.editorWidget = new AltoEditorWidget(this.container, this._languageModel);
            this._containerTitle = jQuery(`<span>${this._languageModel.getTranslation("containerTitle")}</span>`);
        }
    }
}

addViewerComponent(mycore.viewer.components.MyCoReAltoEditorComponent);
