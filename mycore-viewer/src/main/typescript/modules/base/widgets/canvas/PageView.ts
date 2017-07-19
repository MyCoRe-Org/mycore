namespace mycore.viewer.widgets.canvas {

    export class PageView {

        public container:JQuery = jQuery("<div></div>");
        public drawCanvas:HTMLCanvasElement = null;
        public markCanvas:HTMLCanvasElement = null;

        constructor(public drawImage:boolean = true, public drawHTML:boolean = true) {
            let drawFilter = drawImage && drawHTML ? "grayscale(1) contrast(1000%)" : null;
            this.drawCanvas = PageView.createCanvas(1, drawFilter);
            this.markCanvas = PageView.createCanvas(4);

            jQuery(this.drawCanvas).appendTo(this.container);
            jQuery(this.markCanvas).appendTo(this.container);
            this.container.css({
                "position": "absolute",
                "top": "0px",
                "left": "0px",
                "bottom": "0px",
                "right": "0px",
                "overflow":"hidden"
            });

            let ctx1 = <CanvasRenderingContext2D>this.drawCanvas.getContext("2d");
            let ctx2 = <CanvasRenderingContext2D>this.markCanvas.getContext("2d");
            if ("imageSmoothingEnabled" in ctx1) {
                (<any>ctx1).imageSmoothingEnabled = false;
                (<any>ctx2).imageSmoothingEnabled = false;
            }

        }

        private static createCanvas(zIndex:number = 1, filter?: string) {
            let canvas:HTMLCanvasElement = document.createElement("canvas");
            canvas.style.transform = "scale(1.0)";
            canvas.style.position = "absolute";
            canvas.style.zIndex = zIndex+"";
            if(filter) {
                canvas.style.filter = filter;
            }
            return canvas;
        }

    }
}
