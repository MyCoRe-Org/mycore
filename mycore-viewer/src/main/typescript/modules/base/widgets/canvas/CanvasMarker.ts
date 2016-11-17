module mycore.viewer.widgets.canvas {
    export class CanvasMarker {

        constructor(private updateCallback:()=>void) {
        }

        //<page, area>
        private areasToMark:MyCoReMap<string, Array<AreaInPage>> = new MyCoReMap<string, Array<AreaInPage>>();

        public markAreas(areas:Array<AreaInPage>) {
            areas.forEach(area=>this.markArea(area));
        }

        public markArea(area:AreaInPage) {
            let areasOfPage:Array<AreaInPage>;

            if (!this.areasToMark.has(area.page)) {
                areasOfPage = new Array<AreaInPage>();
                this.areasToMark.set(area.page, areasOfPage);
            } else {
                areasOfPage = this.areasToMark.get(area.page);
            }

            areasOfPage.push(area);
            this.updateCallback();
        }

        public clearAll(word?: mycore.viewer.widgets.canvas.CanvasMarkerType): void {
            if (typeof word == "undefined") {
                this.areasToMark.clear();
                console.warn("Marker clean call without Type :/");
            } else {
                let newMap = new MyCoReMap<string, Array<AreaInPage>>();
                this.areasToMark.forEach((k, v)=> {
                    var newArr = v.filter(area=>area.markerType != word);
                    if (newArr.length != 0) {
                        newMap.set(k, newArr);
                    }
                });
                this.areasToMark = newMap;
            }

            this.updateCallback();
        }

        public draw(id:string, ctx:CanvasRenderingContext2D, pageSize:Size2D) {
            this.areasToMark.hasThen(id, marks=> {
                ctx.save();
                {
                    let words = marks.filter((area)=> {
                        return "markerType" in area && area.markerType == CanvasMarkerType.WORD;
                    });

                    if (words.length > 0) {
                        ctx.fillStyle = "rgba(179,216,253,0.6)";
                        ctx.beginPath();
                        words.forEach(word=> {
                            ctx.rect(word.x, word.y, word.width, word.height);
                        });
                        ctx.closePath();
                        ctx.fill();
                    }
                }
                ctx.restore();


                ctx.save();
                {
                    let wordsStrong = marks.filter((area)=> {
                        return "markerType" in area && area.markerType == CanvasMarkerType.WORD_STRONG;
                    });
                    if (wordsStrong.length > 0) {
                        ctx.strokeStyle = "rgba(244, 244, 66, 0.8)";
                        let lineWidth = Math.max(pageSize.width / 200, pageSize.height / 200) * window.devicePixelRatio;
                        ctx.lineWidth = lineWidth;
                        ctx.beginPath();

                        wordsStrong.forEach(strongWord=> {
                            ctx.rect(strongWord.x - lineWidth / 2, strongWord.y - lineWidth / 2, strongWord.width + lineWidth, strongWord.height + lineWidth);
                        });
                        ctx.closePath();
                        ctx.stroke();
                    }
                }
                ctx.restore();


                ctx.save();
                {
                    let areas = marks.filter((area)=> {
                        return !("markerType" in area) || ("markerType" in area && area.markerType == CanvasMarkerType.AREA);
                    });

                    if (areas.length > 0) {
                        var tolerancePixel = 3;
                        ctx.strokeStyle = "rgba(0,0,0,0.5)";
                        ctx.fillStyle = "rgba(0,0,0,0.5)";
                        ctx.beginPath();
                        areas.forEach(area=> {

                            ctx.rect(
                                area.x - (tolerancePixel * window.devicePixelRatio),
                                area.y - (tolerancePixel * window.devicePixelRatio),
                                area.width + (2 * tolerancePixel * window.devicePixelRatio),
                                area.height + (2 * tolerancePixel * window.devicePixelRatio));
                        });
                        ctx.rect(pageSize.width, 0, -pageSize.width, pageSize.height);
                        ctx.closePath();
                        ctx.fill();

                    }
                }
                ctx.restore();


            });
        }
    }

    export class AreaInPage {
        constructor(public page:string,
                    public x:number,
                    public y:number,
                    public width:number,
                    public height: number,
                    public markerType?: CanvasMarkerType) {
        }
    }

    export enum CanvasMarkerType {
        WORD,
        WORD_STRONG,
        AREA
    }
}
