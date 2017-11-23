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

/// <reference path="group/MobileGroupView.ts" />
/// <reference path="dropdown/MobileDropdownView.ts" />
/// <reference path="button/MobileButtonView.ts" />
/// <reference path="MobileToolbarView.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class MobileToolbarViewFactory implements ToolbarViewFactory {

        createTextInputView(id: string): mycore.viewer.widgets.toolbar.TextInputView {
            throw new ViewerError("text input view not supported by Mobile!");
        }
        createToolbarView():ToolbarView {
            return new MobileToolbarView();
        }

        createTextView(id:string):TextView {
            throw new ViewerError("text view not supported by Mobile!");
        }

        createImageView(id:string):ImageView {
            throw new ViewerError("image view not supported by Mobile!");
        }

        createGroupView(id:string, align:string):GroupView {
            return new MobileGroupView(id, align);
        }

        createDropdownView(id:string):DropdownView {
            return new MobileDropdownView(id);
        }

        createLargeDropdownView(id:string):DropdownView {
            return new MobileDropdownView(id);
        }

        createButtonView(id:string):ButtonView {
            return new MobileButtonView(id);
        }
    }

    ToolbarViewFactoryImpl = new MobileToolbarViewFactory();
}



