///<reference path="ViewerModalWindow.ts"/>
namespace mycore.viewer.widgets.modal {

    export class ViewerErrorModal extends IviewModalWindow {

        constructor(_mobile:boolean, errorTitle:string,errorText:string, imageUrl:string, parent:HTMLElement=document.body) {
            super(_mobile, errorTitle, parent);
            this.modalHeader.children("h4").addClass("text-danger");
            this.modalBody.append("<div class='error-image-holder'><img class='thumbnail error-image' src='"+imageUrl+"' /> <span>" + errorText + "</span></div>")
        }
    }

}