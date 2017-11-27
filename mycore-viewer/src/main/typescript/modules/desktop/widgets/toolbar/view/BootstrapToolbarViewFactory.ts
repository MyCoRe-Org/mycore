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

/// <reference path="text/BootstrapTextView.ts" />
/// <reference path="image/BootstrapImageView.ts" />
/// <reference path="group/BootstrapGroupView.ts" />
/// <reference path="dropdown/BootstrapDropdownView.ts" />
/// <reference path="dropdown/BootstrapLargeDropdownView.ts" />
/// <reference path="button/BootstrapButtonView.ts" />
/// <reference path="input/BootstrapTextInputView.ts" />
/// <reference path="BootstrapToolbarView.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class BootstrapToolbarViewFactory implements ToolbarViewFactory {

        createToolbarView():ToolbarView {
            return new BootstrapToolbarView();
        }

        createTextView(id:string):TextView {
            return new BootstrapTextView(id);
        }

        createImageView(id:string):ImageView {
            return new BootstrapImageView(id);
        }

        createGroupView(id:string, align:string):GroupView {
            return new BootstrapGroupView(id, align);
        }

        createDropdownView(id:string):DropdownView {
            return new BootstrapDropdownView(id);
        }

        createLargeDropdownView(id:string):DropdownView {
            return new BootstrapLargeDropdownView(id);
        }

        createButtonView(id:string):ButtonView {
            return new BootstrapButtonView(id);
        }

        createTextInputView(id: string): TextInputView {
            return new BootstrapTextInputView(id);
        }
    }

    ToolbarViewFactoryImpl = new BootstrapToolbarViewFactory();
}



