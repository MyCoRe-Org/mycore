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

import {ToolbarView} from "./ToolbarView";
import {TextView} from "./text/TextView";
import {ImageView} from "./image/ImageView";
import {GroupView} from "./group/GroupView";
import {DropdownView} from "./dropdown/DropdownView";
import {ButtonView} from "./button/ButtonView";
import {TextInputView} from "./input/TextInputView";

export interface ToolbarViewFactory {
    createToolbarView(): ToolbarView;

    createTextView(id: string): TextView;

    createImageView(id: string): ImageView;

    createGroupView(id: string, order: number, align: string): GroupView;

    createDropdownView(id: string): DropdownView;

    createLargeDropdownView(id: string): DropdownView;

    createButtonView(id: string): ButtonView;

    createTextInputView(id: string): TextInputView;
}



