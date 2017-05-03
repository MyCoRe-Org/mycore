namespace mycore.viewer.components.events {
    export class MetsLoadedEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public mets:{ model:model.StructureModel; document:Document }) {
            super(component, MetsLoadedEvent.TYPE);
        }

        public static TYPE:string = "MetsLoadedEvent";
    }
}