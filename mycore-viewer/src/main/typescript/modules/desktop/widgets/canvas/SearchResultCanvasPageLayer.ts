namespace mycore.viewer.widgets.canvas {

    export class SearchResultCanvasPageLayer implements widgets.canvas.CanvasPageLayer {

        private selected: PageArea = null;
        private areas: MyCoReMap<string, Array<PageArea>> = new MyCoReMap<string, Array<PageArea>>();

        public select(page:string, rect:Rect) {
            this.selected = new PageArea(page, rect);
        }

        public add(page:string, rect:Rect) {
            let pageAreas:Array<PageArea> = this.areas.get(page);
            if (pageAreas == null) {
                pageAreas = new Array<PageArea>();
                this.areas.set(page, pageAreas);
            }
            pageAreas.push(new PageArea(page, rect));
        }

        public clear():void {
            this.selected = null;
            this.areas.clear();
        }

        public draw(ctx:CanvasRenderingContext2D, id:string, pageSize:Size2D, drawOnHtml:boolean = false) {
            if(this.selected != null && id === this.selected.page) {
                this.drawWithPadding(ctx, [this.selected], pageSize);
            }
            this.areas.hasThen(id, areas => {
                this.drawWords(ctx, areas);
            });
        }

        private drawWithPadding(ctx:CanvasRenderingContext2D, pageAreas:Array<PageArea>, pageSize:Size2D) {
            ctx.save();
            {
                ctx.strokeStyle = "rgba(244, 244, 66, 0.8)";
                let lineWidth = Math.max(pageSize.width / 200, pageSize.height / 200) * window.devicePixelRatio;
                ctx.lineWidth = lineWidth;
                ctx.beginPath();
                pageAreas.forEach(word => {
                    let x = word.rect.getX() - lineWidth / 2;
                    let y = word.rect.getY() - lineWidth / 2;
                    let width = word.rect.getWidth() + lineWidth;
                    let height = word.rect.getHeight() + lineWidth;
                    ctx.rect(x, y, width, height);
                });
                ctx.closePath();
                ctx.stroke();
            }
            ctx.restore();
        }

        private drawWords(ctx:CanvasRenderingContext2D, pageAreas:Array<PageArea>) {
            ctx.save();
            {
                ctx.fillStyle = "rgba(179,216,253,0.6)";
                ctx.beginPath();
                pageAreas.forEach(area => {
                    ctx.rect(area.rect.getX(), area.rect.getY(), area.rect.getWidth(), area.rect.getHeight());
                });
                ctx.closePath();
                ctx.fill();
            }
            ctx.restore();
        }
    }

    class PageArea {
        constructor(
            public page: string,
            public rect: Rect
        ) {
        }
    }
}
