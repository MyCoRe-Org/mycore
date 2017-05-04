namespace mycore.viewer.components.events {

    export class RequestAltoModelEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent,
                    public _href:string,
                    public _onResolve:( imgName:string, altoName:string, altoContainer:widgets.alto.AltoFile)=> void) {
            super(component, RequestAltoModelEvent.TYPE);
        }

        public static TYPE:string = "RequestAltoModelEvent";
    }
}