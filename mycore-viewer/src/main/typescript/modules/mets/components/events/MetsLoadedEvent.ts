module mycore.viewer.components.events {
    export class MetsLoadedEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public mets:any) {
            super(component, MetsLoadedEvent.TYPE);
        }

        public static TYPE:string = "MetsLoadedEvent";
    }
}