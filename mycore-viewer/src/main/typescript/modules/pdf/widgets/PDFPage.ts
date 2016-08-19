/// <reference path="../definitions/pdf.d.ts" />
module mycore.viewer.widgets.canvas {
    export class PDFPage implements model.AbstractPage {

        constructor(public id, private _pdfPage:PDFPageProxy) {
            var width = this._pdfPage.view[ 2 ] - this._pdfPage.view[ 0 ];
            var height = this._pdfPage.view[ 3 ] - this._pdfPage.view[ 1 ];

            var pageRotation = (<any>_pdfPage).pageInfo.rotate;
            if (pageRotation == 90 || pageRotation == 270) {
                width = width ^ height;
                height = height ^ width;
                width = width ^ height;
            }

            this._rotation = pageRotation;
            this.size = new Size2D(width, height);
        }

        public refreshCallback:() => void;
        public size:Size2D;

        private _rotation:number;
        private _frontBuffer:HTMLCanvasElement = document.createElement("canvas");
        private _backBuffer:HTMLCanvasElement = document.createElement("canvas");

        private _bbScale:number = -1;
        private _fbScale:number = -1;
        private _promiseRunning:boolean = false;
        private _textData:model.TextContentModel = null;

        public resolveTextContent(callback:(content:model.TextContentModel)=>void):void {
            if (this._textData == null) {
                var textContent:model.TextContentModel = {
                    content : []
                };
                this._pdfPage.getTextContent().then((textData:PDFPageTextData)=> {
                    textData.items.forEach((e)=> {

                        var vp = (<any>this._pdfPage.getViewport(1));
                        var transform = (<any>PDFJS).Util.transform(vp.transform, e.transform);

                        var style = textData.styles[ e.fontName ];
                        var angle = Math.atan2(transform[ 1 ], transform[ 0 ]) + ((style.vertical == true ) ? Math.PI / 2 : 0);
                        var fontHeight = Math.sqrt((transform[ 2 ] * transform[ 2 ]) + (transform[ 3 ] * transform[ 3 ]));
                        var fontAscent = fontHeight;

                        if (style.ascent) {
                            fontAscent = style.ascent * fontAscent;
                        } else if (style.descent) {
                            fontAscent = (1 + style.descent) * fontAscent;
                        }

                        var x;
                        var y;

                            x = transform[ 4 ];
                            y = transform[ 5 ];


                        var textElement = new PDFTextElement(angle, new Size2D(e.width, e.height), fontHeight, e.str, new Position2D(x, y), style.fontFamily, this.id);
                        textContent.content.push(textElement);
                    });
                    this._textData = textContent;
                    callback(textContent);
                }, (reason:string) => {
                    console.error("PDF Page Text Content rejected");
                    console.error("Reason: " + reason);
                });
            } else {
                callback(this._textData);
            }


        }

        public draw(ctx:CanvasRenderingContext2D, rect:Rect, sourceScale):void {
            if (!this._promiseRunning && sourceScale > this._fbScale) {
                this._updateBackBuffer(sourceScale);
            }

            if (this._fbScale == -1) {
                return;
            }


            var scaledRect = rect.scale(this._fbScale);
            var sourceScaleRect = rect.scale(sourceScale);

            var sw = Math.floor(scaledRect.size.width);
            var sh = Math.floor(scaledRect.size.height);

            if (sw > 0 && sh > 0) {
                ctx.save();
                {
                    ctx.drawImage(this._frontBuffer, scaledRect.pos.x, scaledRect.pos.y, Math.min(sw, this._frontBuffer.width), Math.min(sh, this._frontBuffer.height), 0, 0, sourceScaleRect.size.width, sourceScaleRect.size.height)
                }
                ctx.restore();
            }

        }

        private _updateBackBuffer(newScale) {
            var vp = this._pdfPage.getViewport(newScale, this._rotation);
            var task = <any> this._pdfPage.render(<PDFRenderParams>{
                canvasContext : <CanvasRenderingContext2D>this._backBuffer.getContext('2d'),
                viewport : vp
            });

            this._bbScale = newScale;
            this._promiseRunning = true;

            this._backBuffer.width = this.size.width * newScale;
            this._backBuffer.height = this.size.height * newScale;

            var resolve = (page:PDFPageProxy) => {
                this._promiseRunning = false;
                this._swapBuffers();
                this.refreshCallback();
            };

            var error = (err) => {
                console.log("Render Error", err);
            };

            task.promise.then(resolve, error);
        }


        private _swapBuffers() {
            var swap:any = null;

            swap = this._backBuffer;
            this._backBuffer = this._frontBuffer;
            this._frontBuffer = swap;

            swap = this._bbScale;
            this._bbScale = this._fbScale;
            this._fbScale = swap;
        }

        toString():string {
            return <any>this._pdfPage.pageNumber;
        }

        public clear() {
            this._frontBuffer.width = this._backBuffer.width = 1;
            this._frontBuffer.width = this._backBuffer.width = 1;
            this._bbScale = -1;
            this._fbScale = -1;
            this._promiseRunning = false;
        }

    }

    class PDFTextElement implements model.TextElement {
        constructor(public angle:number, public size:Size2D, public fontSize:number, public text:string, pos:Position2D, public fontFamily:string, public pageHref:string) {
            this.pos = new Position2D(pos.x, pos.y - fontSize);
        }

        public pos:Position2D;
        public fromBottomLeft = false;

        toString() {
            return this.pageHref.toString + "-" + this.pos.toString() + "-" + this.text.toString() + "-" + this.angle.toString();
        }
    }

    interface DrawInformation {
        //area:Rect;
        scale: number;
    }

}