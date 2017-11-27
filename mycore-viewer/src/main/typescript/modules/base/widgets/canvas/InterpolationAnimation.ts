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
