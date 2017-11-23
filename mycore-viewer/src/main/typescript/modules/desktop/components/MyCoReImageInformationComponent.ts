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

namespace mycore.viewer.components {
    export class MyCoReImageInformationComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        public init() {
            this._informationBar = jQuery("<div></div>");
            this._informationBar.addClass("informationBar");

            this._scale = jQuery("<span></span>");
            this._scale.addClass("scale");
            this._scale.appendTo(this._informationBar);

            this._scaleEditForm = jQuery("<div class='form-group'></div>")

            this._scaleEdit = jQuery("<input type='text'>");
            this._scaleEdit.addClass("scale form-control");
            this._scaleEdit.appendTo(this._scaleEditForm);
            this.initScaleChangeLogic();

            this._informationBar.addClass("well");
            this._imageLabel = jQuery("<span></span>");
            this._imageLabel.addClass("imageLabel");
            this._imageLabel.appendTo(this._informationBar);

            this._imageLabel.mousedown(Utils.stopPropagation).mousemove(Utils.stopPropagation).mouseup(Utils.stopPropagation);

            this._rotation = jQuery("<span>0 °</span>");
            this._rotation.addClass("rotation");
            this._rotation.appendTo(this._informationBar);

            this.trigger(new events.ComponentInitializedEvent(this));
            this.trigger(new events.ShowContentEvent(this, this._informationBar, events.ShowContentEvent.DIRECTION_SOUTH, 30));
            this.trigger(new events.WaitForEvent(this, events.PageLayoutChangedEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.ImageChangedEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.ViewportInitializedEvent.TYPE));
            this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));

            // TODO: find workarround for this hack (we need to know when this._pageLayout.getCurrentPageZoom() returns the real val)
        }

        private _informationBar:JQuery;
        private _imageLabel:JQuery;
        private _rotation:JQuery;
        private _scale:JQuery;

        private _scaleEditForm:JQuery;
        private _scaleEdit:JQuery;

        private _pageLayout:widgets.canvas.PageLayout;

        private _currentZoom = -1;
        private _currentRotation = -1;

        private initScaleChangeLogic() {
            this._scale.click(()=> {
                this._scale.detach();
                this._scaleEdit.val(this._pageLayout.getCurrentPageZoom() * 100 + "");
                this._scaleEdit.appendTo(this._informationBar);
                Utils.selectElementText(this._scaleEdit.get(0));

                this._scaleEdit.keyup((ev)=> {
                    var isValid = this.validateScaleEdit();

                    if (ev.keyCode == 13) {
                        if (isValid) {
                            this.applyNewZoom();
                            this.endEdit();
                        }
                    } else if (ev.keyCode == 27) {
                        this.endEdit();
                    }
                });
            });
        }

        private endEdit() {
            this._scaleEdit.remove();
            this._scale.appendTo(this._informationBar);
        }

        private applyNewZoom() {
            var zoom = this._scaleEdit.val().trim();
            if (typeof this._pageLayout != "undefined" && this._pageLayout != null) {
                this._pageLayout.setCurrentPageZoom(zoom / 100)
            }
        }

        public validateScaleEdit() {
            var zoom = this._scaleEdit.val().trim();

            if (isNaN(zoom)) {
                this._scaleEdit.addClass("error");
                return false;
            }

            var zoomNumber = (zoom * 1);
            if(zoomNumber <50 ||  zoomNumber>400){
                this._scaleEdit.addClass("error");
                return false;
            }

            this._scaleEdit.removeClass("error");
            return true;
        }

        public get handlesEvents():string[] {
            var handles = new Array<any>();
            handles.push(events.ImageChangedEvent.TYPE);
            handles.push(events.StructureModelLoadedEvent.TYPE);
            handles.push(events.ViewportInitializedEvent.TYPE);
            handles.push(events.PageLayoutChangedEvent.TYPE);

            return handles;
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                this.updateLayoutInformation();
            }

            if (e.type == events.ViewportInitializedEvent.TYPE) {
                var vie = <events.ViewportInitializedEvent> e;

                vie.viewport.scaleProperty.addObserver({
                    propertyChanged : (oldScale:ViewerProperty<number>, newScale:ViewerProperty<number>) => {
                        this.updateLayoutInformation();
                    }
                });

                vie.viewport.rotationProperty.addObserver({
                    propertyChanged : (oldRotation:ViewerProperty<number>, newRotation:ViewerProperty<number>) => {
                        this.updateLayoutInformation();
                    }
                });

                vie.viewport.positionProperty.addObserver({
                    propertyChanged : (oldPos, newPos) => {
                        this.updateLayoutInformation();
                    }
                });

                vie.viewport.sizeProperty.addObserver({
                    propertyChanged : (oldSize, newSize) => {
                        this.updateLayoutInformation();
                    }
                });
            }

            if (e.type == events.PageLayoutChangedEvent.TYPE) {
                var plce = <events.PageLayoutChangedEvent>e;
                this._pageLayout = plce.pageLayout;
                this.updateLayoutInformation();
            }


            if (e.type == events.ImageChangedEvent.TYPE) {
                var imageChangedEvent = <events.ImageChangedEvent>e;
                if (typeof imageChangedEvent.image != "undefined" && imageChangedEvent.image != null) {
                    var text = imageChangedEvent.image.orderLabel || imageChangedEvent.image.order;
                    if (imageChangedEvent.image.uniqueIdentifier != null) {
                        text += " - " + imageChangedEvent.image.uniqueIdentifier;
                    }
                    this._imageLabel.text(text);
                }
                this.updateLayoutInformation();
            }

        }

        public updateLayoutInformation() {
            if (typeof this._pageLayout != "undefined") {
                var currentPageZoom = this._pageLayout.getCurrentPageZoom();
                if (this._currentZoom != currentPageZoom) {
                    this._scale.text(Math.round(currentPageZoom * 100) + "%");
                    this._currentZoom = currentPageZoom;
                }

                var currentPageRotation = this._pageLayout.getCurrentPageRotation();
                if (this._currentRotation != currentPageRotation) {
                    this._rotation.text(currentPageRotation + " °");
                    this._currentRotation = currentPageRotation;
                }

            }
        }

        public get container() {
            return this._informationBar;
        }
    }
}

addViewerComponent(mycore.viewer.components.MyCoReImageInformationComponent);
