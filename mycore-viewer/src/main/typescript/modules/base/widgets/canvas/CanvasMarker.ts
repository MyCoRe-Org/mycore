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
            var isAreaMarked:boolean = this.areasToMark.values.some(marks => {
                return marks.filter(isArea).length > 0;
            });
            if(isAreaMarked) {
                var areas:Array<AreaInPage> = this.areasToMark.get(id);
                var filteredAreas:Array<AreaInPage> = areas == null ? [] : areas.filter(isArea);
                var areaRGBA = "rgba(0,0,0,0.4)";
                ctx.save();
                {
                    ctx.strokeStyle = areaRGBA;
                    ctx.fillStyle = areaRGBA;
                    ctx.beginPath();
                    ctx.rect(0, 0, pageSize.width, pageSize.height );
                    ctx.closePath();
                    ctx.fill();
                    filteredAreas.forEach(area => {
                        ctx.clearRect(area.x, area.y, area.width, area.height);
                    });
                }
                ctx.restore();
            }            

            this.areasToMark.hasThen(id, marks => {
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
                    let wordsStrong = marks.filter((area) => {
                        return "markerType" in area && area.markerType == CanvasMarkerType.WORD_STRONG;
                    });
                    if (wordsStrong.length > 0) {
                        ctx.strokeStyle = "rgba(244, 244, 66, 0.8)";
                        let lineWidth = Math.max(pageSize.width / 200, pageSize.height / 200) * window.devicePixelRatio;
                        ctx.lineWidth = lineWidth;
                        ctx.beginPath();

                        wordsStrong.forEach(strongWord => {
                            ctx.rect(strongWord.x - lineWidth / 2, strongWord.y - lineWidth / 2, strongWord.width + lineWidth, strongWord.height + lineWidth);
                        });
                        ctx.closePath();
                        ctx.stroke();
                    }
                }
                ctx.restore();
            });

            function isArea(marker:AreaInPage):boolean {
                return "markerType" in marker && marker.markerType == CanvasMarkerType.AREA;
            }
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

        /**
         * Tries to maximize the bounds of this area.
         */
        public maximize(x:number, y:number, width:number, height:number):void {
            var right1:number = this.x + this.width;
            var right2:number = x + width;
            var bottom1:number = this.y + this.height;
            var bottom2:number = y + height;
            this.x = x < this.x ? x : this.x;
            this.y = y < this.y ? y : this.y;
            this.width = Math.max(right1, right2) - this.x;
            this.height = Math.max(bottom1, bottom2) - this.y;
            this.correctBounds();
        }

        /**
         * Increase this area with the given number of pixel's on all sides.
         * This is like adding a padding.
         */
        public increase(pixel:number):void {
            this.x -= pixel;
            this.y -= pixel;
            this.width += 2 * pixel;
            this.height += 2 * pixel;
            this.correctBounds();
        }

        public correctBounds() {
            this.x = this.x < 0 ? 0 : this.x;
            this.y = this.y < 0 ? 0 : this.y;
            // TODO handle width/height somehow
        }

    }

    export enum CanvasMarkerType {
        WORD,
        WORD_STRONG,
        AREA
    }
}
