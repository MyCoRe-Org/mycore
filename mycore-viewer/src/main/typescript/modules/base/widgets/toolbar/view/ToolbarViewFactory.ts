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

/// <reference path="text/TextView.ts" />
/// <reference path="image/ImageView.ts" />
/// <reference path="group/GroupView.ts" />
/// <reference path="dropdown/DropdownView.ts" />
/// <reference path="button/ButtonView.ts" />
/// <reference path="input/TextInputView.ts" />
/// <reference path="ToolbarView.ts" />

namespace mycore.viewer.widgets.toolbar {
    export interface ToolbarViewFactory {
        createToolbarView():ToolbarView;
        createTextView(id:string):TextView;
        createImageView(id:string):ImageView;
        createGroupView(id:string, align:string):GroupView;
        createDropdownView(id:string):DropdownView;
        createLargeDropdownView(id:string):DropdownView;
        createButtonView(id:string):ButtonView;
        createTextInputView(id: string): TextInputView;
    }

    export var ToolbarViewFactoryImpl: ToolbarViewFactory;
}



