/// <reference path="Animation.ts" />

namespace mycore.viewer.widgets.canvas {

    /**
     * An animation that knows about its own state.
     */
    export abstract class StatefulAnimation implements Animation {

        public isRunning;
        public isFinished;
        public isPaused;
        protected totalElapsedTime;

        constructor() {
            this.isRunning = false;
            this.isFinished = false;
            this.isPaused = false;
            this.totalElapsedTime = 0;
        }

        updateAnimation( elapsedTime: number ): boolean {
            if(this.isPaused || this.isFinished) {
                return this.isFinished;
            }
            if(!this.isRunning) {
                elapsedTime = 0;
                this.totalElapsedTime = 0;
            }
            this.totalElapsedTime += elapsedTime;
            this.isFinished = this.update(elapsedTime);
            this.isRunning = !this.isFinished;
            return this.isFinished;
        }

        abstract update(elapsedTime: number):boolean;

        pause() {
            this.isPaused = true;
        }

        continue() {
            this.isPaused = false;
        }

    }

}

