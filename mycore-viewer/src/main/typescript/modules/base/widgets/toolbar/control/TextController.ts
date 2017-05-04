/// <reference path="../../../Utils.ts" />
/// <reference path="../model/ToolbarText.ts" />
/// <reference path="../model/ToolbarComponent.ts" />
/// <reference path="../model/ToolbarGroup.ts" />
/// <reference path="../view/text/TextView.ts" />
/// <reference path="../view/group/GroupView.ts" />
/// <reference path="../view/ToolbarViewFactory.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class TextController implements ContainerObserver<ToolbarGroup, ToolbarComponent>, ViewerPropertyObserver<any> {

        constructor(private _groupMap:MyCoReMap<string, GroupView>, private _textViewMap:MyCoReMap<string, TextView>, private _mobile:boolean) {
        }

        public childAdded(parent:any, component:any):void {
            var group = <ToolbarGroup>parent;
            var groupView = this._groupMap.get(group.name);
            var componentId = component.getProperty("id").value;
            var text = <ToolbarText>component;

            var textView = this.createTextView(componentId);

            var textProperty = text.getProperty("text");
            textProperty.addObserver(this);
            textView.updateText(textProperty.value);

            groupView.addChild(textView.getElement());

            this._textViewMap.set(componentId, textView);
        }

        public childRemoved(parent:any, component:any):void {
            var componentId = component.getProperty("id").value;
            this._textViewMap.get(componentId).getElement().remove();

            component.getProperty("text").removeObserver(this);
        }

        public propertyChanged(_old:ViewerProperty<any>, _new:ViewerProperty<any>) {
            var textId = _new.from.getProperty("id").value;
            if (_old.name == "text" && _new.name == "text") {
                this._textViewMap.get(textId).updateText(_new.value);
            }
        }

        public createTextView(id:string):TextView {
            return ToolbarViewFactoryImpl.createTextView(id);
        }

    }
}