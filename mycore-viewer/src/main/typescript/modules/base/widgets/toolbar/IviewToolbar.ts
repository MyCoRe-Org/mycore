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

/// <reference path="../../Utils.ts" />
/// <reference path="../../definitions/jquery.d.ts" />
/// <reference path="../events/ViewerEventManager.ts" />
/// <reference path="../events/ViewerEvent.ts" />
/// <reference path="view/ToolbarViewFactory.ts" />
/// <reference path="model/ToolbarModel.ts" />
/// <reference path="model/ToolbarComponent.ts" />
/// <reference path="view/ToolbarView.ts" />
/// <reference path="model/ToolbarGroup.ts" />
/// <reference path="view/group/GroupView.ts" />
/// <reference path="model/ToolbarButton.ts" />
/// <reference path="control/ButtonController.ts" />
/// <reference path="view/button/ButtonView.ts" />
/// <reference path="model/ToolbarDropdownButton.ts" />
/// <reference path="control/DropdownButtonController.ts" />
/// <reference path="view/dropdown/DropdownView.ts" />
/// <reference path="control/ImageController.ts" />
/// <reference path="model/ToolbarImage.ts" />
/// <reference path="view/image/ImageView.ts" />
/// <reference path="control/TextController.ts" />
/// <reference path="model/ToolbarText.ts" />
/// <reference path="view/text/TextView.ts" />
/// <reference path="control/TextInputController.ts" />


namespace mycore.viewer.widgets.toolbar {

    export class IviewToolbar implements ContainerObserver<ToolbarGroup, ToolbarComponent>, ContainerObserver<ToolbarModel, ToolbarGroup> {

        constructor(private _container:JQuery, private _mobile:boolean = false, private _model:ToolbarModel = new ToolbarModel("default")) {
            this._idViewMap = new MyCoReMap<string, any>();
            this._idGroupViewMap = new MyCoReMap<string, GroupView>();
            this._eventManager = new mycore.viewer.widgets.events.ViewerEventManager();

            this._model.addGroupObserver(this);
            this._model.addObserver(this);

            this._buttonController = new ButtonController(<any>this._idGroupViewMap, this._idViewMap, _mobile);
            this._dropdownController = new DropdownButtonController(<any>this._idGroupViewMap, this._idViewMap,_mobile);
            this._textController = new TextController(<any>this._idGroupViewMap, this._idViewMap, _mobile);
            this._imageController = new ImageController(<any>this._idGroupViewMap, this._idViewMap);
            this._textInputController = new TextInputController(<any>this._idGroupViewMap, this._idViewMap);

            var that = this;
            this._buttonController.eventManager.bind(function ToolbarCallback(e:mycore.viewer.widgets.events.ViewerEvent){
                that.eventManager.trigger(e);
            });

            this._dropdownController.eventManager.bind(function ToolbarCallback(e:mycore.viewer.widgets.events.ViewerEvent){
                that.eventManager.trigger(e);
            });

            this._createView();
        }

        private _eventManager: mycore.viewer.widgets.events.ViewerEventManager;
        private _toolbarElement:JQuery;
        private _toolbarView:ToolbarView;
        private _idViewMap:MyCoReMap<string, any>;
        private _idGroupViewMap:MyCoReMap<string, GroupView>;

        private _buttonController:ButtonController;
        private _dropdownController:DropdownButtonController;
        private _textController:TextController;
        private _imageController:ImageController;
        private _textInputController:TextInputController;

        public get model() {
            return this._model;
        }

        public get eventManager() {
            return this._eventManager;
        }

        private _createView():void {
            this._toolbarView = this.createToolbarView();
            this._toolbarElement = this._toolbarView.getElement();
            this._toolbarElement.appendTo(this._container);
            var groups = this._model.getGroups();
            for (var groupIndex in groups) {
                var group = groups[groupIndex];
                this.childAdded(this._model, group);
            }
            if(this._mobile) {
                this._toolbarView.getElement().trigger("create");
            }
        }

        public childAdded(parent:any, component:any):void {
            if (parent instanceof ToolbarModel) {
                // add a Group to the Model
                var parentModel = <ToolbarModel>parent;
                var childGroup = <ToolbarGroup>component;

                var gv = this.createGroupView(childGroup.name, childGroup.align);
                this._idGroupViewMap.set(childGroup.name, gv);
                this._toolbarView.addChild(gv.getElement());

                var children = childGroup.getComponents();
                children.forEach((child:ToolbarComponent)=>{
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
                    if(this._mobile) {
                        throw new ViewerError("Mobile Toolbar doesnt support Image!");
                    }
                    this._imageController.childAdded(parent, component);
                    return;
                }
            }

        }

        private createToolbarView():ToolbarView {
            return ToolbarViewFactoryImpl.createToolbarView();
        }

        private createGroupView(id:string, align:string):GroupView {
            return ToolbarViewFactoryImpl.createGroupView(id, align);
        }


        public childRemoved(parent:any, component:any):void {
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
        }

        public getView(componentId:string):ToolbarView;

        public getView(component:ToolbarComponent):ToolbarView;

        public getView(component:any):ToolbarView {
            if (component instanceof ToolbarComponent) {
                var toolbarComponent:ToolbarComponent = component;
                var componentId:string = toolbarComponent.getProperty("id").value;
            } else if (typeof component == "string") {
                var componentId:string = component;
            } else {
                return this._toolbarView;
            }

            return this._idViewMap.get(componentId);
        }

    }
} 
