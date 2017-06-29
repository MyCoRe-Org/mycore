///<reference path="ViewerModalWindow.ts"/>
namespace mycore.viewer.widgets.modal {

    export class ViewerConfirmModal extends IviewModalWindow {

        constructor(_mobile:boolean, confirmTitle:string, confirmText:string, callback:Function, parent:HTMLElement=document.body) {
            super(_mobile, confirmTitle, parent);
            this.modalHeader.children("h4").addClass("text-info");
            this.modalBody.append("<p><span data-i18n='" + confirmText +"'>" + confirmText + "</span></p>");

            this.modalFooter.empty();

            this.createButton(true, callback);
            this.createButton(false, callback);
        }

        private createButton(confirm:boolean, callback:Function):void {
            let key = confirm ? "yes" : "no";
            let button = jQuery("<a data-i18n='modal." + key + "'></a>");
            button.attr("type", "button");
            button.addClass("btn btn-default");
            button.appendTo(this.modalFooter);

            var that = this;
            button.click(()=> {
                if(callback) {
                    callback(confirm);
                }
                that.hide();
            });
        }
    }

}
