/// <reference path="viewport/Viewport.ts" />
/// <reference path="viewport/ViewportTools.ts" />
/// <reference path="Animation.ts" />
/// <reference path="StatefulAnimation.ts" />
/// <reference path="InterpolationAnimation.ts" />
/// <reference path="model/PageArea.ts" />
/// <reference path="../../components/model/AbstractPage.ts" />
/// <reference path="../../components/model/TextContent.ts" />
/// <reference path="HtmlRenderer.ts" />
/// <reference path="TextRenderer.ts" />
/// <reference path="CanvasPageLayer.ts" />
/// <reference path="PageView.ts" />
/// <reference path="Scrollbar.ts" />
/// <reference path="Overview.ts" />

namespace mycore.viewer.widgets.canvas {
    export class PageController {

        constructor(private miniOverview:boolean = false) {
            this._pageArea.updateCallback = () => {
                this.update();
            };

            this._updateSizeIfChanged();
            this._registerViewport();
        }

        private _lastSize:Size2D = new Size2D(0, 0);
        private _requestRunning:boolean = false;
        private _nextRequested:boolean = false;
        private _pageArea:PageArea = new PageArea();
        private _viewport:Viewport = new Viewport();
        private _views:Array<PageView> = new Array<PageView>();
        private _viewHTMLRendererMap = new MyCoReMap<PageView, HtmlRenderer>();
        private _textRenderer:TextRenderer = null;
        private _canvasPageLayers = new MyCoReMap<number, CanvasPageLayer>();
        private _lastAnimationTime:number = null;
        private _animations = new Array<Animation>();

        public _overview:Overview = null;

        public _updateSizeIfChanged() {
            this._views.forEach(view => {
                var retinaWidth = view.container.width() * (window.devicePixelRatio || 1);
                var retinaHeight = view.container.height() * (window.devicePixelRatio || 1);
                if (view.drawCanvas.width != retinaWidth
                    || view.drawCanvas.height != retinaHeight
                    || view.markCanvas.width != retinaWidth
                    || view.markCanvas.height != retinaHeight) {
                    this._updateSize(view);
                }
            });

        }

        private _updateSize(view:PageView) {
            view.drawCanvas.width = view.container.width() * (window.devicePixelRatio || 1);
            view.drawCanvas.height = view.container.height() * (window.devicePixelRatio || 1);
            view.markCanvas.width = view.container.width() * (window.devicePixelRatio || 1);
            view.markCanvas.height = view.container.height() * (window.devicePixelRatio || 1);
            this._lastSize = new Size2D(view.drawCanvas.width, view.drawCanvas.height);
            this._viewport.size = new Size2D(view.drawCanvas.width, view.drawCanvas.height);
        }

        private _unregisterViewport() {
            if (this._viewport != null) {
                this._viewport.sizeProperty.removeAllObserver();
                this._viewport.positionProperty.removeAllObserver();
                this._viewport.scaleProperty.removeAllObserver();
                this._viewport.rotationProperty.removeAllObserver();
            }
        }

        private _registerViewport() {
            var updater = {
                propertyChanged: (_old:ViewerProperty<any>, _new:ViewerProperty<any>) => {
                    this.update();
                }
            };

            this._viewport.sizeProperty.addObserver(updater);
            this._viewport.positionProperty.addObserver(updater);
            this._viewport.scaleProperty.addObserver(updater);
            this._viewport.rotationProperty.addObserver(updater);
        }

        public update():void {
            if (!this._nextRequested) {
                this._nextRequested = true;
                if (!this._requestRunning) {
                    this._requestRunning = true;
                    viewerRequestAnimationFrame(() => {
                        this._nextRequested = false;
                        this._updateSizeIfChanged();
                        this.updateAnimations();
                        this._views.forEach(view=> {
                            if (view.drawHTML) {
                                var htmlRenderer:HtmlRenderer;
                                if (this._viewHTMLRendererMap.has(view)) {
                                    htmlRenderer = this._viewHTMLRendererMap.get(view);
                                } else {
                                    htmlRenderer = new HtmlRenderer(this._viewport, this._pageArea, view);
                                    this._viewHTMLRendererMap.set(view, htmlRenderer);
                                }
                                htmlRenderer.update();
                            }
                            this.drawOnView(view, this.viewport, !view.drawImage);
                        });

                        if(this._textRenderer != null){
                            this._textRenderer.update();
                        }

                        if (this._overview != null) {
                            this.drawOnView(this._overview, this._overview.overviewViewport);
                            this._overview.drawRect();
                        }

                        //this._ocrRenderer.update();
                    });
                    this._requestRunning = false;
                }
            }
        }


