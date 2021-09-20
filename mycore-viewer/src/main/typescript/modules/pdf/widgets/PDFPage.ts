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

/// <reference path="../definitions/pdf.d.ts" />
/// <reference path="PDFStructureBuilder.ts" />

namespace mycore.viewer.widgets.canvas {
    export class PDFPage implements model.AbstractPage {

        constructor(public id: string, private pdfPage: PDFPageProxy, private builder: pdf.PDFStructureBuilder) {
            let width = (this.pdfPage.view[ 2 ] - this.pdfPage.view[ 0 ]) * PDFPage.CSS_UNITS;
            let height = (this.pdfPage.view[ 3 ] - this.pdfPage.view[ 1 ]) * PDFPage.CSS_UNITS;

            const pageRotation = pdfPage.rotate;
            if (pageRotation == 90 || pageRotation == 270) {
                width = width ^ height;
                height = height ^ width;
                width = width ^ height;
            }

            this._rotation = pageRotation;
            this.size = new Size2D(width, height);
        }

        static CSS_UNITS = 96.0 / 72.0;
        public refreshCallback:() => void;
        public size:Size2D;

        private _rotation:number;
        private _frontBuffer:HTMLCanvasElement = document.createElement("canvas");
        private _backBuffer:HTMLCanvasElement = document.createElement("canvas");
        private _timeOutIDHolder: number = null;

        private _bbScale:number = -1;
        private _fbScale:number = -1;
        private _promiseRunning:boolean = false;
        private _textData:model.TextContentModel = null;

        public resolveTextContent(callback:(content:model.TextContentModel)=>void):void {
            if (this._textData == null) {
                var textContent:model.TextContentModel = {
                    content : [],
                    links : [],
                    internLinks: []
                };
                this._textData = textContent;
                let contentReady = false, linksReady = false;
                let completeCall = () => (contentReady && linksReady) ? callback(textContent) : null;

                this.pdfPage.getAnnotations().then((anotations) => {
                    linksReady = true;
                    if (anotations.length > 0) {
                        for (var annotation of anotations) {
                            if ((<any>annotation).annotationType == 2 && (<any>annotation).subtype == 'Link') {
                                if ("url" in annotation) {
                                    textContent.links.push({
                                        rect: this.getRectFromAnnotation(annotation),
                                        url: (<any>annotation).url
                                    });
                                } else if ("dest" in annotation) {
                                    let numberResolver = ((annotation:any) => {
                                        return (callback) => {
                                            this.builder.getPageNumberFromDestination(annotation.dest, (pageNumber) => {
                                                callback(pageNumber+"");
                                            });
                                        }
                                    })(annotation);

                                    textContent.internLinks.push(
                                        {
                                            rect: this.getRectFromAnnotation(annotation),
                                            pageNumberResolver:numberResolver
                                        }
                                    );
                                }
                            }
                        }
                    }
                    completeCall();
                });

                this.pdfPage.getTextContent().then((textData:PDFPageTextData)=> {
                    contentReady = true;
                    textData.items.forEach((e)=> {

                        var vp = (<any>this.pdfPage.getViewport({scale: 1}));
                        var transform = (<any>pdfjsLib).Util.transform(vp.transform, e.transform);

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

                            x = transform[ 4 ]  * PDFPage.CSS_UNITS;
                            y = transform[ 5 ]  * PDFPage.CSS_UNITS;


                        var textElement = new PDFTextElement(angle, new Size2D(e.width, e.height).scale(PDFPage.CSS_UNITS).roundDown(), fontHeight, e.str, new Position2D(x, y), style.fontFamily, this.id);
                        textContent.content.push(textElement);
                    });

                    completeCall();
                }, (reason:string) => {
                    contentReady = true;
                    console.error("PDF Page Text Content rejected");
                    console.error("Reason: " + reason);
                    completeCall();
                });

            } else {
                callback(this._textData);
            }


        }

        private getRectFromAnnotation(annotation) {
            return new Rect(
                new Position2D(annotation.rect[0] * PDFPage.CSS_UNITS, this.size.height - (annotation.rect[1] * PDFPage.CSS_UNITS) - ((annotation.rect[3] - annotation.rect[1]) * PDFPage.CSS_UNITS)),
                new Size2D((annotation.rect[2] - annotation.rect[0]) * PDFPage.CSS_UNITS, (annotation.rect[3] - annotation.rect[1]) * PDFPage.CSS_UNITS)
            );

        }

        public draw(ctx: CanvasRenderingContext2D, rect: Rect, sourceScale, overview: boolean, infoScale:number): void {
            if ((!overview && sourceScale != this._fbScale) || this._fbScale == -1) {
                if(!this._promiseRunning ){
                    this._updateBackBuffer(sourceScale);
                }
            }

            if (this._fbScale == -1) {
                return;
            }

            var scaledRect = rect.scale(this._fbScale);
            var sourceScaleRect = rect.scale(sourceScale);

            var sw =scaledRect.size.width;
            var sh = scaledRect.size.height;

            if (sw > 0 && sh > 0) {
                ctx.save();
                {
                    ctx.drawImage(this._frontBuffer, scaledRect.pos.x, scaledRect.pos.y, Math.min(sw, this._frontBuffer.width), Math.min(sh, this._frontBuffer.height), 0, 0, sourceScaleRect.size.width, sourceScaleRect.size.height)
                }
                ctx.restore();
            }

        }

        private _updateBackBuffer(newScale) {
            var vp = this.pdfPage.getViewport({scale: newScale * PDFPage.CSS_UNITS, rotation: this._rotation});
            var task = <any> this.pdfPage.render(<PDFRenderParams>{
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
            return <any>this.pdfPage.pageNumber;
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
