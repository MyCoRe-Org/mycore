/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
