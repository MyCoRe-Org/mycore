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

namespace mycore.viewer.widgets.imagebar {

    export class ImagebarView {
        constructor(__container:JQuery, private _imageSelectedCallback:(position:number, hover:boolean)=>void) {
            this._idElementMap = new MyCoReMap<string, JQuery>();
            this._lastSelectedId = "";
            this._container = jQuery("<div></div>");
            this._container.addClass("imagebar");
            __container.append(this._container);
            this.registerEvents();
        }

        private _idElementMap:MyCoReMap<string, JQuery>;
        private _lastSelectedId:string;
        private _container:JQuery;

        private registerEvents() {
            var that = this;
            var down: boolean = false;
            var lastX = 0;

            // TODO: fix bug z-index change no events
            var end = (e) => {
                down = false;
                var x = lastX;
                that._imageSelectedCallback(x, false);
                that._container.removeClass("selecting");
                e.preventDefault();
                jQuery(window.document.body).unbind("touchmove", move);
                jQuery(window.document.body).unbind("touchend", end);
            };

            var move = (e) => {
                if(down) {
                    var x = (<any>e.originalEvent).targetTouches[0].pageX;
                    that._imageSelectedCallback(x, true);
                    lastX = x;
                }
                e.preventDefault();
            };

            var start = (e)=> {
                lastX = (<any>e.originalEvent).targetTouches[0].pageX;
                down = true;
                that._container.addClass("selecting");
                e.preventDefault();

                jQuery(window.document.body).bind("touchmove", move);
                jQuery(window.document.body).bind("touchend", end);
            };


            this._container.bind("touchstart", start);

        }

        public addImage(id:string, url:string, position:number):void {
            var element = jQuery("<img />");
            element.attr("data-id", id);
            element.attr("src", url);
            element.addClass("miniImage");
            if(this.viewportWidth / 2 > position) {
                element.css({"left": position});
            } else {
                var positionFromRight = this.viewportWidth - position - 18;
                element.css({"right": positionFromRight });
            }

            this._container.append(element);
            this._idElementMap.set(id, element);

        }

        public removeImage(id:string):void {
            var imgElement = this._idElementMap.get(id);
            if(typeof imgElement != "undefined") {
                imgElement.attr("src", "");
                imgElement.remove();
            }

        }

        public setSelectedImage(id:string, url:string, pos:number):void {
            if (this._lastSelectedId != "") {
                this.removeImage(this._lastSelectedId);
            }

            this.addImage(id, url, pos);
            this._idElementMap.get(id).addClass("selected");
            this._lastSelectedId = id;
        }

        public get viewportWidth() {
            return this._container.width();
        }

        public removeAllImages() {
            var that = this;
            this._idElementMap.forEach((k:string, v:JQuery) => {
                that.removeImage(k);
            });
            this._idElementMap.clear();
        }

        public get containerElement() {
            return this._container;
        }


    }
}