        private drawOnView(view:mycore.viewer.widgets.canvas.PageView, vp:Viewport = this._viewport, markerOnly:boolean = false):void {
            if (view != null && vp != null) {
                view.drawCanvas.width = view.drawCanvas.width;
                view.markCanvas.width = view.markCanvas.width;

                var rotatedViewportSize = vp.size.getRotated(vp.rotation);
                var ctx1 = <CanvasRenderingContext2D>view.drawCanvas.getContext("2d");
                var ctx2 = <CanvasRenderingContext2D>view.markCanvas.getContext("2d");

                ctx1.save();
                ctx2.save();
                {
                    {
                        ctx1.translate(vp.size.width / 2, vp.size.height / 2);
                        ctx1.rotate(vp.rotation * Math.PI / 180);
                        ctx1.translate(-rotatedViewportSize.width / 2, -rotatedViewportSize.height / 2);
                    }
                    {
                        ctx2.translate(vp.size.width / 2, vp.size.height / 2);
                        ctx2.rotate(vp.rotation * Math.PI / 180);
                        ctx2.translate(-rotatedViewportSize.width / 2, -rotatedViewportSize.height / 2);
                    }
                    this._pageArea.getPagesInViewport(vp).forEach((page:model.AbstractPage) => {
                        this.drawPage(page, this._pageArea.getPageInformation(page), vp.asRectInArea(), vp.scale, vp != this._viewport, view, markerOnly);
                    });
                }
                ctx1.restore();
                ctx2.restore();
            }
        }

        public drawPage(page:model.AbstractPage, info:PageAreaInformation, areaInViewport:Rect, scale:number, preview:boolean, view:mycore.viewer.widgets.canvas.PageView, markerOnly:boolean = false) {
            var realPageDimension = page.size.getRotated(info.rotation).scale(info.scale);
            var pageRect = new Rect(new Position2D(
                        info.position.x - (realPageDimension.width / 2),
                        info.position.y - (realPageDimension.height / 2)),
                realPageDimension);

            // This is the area of the page wich will be drawed in absolute coordinates 
            var pagePartInArea = areaInViewport.getIntersection(pageRect);

            /* 
             * This is the area of the page wich will be drawed in relative coordinates(to the page)
             * If the page is not rotatet its okay, but if the page is rotatet 90deg we need to rotate it back
             */
            var pagePartInPageRotatet = new Rect(new Position2D(Math.max(0, pagePartInArea.pos.x - pageRect.pos.x)
                , Math.max(0, pagePartInArea.pos.y - pageRect.pos.y)), pagePartInArea.size);

            var rotateBack = (deg:number, pagePartBefore:Rect, realPageDimension:Size2D) => {
                if (deg == 0) {
                    return pagePartBefore;
                }
                var newPositionX = pagePartBefore.pos.y;
                // we use realPageDimension.width instead of height because realPageDimension is the rotated value
                var newPositionY = realPageDimension.width - pagePartBefore.pos.x - pagePartBefore.size.width;
                var rect = new Rect(new Position2D(newPositionX, newPositionY), pagePartBefore.size.getRotated(90));
                if (deg == 90) {
                    return rect;
                } else {
                    return rotateBack(deg - 90, rect, realPageDimension.getRotated(90));
                }
            };

            var pagePartInPage = rotateBack(info.rotation, pagePartInPageRotatet, realPageDimension);
            var notRotated = page.size.scale(info.scale);
            var ctx1 = <CanvasRenderingContext2D> view.drawCanvas.getContext("2d");
            var ctx2 = <CanvasRenderingContext2D> view.markCanvas.getContext("2d");
            ctx1.save();
            ctx2.save();
            {
                {
                    ctx1.translate(Math.floor((-areaInViewport.pos.x + info.position.x) * scale), Math.floor((-areaInViewport.pos.y + info.position.y) * scale));
                    ctx1.rotate(info.rotation * Math.PI / 180);
                    ctx1.translate(Math.floor(((-notRotated.width / 2) + pagePartInPage.pos.x) * scale), Math.floor(((-notRotated.height / 2) + pagePartInPage.pos.y) * scale));

                    ctx2.translate(Math.floor((-areaInViewport.pos.x + info.position.x) * scale), Math.floor(-areaInViewport.pos.y + info.position.y) * scale);
                    ctx2.rotate(info.rotation * Math.PI / 180);
                    ctx2.translate((Math.floor(-notRotated.width / 2) + pagePartInPage.pos.x) * scale, Math.floor((-notRotated.height / 2) + pagePartInPage.pos.y) * scale);
                }

                var realAreaToDraw = pagePartInPage.scale(1 / info.scale);
                realAreaToDraw = new Rect(realAreaToDraw.pos.roundDown(), realAreaToDraw.size.roundDown());

                if (!markerOnly) {
                    page.draw(ctx1, realAreaToDraw, scale * info.scale, preview);
                }

                ctx2.translate(-realAreaToDraw.pos.x * scale * info.scale, -realAreaToDraw.pos.y * scale * info.scale);
                if(!preview){
                    ctx2.scale(scale * info.scale, scale * info.scale);
                    var layers:Array<CanvasPageLayer> = this.getCanvasPageLayersOrdered();
                    layers.forEach(layer => {
                        layer.draw(ctx2, page.id, page.size, view.drawHTML);
                    });
                    ctx2.scale(1 / scale * info.scale, 1 / scale * info.scale);
                }
            }
            ctx1.restore();
            ctx2.restore();
        }

