module mycore.viewer.widgets.canvas {

    export class MouseSession {

        /**
         * Creates a new mouse session
         * @param startPositionInputElement the start position in the input element
         * @param startPositionViewport the position of the viewport on mousedown
         * @param currentPositionInputElement the current position of the mouse (changes)
         * @param lastMouseSession the last mouse session (not chaining to prevent memory leak)
         */
        constructor(public startPositionInputElement:Position2D,
                    public startPositionViewport:Position2D,
                    public currentPositionInputElement:Position2D,
                    public lastMouseSession:MouseSession){

            if(this.lastMouseSession != null){
                this.lastMouseSession.lastMouseSession = null; // prevent chaining
            }
            this.downDate = new Date().valueOf();
        }

        /**
         * The Date.valueOf the mouseDown occurred
         */
        public downDate:number;

        /**
         * The Date.valueOf the mouseUp occurred
         */
        public upDate:number = null;

    }

}