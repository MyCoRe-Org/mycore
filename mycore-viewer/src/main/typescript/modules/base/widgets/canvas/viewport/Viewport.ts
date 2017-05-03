/// <reference path="../../../Utils.ts" />
/// <reference path="../Animation.ts" />

namespace mycore.viewer.widgets.canvas {

    export class Viewport {

        constructor() {
            this.positionProperty = new ViewerProperty<Position2D>(this, "_position", new Position2D(0, 0));
            this.sizeProperty = new ViewerProperty<Size2D>(this, "_position", new Size2D(0, 0));
            this.rotationProperty = new ViewerProperty<number>(this, "_rotation", 0);
            this.scaleProperty = new ViewerProperty<number>(this, "_scale", 1);
        }

        private _currentAnimation:Animation = null;
        private _lastAnimTime:number = -1;
        public positionProperty:ViewerProperty<Position2D>;
        public sizeProperty:ViewerProperty<Size2D>;
        public rotationProperty:ViewerProperty<number>
        public scaleProperty:ViewerProperty<number>;

        get scale():number {
            return this.scaleProperty.value;
        }

        get rotation():number {
            return this.rotationProperty.value;
        }

        get size():Size2D {
            return this.sizeProperty.value;
        }

        get position():Position2D {
            return this.positionProperty.value;
        }

        set scale(val:number) {
            if(val == 0 || typeof val == "undefined" || val == null || isNaN(val)) {
                throw new ViewerError("The scale of viewport is not valid!", val);
            }
            this.scaleProperty.value = val;
        }

        set rotation(val:number) {
            if(val != 0 && val != 90 && val != 180 && val != 270) {
                throw new ViewerError("The rotation of viewport is not valid!", val);
            }
            this.rotationProperty.value = val;
        }

        set size(val:Size2D) {
            if(typeof val == "undefined" || val == null || isNaN(val.width) || isNaN(val.height)) {
                throw new ViewerError("The size of viewport is not valid!", val);
            }
            this.sizeProperty.value = val;
        }

        set position(val:Position2D) {
             if(typeof val == "undefined" || val == null || isNaN(val.x) || isNaN(val.y)) {
                throw new ViewerError("The position of viewport is not valid!", val);
            }
            this.positionProperty.value = val;
        }

        asRectInArea():Rect {
            var realSize = this.size.getRotated(this.rotation).scale(1 / this.scale);
            return new Rect(new Position2D(this.position.x - (realSize.width / 2), this.position.y - (realSize.height / 2)), realSize);
        }

        public startAnimation(anim:Animation) {
            this._currentAnimation = anim;
            this._lastAnimTime = new Date().valueOf();
            this.scale = this.scale;
        }

        public getAbsolutePosition(positionInViewport:Position2D):Position2D {
            var rectPoints = this.asRectInArea().getPoints();
            var upperLeftPosition:Position2D = rectPoints.upperLeft;
            switch (this.rotation){
                case 90: upperLeftPosition = rectPoints.lowerLeft; break;
                case 180: upperLeftPosition= rectPoints.lowerRight; break;
                case 270: upperLeftPosition= rectPoints.upperRight; break;
            }

            var scaledPositionInViewport = positionInViewport.scale(1 / this.scale).rotate(this.rotation);

            return new Position2D(upperLeftPosition.x + scaledPositionInViewport.x, upperLeftPosition.y + scaledPositionInViewport.y);
        }

        public stopAnimation() {
            this._currentAnimation = null;
        }

        public updateAnimation():void {
            if (this._currentAnimation != null) {
                var currentAnimTime = new Date().valueOf();

                if (this._currentAnimation.updateAnimation(currentAnimTime - this._lastAnimTime)) {
                    this._currentAnimation = null;
                } else {
                    this._lastAnimTime = new Date().valueOf();
                }
            }
        }

        get currentAnimation():mycore.viewer.widgets.canvas.Animation {
            return this._currentAnimation;
        }
        
        public setRect(rect:Rect):void {
            if(this.size.width > 0 && this.size.height > 0 ) {
                this.scale = Math.min(this.size.width / rect.size.width, this.size.height / rect.size.height);
            }
            this.position = rect.getMiddlePoint();
        }

    }


}