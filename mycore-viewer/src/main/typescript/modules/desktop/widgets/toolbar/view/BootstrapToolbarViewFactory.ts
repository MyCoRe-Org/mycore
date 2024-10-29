/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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


import {ToolbarViewFactory} from "../../../../base/widgets/toolbar/view/ToolbarViewFactory";
import {ToolbarView} from "../../../../base/widgets/toolbar/view/ToolbarView";
import {TextView} from "../../../../base/widgets/toolbar/view/text/TextView";
import {ImageView} from "../../../../base/widgets/toolbar/view/image/ImageView";
import {GroupView} from "../../../../base/widgets/toolbar/view/group/GroupView";
import {DropdownView} from "../../../../base/widgets/toolbar/view/dropdown/DropdownView";
import {ButtonView} from "../../../../base/widgets/toolbar/view/button/ButtonView";
import {TextInputView} from "../../../../base/widgets/toolbar/view/input/TextInputView";
import {BootstrapToolbarView} from "./BootstrapToolbarView";
import {BootstrapTextView} from "./text/BootstrapTextView";
import {BootstrapImageView} from "./image/BootstrapImageView";
import {BootstrapGroupView} from "./group/BootstrapGroupView";
import {BootstrapDropdownView} from "./dropdown/BootstrapDropdownView";
import {BootstrapLargeDropdownView} from "./dropdown/BootstrapLargeDropdownView";
import {BootstrapButtonView} from "./button/BootstrapButtonView";
import {BootstrapTextInputView} from "./input/BootstrapTextInputView";

export class BootstrapToolbarViewFactory implements ToolbarViewFactory {

    createToolbarView(): ToolbarView {
        return new BootstrapToolbarView();
    }

    createTextView(id: string): TextView {
        return new BootstrapTextView(id);
    }

    createImageView(id: string): ImageView {
        return new BootstrapImageView(id);
    }

    createGroupView(id: string, order: number, align: string): GroupView {
        return new BootstrapGroupView(id, order, align);
    }

    createDropdownView(id: string): DropdownView {
        return new BootstrapDropdownView(id);
    }

    createLargeDropdownView(id: string): DropdownView {
        return new BootstrapLargeDropdownView(id);
    }

    createButtonView(id: string): ButtonView {
        return new BootstrapButtonView(id);
    }

    createTextInputView(id: string): TextInputView {
        return new BootstrapTextInputView(id);
    }
}

export function register(){
    ((window as any).ToolbarViewFactoryImpl as ToolbarViewFactory)=new BootstrapToolbarViewFactory();
}


