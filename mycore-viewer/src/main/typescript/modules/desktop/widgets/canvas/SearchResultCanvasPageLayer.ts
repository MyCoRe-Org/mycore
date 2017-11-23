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

namespace mycore.viewer.widgets.canvas {

    export class SearchResultCanvasPageLayer implements widgets.canvas.CanvasPageLayer {

        private selected: Array<PageArea> = [];
        private areas: MyCoReMap<string, Array<PageArea>> = new MyCoReMap<string, Array<PageArea>>();

        public select(page:string, rect:Rect) {
            this.selected.push(new PageArea(page, rect));
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
            this.selected = [];
            this.areas.clear();
        }

        public clearSelected(): void {
            this.selected = [];
        }

        public draw(ctx:CanvasRenderingContext2D, id:string, pageSize:Size2D, drawOnHtml:boolean = false) {
            this.selected.forEach(area => {
                if (this.selected != null && id === area.page) {
                    this.drawWithPadding(ctx, [area], pageSize);
                }
            });
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
