/// <reference path="../../components/MyCoReHighlightAltoComponent.ts" />

namespace mycore.viewer.widgets.canvas {

    export class HighlightAltoCanvasPageLayer implements widgets.canvas.CanvasPageLayer {

        public selectedChapter: components.ChapterArea = null;
        public highlightedChapter: components.ChapterArea = null;
        public fadeAnimation:widgets.canvas.InterpolationAnimation = null;

        private chaptersToClear: MyCoReMap<string, components.ChapterArea> = new MyCoReMap<string, components.ChapterArea>();

        public draw( ctx: CanvasRenderingContext2D, id: string, pageSize: Size2D, drawOnHtml: boolean = false ) {
            let selected:boolean = this.isChapterSelected();
            let highlighted:boolean = this.isHighlighted();
            let animated:boolean = this.fadeAnimation != null && this.fadeAnimation.isRunning;

            if(!animated && !selected && !highlighted) {
                this.chaptersToClear.clear();
            }

            if(selected) {
                this.chaptersToClear.set("selected", this.selectedChapter);
            }
            if(highlighted) {
                this.chaptersToClear.set("highlighted", this.highlightedChapter);
            } else if(selected) {
                this.chaptersToClear.remove("highlighted");
            }

            if(animated || selected || highlighted) {
                let rgba = selected ? "rgba(0,0,0,0.4)" : "rgba(0,0,0,0.15)";
                if(this.fadeAnimation != null) {
                    rgba = "rgba(0,0,0," + this.fadeAnimation.value + ")";
                }
                this.darkenPage(ctx, pageSize, rgba);
                this.clearRects(ctx, id);
            }

            if(highlighted && selected) {
                this.drawRects(ctx, id, this.highlightedChapter.pages, "rgba(0,0,0,0.2)");
            }
        }

        private isChapterSelected():boolean {
            return this.selectedChapter != null && !this.selectedChapter.pages.isEmpty();
        }

        private isHighlighted():boolean {
            let highlighted:boolean = this.highlightedChapter != null && !this.highlightedChapter.pages.isEmpty();
            if(highlighted && this.isChapterSelected()) {
                return this.highlightedChapter.chapterId !== this.selectedChapter.chapterId;
            }
            return highlighted;
        }

        private darkenPage(ctx: CanvasRenderingContext2D, pageSize: Size2D, rgba:string) {
            ctx.save();
            {
                ctx.strokeStyle = rgba;
                ctx.fillStyle = rgba;
                ctx.beginPath();
                ctx.rect( 0, 0, pageSize.width, pageSize.height );
                ctx.closePath();
                ctx.fill();
            }
            ctx.restore();
        }

        private clearRects(ctx: CanvasRenderingContext2D, id: string) {
            ctx.save();
            {
                this.chaptersToClear.values.forEach(chapterArea => {
                    chapterArea.pages.hasThen( id, rects => {
                        rects.forEach(rect => {
                            ctx.clearRect( rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight() );
                        });
                    });
                });
            }
            ctx.restore();
        }

        private drawRects(ctx: CanvasRenderingContext2D, pageId:string, pages:MyCoReMap<string, Array<Rect>>, rgba:string) {
            ctx.save();
            {
                ctx.strokeStyle = rgba;
                ctx.fillStyle = rgba;
                ctx.beginPath();
                pages.hasThen( pageId, rects => {
                    rects.forEach(rect => {
                        ctx.rect( rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight() );
                    });
                });
                ctx.closePath();
                ctx.fill();
            }
            ctx.restore();
        }

    }

}
