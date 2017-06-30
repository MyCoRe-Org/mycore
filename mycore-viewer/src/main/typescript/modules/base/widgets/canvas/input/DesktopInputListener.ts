/// <reference path="../../../Utils.ts" />

namespace mycore.viewer.widgets.canvas {

    export interface DesktopInputListener {
        mouseDown(position: Position2D, e: JQueryMouseEventObject): void;
        mouseUp(position: Position2D, e: JQueryMouseEventObject): void;
        mouseClick(position: Position2D, e: JQueryMouseEventObject): void;
        mouseDoubleClick(position: Position2D, e: JQueryMouseEventObject): void;
        mouseMove(position: Position2D, e: JQueryMouseEventObject): void;
        mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
                  e: JQueryMouseEventObject): void;
        scroll(e: {
            deltaX: number;
            deltaY: number;
            orig: any;
            pos: Position2D;
            altKey?: boolean
        });
        keydown(e: JQueryKeyEventObject): void;
        keypress(e: JQueryKeyEventObject): void;
        keyup(e: JQueryKeyEventObject): void;
    }

    export abstract class DesktopInputAdapter implements DesktopInputListener {
        mouseDown(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseUp(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseClick(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseDoubleClick(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseMove(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
                  e: JQueryMouseEventObject): void {
        }

        scroll(e: {
            deltaX: number;
            deltaY: number;
            orig: any;
            pos: Position2D;
            altKey?: boolean
        }) {
        }

        keydown(e: JQueryKeyEventObject): void {
        }

        keypress(e: JQueryKeyEventObject): void {
        }

        keyup(e: JQueryKeyEventObject): void {
        }
    }

}
