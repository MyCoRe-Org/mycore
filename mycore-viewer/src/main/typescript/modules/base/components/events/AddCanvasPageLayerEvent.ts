/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
  export class AddCanvasPageLayerEvent extends MyCoReImageViewerEvent {
      constructor(component:ViewerComponent, public zIndex:number, public canvasPageLayer:widgets.canvas.CanvasPageLayer) {
          super(component, AddCanvasPageLayerEvent.TYPE);
      }

      public static TYPE:string = "AddCanvasPageLayerEvent";

  }
}
