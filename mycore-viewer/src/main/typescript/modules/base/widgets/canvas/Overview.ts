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

/// <reference path="viewport/Viewport.ts" />
/// <reference path="../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.canvas {
    export class Overview extends PageView {

        constructor(private vp:Viewport, private  _maxOverviewSize:Size2D = new Size2D(250, 250)) {
            super(true, false);
            this.container.addClass("overview");
            this.container.attr("style", "");
            this.container.css("z-index", "6");
            jQuery(this.markCanvas).detach();
            //this.updateOverviewSize(_maxOverviewSize);
        }

        private updateOverviewSize(size:Size2D) {
            size = size.roundUp();
            this.container[0].style.width = size.width + "px";
            this.container[0].style.height = size.height + "px";
            if(this.drawCanvas.width != size.width || this.drawCanvas.height != size.height
                || this.markCanvas.width != size.width
                || this.markCanvas.height != size.height) {
                this.drawCanvas.width = size.width;
                this.drawCanvas.height = size.height;
                this.markCanvas.width = size.width;
                this.markCanvas.height = size.height;
            }
            this.overviewViewport.size = size;
        }

        public overviewViewport:Viewport = new Viewport();
        private _overviewRect:Rect;

        public set overviewRect(rect:Rect) {
            this.overviewViewport.position = rect.getMiddlePoint();
            var scale = this.overviewViewport.scale = Math.min(this._maxOverviewSize.width / rect.size.width,
                    this._maxOverviewSize.height / rect.size.height);
            var toWidth = this._maxOverviewSize.width / rect.size.width == scale;
            var realSize;
            if (toWidth) {
                var relation = rect.size.width / rect.size.height;
                realSize = new Size2D(this._maxOverviewSize.width, this._maxOverviewSize.height / relation);
            } else {
                var relation = rect.size.height / rect.size.width;
                realSize = new Size2D(this._maxOverviewSize.width / relation, this._maxOverviewSize.height);
            }
            this.updateOverviewSize(realSize);

            this._overviewRect = rect;
        }


        public get overviewRect() {
            return this.overviewViewport.asRectInArea();
        }

        public drawRect() {
            var ctx = <CanvasRenderingContext2D>this.drawCanvas.getContext("2d");
            var overviewArea = this.overviewViewport.asRectInArea();
            var vpArea = this.vp.asRectInArea();
            var lineWidth = 500;

            var pos = new Position2D(vpArea.pos.x - overviewArea.pos.x, vpArea.pos.y - overviewArea.pos.y);
            pos = pos.scale(this.overviewViewport.scale);

            var vpSizeInOverview = this.vp.asRectInArea().size.scale(this.overviewViewport.scale);
            ctx.save();
            {
                ctx.lineWidth = lineWidth;
                ctx.strokeStyle = "rgba(0,0,0,0.5)";
                ctx.translate(-lineWidth / 2, -lineWidth / 2);
                ctx.strokeRect(
                    pos.x,
                    pos.y,
                    vpSizeInOverview.width + (lineWidth ),
                    vpSizeInOverview.height + (lineWidth)
                )
            }
            ctx.restore();
        }

        public initEventHandler() {
            var handler = (e:JQueryMouseEventObject)=> {
                e.preventDefault();
                var x =  (e.clientX + window.pageXOffset) - jQuery(e.target).offset().left;
                var y =  (e.clientY + window.pageYOffset) - jQuery(e.target).offset().top;
                var pos = new Position2D(x, y);
                var scaledPos = pos.scale(1 / this.overviewViewport.scale);
                var upperLeftVpPos = this.overviewViewport.asRectInArea().getPoints().upperLeft;
                var correctedPos = new Position2D(scaledPos.x + upperLeftVpPos.x, scaledPos.y + upperLeftVpPos.y);

                this.vp.position = correctedPos;
                e.stopImmediatePropagation();
            };

            jQuery(this.drawCanvas).mousedown((e)=>{
                jQuery(this.drawCanvas).bind("mousemove",handler);
            });
            jQuery(this.drawCanvas).mouseup((e)=>{
                jQuery(this.drawCanvas).unbind("mousemove",handler);
            });
            jQuery(this.drawCanvas).mouseout((e)=>{
                jQuery(this.drawCanvas).unbind("mousemove",handler);
            });

        }

    }
}



