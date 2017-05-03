/// <reference path="StatefulAnimation.ts" />

namespace mycore.viewer.widgets.canvas {

    export class InterpolationAnimation extends StatefulAnimation {

        public value:number;

        constructor(
            protected duration: number,
            protected from: number,
            protected to: number,
            protected interpolationFunction: ( from: number, to: number, progress: number ) => number = linearInterpolation ) {
            super();
            this.value = this.from;
        }

        update(elapsedTime: number):boolean {
            if(this.totalElapsedTime >= this.duration) {
                this.totalElapsedTime = this.duration;
                this.value = this.to;
                return true;
            }
            const progress:number = this.totalElapsedTime / this.duration;
            this.value = this.interpolationFunction(this.from, this.to, progress);
            return false;
        }

    }

    function linearInterpolation(from:number, to:number, progress:number):number {
        return from + ((to - from) * progress);
    }

}
