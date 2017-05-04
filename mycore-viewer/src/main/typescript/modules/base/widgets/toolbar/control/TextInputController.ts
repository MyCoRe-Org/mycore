/// <reference path="../../../Utils.ts" />
/// <reference path="../model/ToolbarTextInput.ts" />
/// <reference path="../model/ToolbarComponent.ts" />
/// <reference path="../model/ToolbarGroup.ts" />
/// <reference path="../view/input/TextInputView.ts" />
/// <reference path="../view/group/GroupView.ts" />
/// <reference path="../view/ToolbarViewFactory.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class TextInputController implements ContainerObserver<ToolbarGroup, ToolbarComponent>, ViewerPropertyObserver<any> {

        constructor(private _groupMap: MyCoReMap<string, GroupView>, private _textInputViewMap: MyCoReMap<string, TextInputView>) {
        }

        public childAdded(parent: any, component: any): void {
            var group = <ToolbarGroup>parent;
            var groupView = this._groupMap.get(group.name);
            var componentId = component.getProperty("id").value;
            var text = <ToolbarTextInput>component;

            var textView = this.createTextInputView(componentId);

            var valueProperty = text.getProperty("value");
            valueProperty.addObserver(this);
            textView.updateValue(valueProperty.value);

            var placeHolderProperty = text.getProperty("placeHolder");
            placeHolderProperty.addObserver(this);
            textView.updatePlaceholder(placeHolderProperty.value);

            groupView.addChild(textView.getElement());

            textView.onChange = ()=>{
                if (textView.getValue() != valueProperty.value) {
                    valueProperty.value = textView.getValue();
                }
            };

            this._textInputViewMap.set(componentId, textView);
        }

        public childRemoved(parent: any, component: any): void {
            var componentId = component.getProperty("id").value;
            this._textInputViewMap.get(componentId).getElement().remove();

            component.getProperty("value").removeObserver(this);
            component.getProperty("placeHolder").removeObserver(this);
        }

        public propertyChanged(_old: ViewerProperty<any>, _new: ViewerProperty<any>) {
            var textId = _new.from.getProperty("id").value;
                var textInputView = this._textInputViewMap.get(textId);
            if (_old.name == "value" && _new.name == "value") {
                if (textInputView.getValue() != _new.value) {
                    textInputView.updateValue(_new.value);
                }
            }

            if(_old.name == "placeHolder" && _new.name == "placeHolder"){
                if(textInputView.getValue() != _new.value){
                    textInputView.updatePlaceholder(_new.value);
                }
            }
        }

        public createTextInputView(id: string): TextInputView {
            return ToolbarViewFactoryImpl.createTextInputView(id);
        }

    }
}
