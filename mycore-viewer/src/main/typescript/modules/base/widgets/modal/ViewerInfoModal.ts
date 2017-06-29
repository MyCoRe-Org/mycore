///<reference path="ViewerModalWindow.ts"/>
namespace mycore.viewer.widgets.modal {

    export class ViewerInfoModal extends IviewModalWindow {

        constructor(_mobile:boolean, title:string,text:string, parent:HTMLElement=document.body) {
            super(_mobile, title, parent);
            this.modalHeader.children("h4").addClass("text-info");
            this.modalBody.append("<p><span data-i18n='" + text + "'>" + text + "</span></p>");
        }
    }

}
