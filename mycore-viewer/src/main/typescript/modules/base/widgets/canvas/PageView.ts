module mycore.viewer.widgets.canvas {
    export class PageView {

        constructor(private _drawImage:boolean = true, private _drawHTML:boolean = true) {
            jQuery(this._drawCanvas).appendTo(this._container);
            jQuery(this._markCanvas).appendTo(this._container);
            this._container.css({
                "position": "absolute",
                "top": "0px",
                "left": "0px",
                "bottom": "0px",
                "right": "0px",
                "overflow":"hidden"
            });
        }

        private _container:JQuery = jQuery("<div></div>");
        private _drawCanvas:HTMLCanvasElement = PageView.createCanvas(1);
        private _markCanvas:HTMLCanvasElement = PageView.createCanvas(2);

        private _ctx:CanvasRenderingContext2D;

        public get drawCanvas() {
            return this._drawCanvas;
        }

        public get markCanvas() {
            return this._markCanvas;
        }

        public get container() {
            return this._container;
        }

        public set container(container:JQuery) {
            this._container = container;
        }

        private static createCanvas(zIndex:number = 1) {
            var canvas:HTMLCanvasElement = document.createElement("canvas");
            canvas.style.transform = "scale(1.0)";
            canvas.style.position = "absolute";
            canvas.style.zIndex = zIndex+"";
            return canvas;
        }


        public get drawImage():boolean {
            return this._drawImage;
        }

        public get drawHTML():boolean {
            return this._drawHTML;
        }


    }
}