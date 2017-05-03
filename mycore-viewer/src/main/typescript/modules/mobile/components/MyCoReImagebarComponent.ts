/// <reference path="../widgets/imagebar/IviewImagebar.ts" />
/// <reference path="../widgets/imagebar/ImagebarModel.ts" />


namespace mycore.viewer.components {
    export class MyCoReImagebarComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings, private _container:JQuery) {
            super();
            if(this._settings.mobile){
                this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
            }
        }

        private _imagebar:widgets.imagebar.IviewImagebar;
        private _model : Array<model.StructureImage>;

        public _init(imageList:Array<model.StructureImage>) {
            // need to find the object for the start image
            var imagebarStartImage:mycore.viewer.widgets.imagebar.ImagebarImage;
            var startImage = this._settings.filePath;
            if(startImage.charAt(0) == "/" ){
                startImage = startImage.substr(1);
            }
            for (var i in imageList) {
                var image:mycore.viewer.widgets.imagebar.ImagebarImage;
                image = <mycore.viewer.widgets.imagebar.ImagebarImage>imageList[i];
                if (image.href == startImage) {
                    imagebarStartImage = image;
                }
            }

            var that = this;
            this._imagebar = new mycore.viewer.widgets.imagebar.IviewImagebar(this._container, <Array<mycore.viewer.widgets.imagebar.ImagebarImage>>imageList, imagebarStartImage, (img:mycore.viewer.widgets.imagebar.ImagebarImage)=> {
                this.trigger(new events.ImageSelectedEvent(that, <model.StructureImage>img));
            }, this._settings.tileProviderPath + this._settings.derivate + "/");

            this.trigger(new events.ComponentInitializedEvent(this));
        }

        public get content() {
            return this._container;
        }

        public get handlesEvents():string[] {
            if(this._settings.mobile) {
                return [events.StructureModelLoadedEvent.TYPE, events.ImageChangedEvent.TYPE, events.CanvasTapedEvent.TYPE];
            } else {
                return [];
            }
        }

        /// TODO: jump to the right image
        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var imageList = <Array<model.StructureImage>>(<events.StructureModelLoadedEvent>e).structureModel._imageList;
                this._model = imageList;
                var convertedImageList:Array<model.StructureImage> = imageList;
                this._init(convertedImageList);
            }

            if (e.type == events.ImageChangedEvent.TYPE) {
                var imageChangedEvent = <events.ImageChangedEvent>e;
                if(typeof this._imagebar != "undefined") {
                    this._imagebar.setImageSelected((<model.StructureImage>imageChangedEvent.image));
                }
            }

            if(e.type == events.CanvasTapedEvent.TYPE) {
                this._imagebar.view.slideToggle();
            }

        }

    }
}

addViewerComponent(mycore.viewer.components.MyCoReImagebarComponent);