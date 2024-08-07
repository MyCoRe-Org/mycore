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

import {ContainerObserver, MyCoReMap, ViewerError} from "../../Utils";
import {ToolbarGroup} from "./model/ToolbarGroup";
import {ToolbarComponent} from "./model/ToolbarComponent";
import {ToolbarModel} from "./model/ToolbarModel";
import {GroupView} from "./view/group/GroupView";
import {ButtonController} from "./control/ButtonController";
import {TextController} from "./control/TextController";
import {DropdownButtonController} from "./control/DropdownButtonController";
import {ImageController} from "./control/ImageController";
import {TextInputController} from "./control/TextInputController";
import {ToolbarView} from "./view/ToolbarView";
import {ToolbarDropdownButton} from "./model/ToolbarDropdownButton";
import {ToolbarButton} from "./model/ToolbarButton";
import {ToolbarText} from "./model/ToolbarText";
import {ToolbarTextInput} from "./model/ToolbarTextInput";
import {ToolbarImage} from "./model/ToolbarImage";
import {ToolbarViewFactory} from "./view/ToolbarViewFactory";
import {ViewerEvent} from "../events/ViewerEvent";
import {ViewerEventManager} from "../events/ViewerEventManager";


export class IviewToolbar implements ContainerObserver<ToolbarGroup, ToolbarComponent>, ContainerObserver<ToolbarModel, ToolbarGroup> {

    constructor(private _container: JQuery, private _mobile: boolean = false, private _model: ToolbarModel = new ToolbarModel("default")) {
        this._idViewMap = new MyCoReMap<string, any>();
        this._idGroupViewMap = new MyCoReMap<string, GroupView>();
        this._eventManager = new ViewerEventManager();

        this._model.addGroupObserver(this);
        this._model.addObserver(this);

        this._buttonController = new ButtonController(this._idGroupViewMap, this._idViewMap, _mobile);
        this._dropdownController = new DropdownButtonController(this._idGroupViewMap, this._idViewMap, _mobile);
        this._textController = new TextController(this._idGroupViewMap, this._idViewMap, _mobile);
        this._imageController = new ImageController(this._idGroupViewMap, this._idViewMap);
        this._textInputController = new TextInputController(this._idGroupViewMap, this._idViewMap);

        this._buttonController.eventManager.bind((e: ViewerEvent) => {
            this.eventManager.trigger(e);
        });

        this._dropdownController.eventManager.bind((e: ViewerEvent) => {
            this.eventManager.trigger(e);
        });

        this._createView();
    }

    private _eventManager: ViewerEventManager;
    private _toolbarElement: JQuery;
    private _toolbarView: ToolbarView;
    private _idViewMap: MyCoReMap<string, any>;
    private _idGroupViewMap: MyCoReMap<string, GroupView>;

    private _buttonController: ButtonController;
    private _dropdownController: DropdownButtonController;
    private _textController: TextController;
    private _imageController: ImageController;
    private _textInputController: TextInputController;

    public get model() {
        return this._model;
    }

    public get eventManager() {
        return this._eventManager;
    }

    private _createView(): void {
        this._toolbarView = this.createToolbarView();
        this._toolbarElement = this._toolbarView.getElement();
        this._toolbarElement.appendTo(this._container);
        const groups = this._model.getGroups();
        for (const group of groups) {
            this.childAdded(this._model, group);
        }
        if (this._mobile) {
            this._toolbarView.getElement().trigger("create");
        }
    }

    public childAdded(parent: any, component: any): void {
        if (parent instanceof ToolbarModel) {
            // add a Group to the Model
            const childGroup = component as ToolbarGroup;

            const gv = this.createGroupView(childGroup.name, childGroup.order, childGroup.align);
            this._idGroupViewMap.set(childGroup.name, gv);
            this._toolbarView.addChild(gv.getElement());

            const children = childGroup.getComponents();
            children.forEach((child: ToolbarComponent) => {
                childGroup.notifyObserverChildAdded(childGroup, child);
            });

        } else if (parent instanceof ToolbarGroup) {
            // Split events to the Component Controller

            // needs to be before ToolbarButton because inheritance
            if (component instanceof ToolbarDropdownButton) {
                this._dropdownController.childAdded(parent, component);
                return;
            }

            if (component instanceof ToolbarButton) {
                this._buttonController.childAdded(parent, component);
                return;
            }

            if (component instanceof ToolbarText) {
                this._textController.childAdded(parent, component);
                return;
            }

            if (component instanceof ToolbarTextInput) {
                this._textInputController.childAdded(parent, component);
                return;
            }

            if (component instanceof ToolbarImage) {
                if (this._mobile) {
                    throw new ViewerError("Mobile Toolbar doesnt support Image!");
                }
                this._imageController.childAdded(parent, component);
                return;
            }
        }

    }

    private createToolbarView(): ToolbarView {
        return ((window as any).ToolbarViewFactoryImpl as ToolbarViewFactory).createToolbarView();
    }

    private createGroupView(id: string, order: number, align: string): GroupView {
        return ((window as any).ToolbarViewFactoryImpl as ToolbarViewFactory).createGroupView(id, order, align);
    }


    public childRemoved(parent: any, component: any): void {
        if (component instanceof ToolbarDropdownButton) {
            this._dropdownController.childRemoved(parent, component);
            return;
        }

        if (component instanceof ToolbarButton) {
            this._buttonController.childRemoved(parent, component);
            return;
        }

        if (component instanceof ToolbarText) {
            this._textController.childRemoved(parent, component);
            return;
        }

        if (component instanceof ToolbarImage) {
            this._imageController.childRemoved(parent, component);
            return;
        }

        if (component instanceof ToolbarTextInput) {
            this._textInputController.childRemoved(parent, component);
            return;
        }

        if (component instanceof ToolbarGroup) {
            const element = this._idGroupViewMap.get(component.name).getElement();
            element.remove();
            return;
        }
    }

    public getView(componentId: string): ToolbarView;

    public getView(component: ToolbarComponent): ToolbarView;

    public getView(component: any): ToolbarView {
        let componentId: string;
        if (component instanceof ToolbarComponent) {
            const toolbarComponent: ToolbarComponent = component;
            componentId = toolbarComponent.getProperty("id").value;
        } else if (typeof component == "string") {
            componentId = component;
        } else {
            return this._toolbarView;
        }

        return this._idViewMap.get(componentId);
    }

}

