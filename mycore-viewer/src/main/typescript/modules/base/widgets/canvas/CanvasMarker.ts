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

        public clearAll() {
            this.areasToMark.clear();
            this.updateCallback();
        }

        public draw(id:string, ctx:CanvasRenderingContext2D, pageSize:Size2D) {
            this.areasToMark.hasThen(id, areas=>{
                ctx.save();
                var tolerancePixel = 3;

                
                ctx.fillStyle = "rgba(0,0,0,0.5)";
                ctx.beginPath();
                areas.forEach(area=> {

                    ctx.rect(
                        area.x-(tolerancePixel*window.devicePixelRatio),
                        area.y-(tolerancePixel*window.devicePixelRatio),
                        area.width+(2*tolerancePixel*window.devicePixelRatio),
                        area.height+(2*tolerancePixel*window.devicePixelRatio));
                });
                ctx.rect(pageSize.width, 0, -pageSize.width, pageSize.height);
                ctx.fill();

                /*
                ctx.strokeStyle = "rgba(255,0,0,1)";
                ctx.lineWidth = 3*window.devicePixelRatio;
                ctx.beginPath();
                areas.forEach(area=> {
                    ctx.rect(
                        area.x - (tolerancePixel * window.devicePixelRatio),
                        area.y - (tolerancePixel * window.devicePixelRatio),
                        area.width + (2 * tolerancePixel * window.devicePixelRatio),
                        area.height + (2 * tolerancePixel * window.devicePixelRatio));
                });
                ctx.stroke();*/

                ctx.restore();
            });
        }
    }

    export class AreaInPage {
        constructor(public page:string,
                    public x:number,
                    public y:number,
                    public width:number,
                    public height:number) {
        }
    }
}