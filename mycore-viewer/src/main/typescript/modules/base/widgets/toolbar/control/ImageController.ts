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