        private updateAnimations() {
            this._viewport.updateAnimation();
            if(this._animations.length == 0) {
                this._lastAnimationTime = null;
                return;
            }
            if(this._lastAnimationTime == null) {
                this._lastAnimationTime = new Date().valueOf();
            }
            var elapsedTime:number = new Date().valueOf() - this._lastAnimationTime;
            var finishedAnimations:Array<Animation> = [];
            for(var animation of this._animations) {
                if(animation.updateAnimation(elapsedTime)) {
                    finishedAnimations.push(animation);
                }
            }
            finishedAnimations.forEach(animation => {
                this.removeAnimation(animation);
            });
            if(this._animations.length == 0) {
                this._lastAnimationTime = null;
                return;
            }
            setTimeout(() => {
                this.update();
            }, 0);   
        }

        public addAnimation(animation:Animation) {
            this._animations.push(animation);
        }

        public removeAnimation(animation:Animation) {
            var index = this._animations.indexOf(animation);
            if (index >= 0) {
                this._animations.splice( index, 1 );
            }
        }

        public get viewport():mycore.viewer.widgets.canvas.Viewport {
            return this._viewport;
        }


        public get views():Array<mycore.viewer.widgets.canvas.PageView> {
            return this._views;
        }

        public addPage(page:model.AbstractPage, info:PageAreaInformation):void {
            page.refreshCallback = () => {
                this.update();
            };
            this._pageArea.addPage(page, info);
        }

        public removePage(page:model.AbstractPage):void {
            page.refreshCallback = ()=> {
            };
            page.clear();
            this._pageArea.removePage(page);
        }


        public getPages():Array<model.AbstractPage> {
            return this._pageArea.getPages();
        }

        public setPageAreaInformation(page:model.AbstractPage, info:PageAreaInformation):void {
            this._pageArea.setPageAreaInformation(page, info);
        }

        public getPageAreaInformation(page:model.AbstractPage) {
            return this._pageArea.getPageInformation(page);
        }

        public addCanvasPageLayer(zIndex:number, canvas:CanvasPageLayer) {
            this._canvasPageLayers.set(zIndex, canvas);
        }

        public getCanvasPageLayers():MyCoReMap<number, CanvasPageLayer> {
            return this._canvasPageLayers;
        }

        public getCanvasPageLayersOrdered():Array<CanvasPageLayer> {
            if(this._canvasPageLayers == null || this._canvasPageLayers.isEmpty()) {
                return [];
            }
            var sortedArray:Array<CanvasPageLayer> = [];
            this._canvasPageLayers.keys.sort().forEach(k => {
                sortedArray.push(this._canvasPageLayers.get(k));
            });
            return sortedArray;
        }

        public getPageArea():widgets.canvas.PageArea {
            return this._pageArea;
        }


        get textRenderer():mycore.viewer.widgets.canvas.TextRenderer {
            return this._textRenderer;
        }

        set textRenderer(value:mycore.viewer.widgets.canvas.TextRenderer) {
            this._textRenderer = value;
        }
    }


}



