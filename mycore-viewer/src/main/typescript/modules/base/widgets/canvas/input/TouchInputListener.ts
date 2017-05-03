/// <reference path="TouchSession.ts" />
/// <reference path="TouchMove.ts" />

namespace mycore.viewer.widgets.canvas {

    export interface TouchInputListener {
        touchStart( session: TouchSession ): void;
        touchMove( session: TouchSession ): void;
        touchEnd( session: TouchSession ): void;
    }

    export abstract class TouchInputAdapter implements TouchInputListener {
        touchStart( session: TouchSession ): void { }
        touchMove( session: TouchSession ): void { }
        touchEnd( session: TouchSession ): void { }
    }

}