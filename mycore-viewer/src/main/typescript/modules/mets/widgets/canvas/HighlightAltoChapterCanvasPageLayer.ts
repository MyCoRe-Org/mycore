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

/// <reference path="../../components/MyCoReHighlightAltoComponent.ts" />

namespace mycore.viewer.widgets.canvas {

    /**
     * This class is responsible for highlighting chapters.
     */
    export class HighlightAltoChapterCanvasPageLayer implements widgets.canvas.CanvasPageLayer {

        public selectedChapter: components.AltoChapter = null;
        public highlightedChapter: components.AltoChapter = null;
        public fadeAnimation:widgets.canvas.InterpolationAnimation = null;

        private chaptersToClear: MyCoReMap<string, components.AltoChapter> = new MyCoReMap<string, components.AltoChapter>();
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
                this.drawRects(ctx, id, this.highlightedChapter.boundingBoxMap, "rgba(0,0,0,0.2)");
            }
        }

        /**
         * Checks if a chapter is selected.
         *
         * @returns true if a chapter is selected
         */
        private isChapterSelected():boolean {
            return this.selectedChapter != null && !this.selectedChapter.boundingBoxMap.isEmpty();
        }

        /**
         * Checks if the highlighting is currently activated.
         *
         * @returns true if highlighted false otherwise
         */
        private isHighlighted():boolean {
            let highlighted:boolean = this.highlightedChapter != null && !this.highlightedChapter.boundingBoxMap.isEmpty();
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
            return !this.chaptersToClear.filter((id:string, area:components.AltoChapter) => {
                let rects:Array<Rect> = area.boundingBoxMap.get(fileID);
                return rects != null && rects.length === 0;
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
                    chapterArea.boundingBoxMap.hasThen( id, rects => {
                        rects.forEach(rect => {
                            ctx.clearRect( rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight() );

                            /*
                            // Add some strokes around the blocks for testing purpose
                            ctx.strokeStyle = "rgba(1,0,0,0.8)";
                            ctx.lineWidth = 5;
                            ctx.beginPath();
                            ctx.rect( rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight() );
                            ctx.closePath();
                            ctx.stroke();
                            */
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
