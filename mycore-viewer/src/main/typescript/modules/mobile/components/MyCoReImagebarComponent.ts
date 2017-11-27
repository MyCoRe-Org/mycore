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
