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

