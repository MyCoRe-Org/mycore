namespace mycore.viewer.widgets.toolbar {

    export class BootstrapTextInputView implements TextInputView {

        constructor(private _id: string) {
            this.element = jQuery("<form></form>");
            this.element.addClass("navbar-form");
            this.element.css({display : "inline-block"});

            this.childText = jQuery("<input type='text' class='form-control'/>");
            this.childText.appendTo(this.element);

            this.childText.keydown((e)=>{
                if(e.keyCode==13){
                    e.preventDefault();
                }
            });

            this.childText.keyup((e)=>{
                if(e.keyCode){
                    if(e.keyCode==27){ // Unfocus when pressing escape
                        this.childText.val("");
                        this.childText.blur()
                    }
                }

               if(this.onChange!=null){
                   this.onChange();
               }
            });
        }

        private element: JQuery;
        private childText: JQuery;

        public onChange: ()=>void = null;

        updateValue(value: string): void {
            this.childText.val(value);
        }

        getValue(): string {
            return this.childText.val();
        }

        getElement(): JQuery {
            return this.element;
        }

        updatePlaceholder(placeHolder: string): void {
            this.childText.attr("placeholder", placeHolder);
        }
    }
}
