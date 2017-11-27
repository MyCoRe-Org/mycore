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

/// <reference path="ImagebarModel.ts" />
/// <reference path="ImagebarView.ts" />

namespace mycore.viewer.widgets.imagebar {

    export class IviewImagebar {
        constructor(private _container:JQuery, images:Array<ImagebarImage >, startImage:ImagebarImage, imageSelected:(img:ImagebarImage)=>void, private _urlPrefix:string) {
            this._model = new mycore.viewer.widgets.imagebar.ImagebarModel(images, startImage);
            var that = this;

            this._view = new mycore.viewer.widgets.imagebar.ImagebarView(_container, (position:number, hover:boolean) => {
                if (!hover) {
                    imageSelected(that.getImageForPosition(position));
                } else {
                    that._imageSelected(position);
                }
            });
            this.insertImages();
            var that = this;
            jQuery(window).bind("resize", ()=> {
                that.insertImages();
                that.setImageSelected(that._model.selected);
            });

            this.setImageSelected(startImage);
        }

        private _model:ImagebarModel;
        private _view:ImagebarView;

        private static IMAGE_WIDTH = 20;

        private insertImages() {
            this._view.removeAllImages();

            var imageCount = this._model.images.length;
            var space = this._view.viewportWidth;
            var displayImageCount = Math.floor(space / IviewImagebar.IMAGE_WIDTH);
            var gap = 0;
            if (displayImageCount > imageCount) {
                displayImageCount = imageCount;
                gap = (space / displayImageCount / 2);
            }


            var nthImageToDisplay = Math.max(Math.floor(imageCount / displayImageCount), 1);
            for (var i = 0; i < displayImageCount; i++) {
                var image = this._model.images[i * nthImageToDisplay];
                this.addImage(image, space / displayImageCount * i + gap);

            }
        }

        private addImage(image:ImagebarImage, position) {
            image.requestImgdataUrl((path)=> {
                this._view.addImage(image.id, path, position);
            });
        }

        private getImageForPosition(position:number) {
            var imageCount = this._model.images.length;
            var space = this._view.viewportWidth;

            var selectedImageIndex = Math.floor(position / (space / imageCount));
            var selectedImage = this._model.images[selectedImageIndex];
            return selectedImage;
        }


        private getPositionOfImage(image:ImagebarImage) {
            var pos = this._model.images.indexOf(image);
            var imageCount = this._model.images.length;
            var space = this._view.viewportWidth;

            var position = space / imageCount * pos;
            return position;
        }

        private _imageSelected(position:number) {
            var selectedImage = this.getImageForPosition(position);

            this._model._lastPosition = position;
            var that = this;
            if (typeof selectedImage != "undefined") {
                selectedImage.requestImgdataUrl((path)=> {
                    if(that._model._lastPosition == position){
                        this._view.setSelectedImage(selectedImage.id, path, position);
                    }
                });
            }

        }

        public setImageSelected(image:ImagebarImage) {
            this._model.selected = image;
            this._imageSelected(this.getPositionOfImage(image));
        }

        public get view() {
            return this._view.containerElement;
        }
    }
}
