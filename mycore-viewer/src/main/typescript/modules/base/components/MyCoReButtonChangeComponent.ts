namespace mycore.viewer.components {
    export class MyCoReButtonChangeComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super()
        }

        public init() {

        }

        public get handlesEvents():string[] {
            var handles = new Array<string>();

            handles.push(events.StructureModelLoadedEvent.TYPE);
            handles.push(events.ImageChangedEvent.TYPE);
            handles.push(events.ProvideToolbarModelEvent.TYPE);

            return handles;
        }

        private _nextImageButton:mycore.viewer.widgets.toolbar.ToolbarButton = null;
        private _previousImageButton:mycore.viewer.widgets.toolbar.ToolbarButton = null;
        private _structureModel:mycore.viewer.model.StructureModel = null;
        private _currentImage:mycore.viewer.model.StructureImage = null;

        private _checkAndDisableSynchronize = Utils.synchronize<MyCoReButtonChangeComponent>([
            (context:MyCoReButtonChangeComponent)=>context._nextImageButton != null,
            (context:MyCoReButtonChangeComponent)=>context._previousImageButton != null,
            (context:MyCoReButtonChangeComponent)=>context._structureModel != null,
            (context:MyCoReButtonChangeComponent)=>context._currentImage != null
        ], (context)=> {
            var positionOfImage = this._structureModel._imageList.indexOf(this._currentImage);
            if (positionOfImage == 0) {
                this._previousImageButton.disabled = true;
            } else {
                this._previousImageButton.disabled = false;
            }

            if(positionOfImage == this._structureModel.imageList.length-1){
                this._nextImageButton.disabled = true;
            } else {
                this._nextImageButton.disabled = false;
            }
        });

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent>e;
                this._nextImageButton = ptme.model._nextImageButton;
                this._previousImageButton = ptme.model._previousImageButton;
                if (this._structureModel == null) {
                    this._nextImageButton.disabled = true;
                    this._previousImageButton.disabled = true;
                }
                this._checkAndDisableSynchronize(this);
            }

            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var structureModelLoadedEvent = <events.StructureModelLoadedEvent>e;
                this._structureModel = structureModelLoadedEvent.structureModel;
                this._nextImageButton.disabled = false;
                this._previousImageButton.disabled = false;
                this._checkAndDisableSynchronize(this);
            }

            if (e.type == events.ImageChangedEvent.TYPE) {
                var imageChangedEvent = <events.ImageChangedEvent> e;
                if (imageChangedEvent.image != null) {
                    this._currentImage = imageChangedEvent.image;
                    this._checkAndDisableSynchronize(this);
                }
            }
        }


    }
}