/// <reference path="../../../Utils.ts" />
/// <reference path="../../events/ViewerEventManager.ts" />
/// <reference path="../events/ButtonPressedEvent.ts" />
/// <reference path="../model/ToolbarButton.ts" />
/// <reference path="../model/ToolbarComponent.ts" />
/// <reference path="../model/ToolbarGroup.ts" />
/// <reference path="../view/button/ButtonView.ts" />
/// <reference path="../view/ToolbarViewFactory.ts" />
/// <reference path="../view/group/GroupView.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class ButtonController implements ContainerObserver<ToolbarGroup, ToolbarComponent>, ViewerPropertyObserver<any> {

        constructor(private _groupMap: MyCoReMap<string, GroupView>, private _buttonViewMap: MyCoReMap<string, ButtonView>, private _mobile:boolean) {
            this._eventManager = new mycore.viewer.widgets.events.ViewerEventManager();
        }

        private _eventManager: mycore.viewer.widgets.events.ViewerEventManager;

        public get eventManager() {
            return this._eventManager;
        }

        public childAdded(parent: any, component: any): void {
            var group = <ToolbarGroup>parent;
            var groupView = this._groupMap.get(group.name);
            var componentId = component.getProperty("id").value;
            var button = <ToolbarButton>component;

            var labelProperty = button.getProperty("label");
            var iconProperty = button.getProperty("icon");
            var tooltipProperty = component.getProperty("tooltip");
            var buttonClassProperty = button.getProperty("buttonClass");
            var activeProperty = button.getProperty("active");
            var disabledProperty = button.getProperty("disabled");

            labelProperty.addObserver(this);
            iconProperty.addObserver(this);
            tooltipProperty.addObserver(this);
            buttonClassProperty.addObserver(this);
            activeProperty.addObserver(this);
            disabledProperty.addObserver(this);

            var buttonView = this.createButtonView(button);

            buttonView.updateButtonTooltip(tooltipProperty.value);
            buttonView.updateButtonLabel(labelProperty.value);
            buttonView.updateButtonClass(buttonClassProperty.value);
            buttonView.updateButtonActive(activeProperty.value);
            buttonView.updateButtonDisabled(disabledProperty.value);

            var that = this;
            buttonView.getElement().bind("click", function(e) {
                that._eventManager.trigger(new events.ButtonPressedEvent(button)) ;
            });

            var iconValue = iconProperty.value;
            if (iconValue != null) {
                buttonView.updateButtonIcon(iconValue);
            }


            groupView.addChild(buttonView.getElement());
            this._buttonViewMap.set(componentId, buttonView);

        }

        public childRemoved(parent: any, component: any): void {
            var componentId = component.getProperty("id").value;
            this._buttonViewMap.get(componentId).getElement().remove();

            component.getProperty("label").removeObserver(this);
            component.getProperty("icon").removeObserver(this);
            component.getProperty("tooltip").removeObserver(this);
            component.getProperty("buttonClass").removeObserver(this);
            component.getProperty("active").removeObserver(this);
            component.getProperty("disabled").removeObserver(this);
        }

        public createButtonView(button: ToolbarButton): ButtonView {
            return ToolbarViewFactoryImpl.createButtonView(button.id);
        }

        public propertyChanged(_old: ViewerProperty<any>, _new: ViewerProperty<any>) {
            // Change button
            var buttonId = _new.from.getProperty("id").value;
            if (_old.name == "label" && _new.name == "label") {
                this._buttonViewMap.get(buttonId).updateButtonLabel(_new.value);
                return;
            }

            if (_old.name == "icon" && _new.name == "icon") {
                this._buttonViewMap.get(buttonId).updateButtonIcon(_new.value);
                return;
            }

            if (_old.name == "tooltip" && _new.name == "tooltip") {
                this._buttonViewMap.get(buttonId).updateButtonTooltip(_new.value);
                return;
            }

            if (_old.name == "buttonClass" && _new.name == "buttonClass") {
                this._buttonViewMap.get(buttonId).updateButtonClass(_new.value);
                return;
            }

            if (_old.name == "active" && _new.name == "active") {
                this._buttonViewMap.get(buttonId).updateButtonActive(_new.value);
                return;
            }

            if (_old.name == "disabled" && _new.name == "disabled") {
                this._buttonViewMap.get(buttonId).updateButtonDisabled(_new.value);
                return;
            }
        }
    }

}