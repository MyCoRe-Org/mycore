/// <reference path="../../../Utils.ts" />
/// <reference path="../model/ToolbarImage.ts" />
/// <reference path="../model/ToolbarComponent.ts" />
/// <reference path="../model/ToolbarGroup.ts" />
/// <reference path="../view/image/ImageView.ts" />
/// <reference path="../view/group/GroupView.ts" />
/// <reference path="../view/ToolbarViewFactory.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class ImageController implements ContainerObserver<ToolbarGroup, ToolbarComponent>, ViewerPropertyObserver<any> {

        constructor(private _groupMap: MyCoReMap<string, GroupView>, private _textViewMap: MyCoReMap<string, ImageView>) {
        }

        public childAdded(parent: any, component: any): void {
            var group = <ToolbarGroup>parent;
            var groupView = this._groupMap.get(group.name);
            var componentId = component.getProperty("id").value;
            var text = <ToolbarImage>component;
 
            var imageView = this.createImageView(componentId);

            var hrefProperty = text.getProperty("href");
            hrefProperty.addObserver(this);
            imageView.updateHref(hrefProperty.value);

            groupView.addChild(imageView.getElement());

            this._textViewMap.set(componentId, imageView);
        }

        public childRemoved(parent: any, component: any): void {
            var componentId = component.getProperty("id").value;
            this._textViewMap.get(componentId).getElement().remove();
            
             component.getProperty("href").removeObserver(this);
        }

        public propertyChanged(_old: ViewerProperty<any>, _new: ViewerProperty<any>) {
            var textId = _new.from.getProperty("id").value;
            if (_old.name == "href" && _new.name == "href") {
                this._textViewMap.get(textId).updateHref(_new.value);
            }
        }

        public createImageView(id: string): ImageView {
            return ToolbarViewFactoryImpl.createImageView(id);
        }

    }
}