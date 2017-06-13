/// <reference path="../../components/MyCoReHighlightAltoComponent.ts" />

namespace mycore.viewer.widgets.canvas {

    /**
     * This class is responsible for highlighting chapters.
     */
    export class HighlightAltoChapterCanvasPageLayer implements widgets.canvas.CanvasPageLayer {

        public selectedChapter: components.ChapterArea = null;
        public highlightedChapter: components.ChapterArea = null;
        public fadeAnimation:widgets.canvas.InterpolationAnimation = null;

        private chaptersToClear: MyCoReMap<string, components.ChapterArea> = new MyCoReMap<string, components.ChapterArea>();
        private enabled: boolean = true;

        public isEnabled() {
            return this.enabled;
        }

        public setEnabled(enabled:boolean) {
            this.enabled = enabled;
        }

        public draw( ctx: CanvasRenderingContext2D, id: string, pageSize: Size2D, drawOnHtml: boolean = false ) {
            if(!this.isEnabled()) {
                return;
            }
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
                if(!this.isLinkedWithoutBlocks(id)) {
                    this.darkenPage(ctx, pageSize, rgba);
                }
                this.clearRects(ctx, id);
            }

            if(highlighted && selected) {
                this.drawRects(ctx, id, this.highlightedChapter.pages, "rgba(0,0,0,0.2)");
            }
        }

        /**
         * Checks if a chapter is selected.
         *
         * @returns true if a chapter is selected
         */
        private isChapterSelected():boolean {
            return this.selectedChapter != null && !this.selectedChapter.pages.isEmpty();
        }

        /**
         * Checks if the highlighting is currently activated.
         *
         * @returns true if highlighted false otherwise
         */
        private isHighlighted():boolean {
            let highlighted:boolean = this.highlightedChapter != null && !this.highlightedChapter.pages.isEmpty();
            if(highlighted && this.isChapterSelected()) {
                return this.highlightedChapter.chapterId !== this.selectedChapter.chapterId;
            }
            return highlighted;
        }

        /**
         * Some pages are linked with a FILEID but has no BETYPE='IDREF'. This happens
         * usually for images like maps. In this case we want to don't want to darken
         * the image. So this method return true if the fileID is linked but has no
         * blocks.
         *
         * @param fileID the mets:area FILEID
         * @returns true if the file is linked but has no blocks
         */
        private isLinkedWithoutBlocks(fileID:string):boolean {
            return !this.chaptersToClear.filter((id:string, area:components.ChapterArea) => {
                let rects:Array<Rect> = area.pages.get(fileID);
                if (rects != null && rects.length === 0) {
                    return true;
                }
                return false;
            }).isEmpty();
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
