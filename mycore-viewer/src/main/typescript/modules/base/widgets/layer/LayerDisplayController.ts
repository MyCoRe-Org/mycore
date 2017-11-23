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

/// <reference path="../../components/model/Layer.ts" />
/// <reference path="../../definitions/jquery.d.ts" />
/// <reference path="LayerDisplayModel.ts" />

namespace mycore.viewer.widgets.layer {

    export class LayerDisplayController {
        constructor(private _container:JQuery, private languageResolver:(id:string)=>string) {
            this.initializeView();
            this.initializeModel();
        }

        private initializeView() {
            this._view = jQuery("<div></div>");
            this._view.addClass("layer-view");
            this._view.appendTo(this._container);
        }

        private _view:JQuery;
        private _layerIdViewMap = new MyCoReMap<string, JQuery>();
        private static REMOVE_EXCLUDE_CLASS = "rm-exclude";

        // used to check if callback is 2 old!
        private _layerIdCallbackMap = new MyCoReMap<string, (success:boolean, content:JQuery)=>void>();

        private addLayerView(layer:model.Layer):void {
            var id = layer.getId();
            var layerView;

            if (!this._layerIdViewMap.has(id)) {
                layerView = this.createLayerView(layer);
            } else {
                layerView = this._layerIdViewMap.get(id);
            }

            this._view.append(layerView);
        }

        private createLayerView(layer:model.Layer) {
            var id = layer.getId();
            var label = this.languageResolver(layer.getLabel());

            var layerView = jQuery(`<div data-id='layer-${id}' class='layer'></div>`);
            var layerHeading = jQuery(`<div class="layer-heading ${LayerDisplayController.REMOVE_EXCLUDE_CLASS}">${label}</div>`);
            layerHeading.appendTo(layerView);

            this._layerIdViewMap.set(id, layerView);

            return layerView;
        }

        private  removeLayerView(layer:model.Layer):void {
            this.getLayerView(layer.getId()).detach();
        }

        private getLayerView(id:string):JQuery {
            return this._layerIdViewMap.get(id);
        }

        /**
         * This method removes everything from a layer view.
         * @param id
         */
        private cleanLayerView(id:string) {
            this._layerIdViewMap.get(id).children().not("." + LayerDisplayController.REMOVE_EXCLUDE_CLASS).detach();
        }

        private initializeModel() {
            this.model.onLayerAdd.push((layer:model.Layer)=> {
                this.addLayerView(layer);
            });

            this.model.onLayerRemove.push((layer:model.Layer)=> {
                this.removeLayerView(layer);
            });
        }

        private model:LayerDisplayModel = new LayerDisplayModel();

        /**
         * Adds a Layer to display.
         * @param layer the layer to add.
         */
        public addLayer(layer:model.Layer) {
            this.model.addLayer(layer);
            this.synchronizeView();
        }

        /**
         * Removes a Layer which is currently displayed.
         * @param layer the layer to remove.
         */
        public removeLayer(layer:model.Layer){
            this.model.removeLayer(layer);
            this.synchronizeView();
        }


        public getLayer(){
            return this.model.getLayerList();
        }

        /**
         * Should be called if the page changes. Requests new content for every layer.
         * @param newHref the href of the new page
         */
        public pageChanged(newHref:string) {
            this.model.currentPage = newHref;
            this.synchronizeView();
        }

        private synchronizeView() {
            this.model.getLayerList().forEach((currentDisplayedLayer:model.Layer)=> {
                var layerId = currentDisplayedLayer.getId();
                this.cleanLayerView(currentDisplayedLayer.getId()); // dont show old content while loading!

                var onResolve = (success:boolean, content?:JQuery)=> {
                    if (success && /* check if the last registered callback matches the current*/
                        this._layerIdCallbackMap.has(layerId) &&
                        this._layerIdCallbackMap.get(layerId) == onResolve) {

                        content.find(".popupTrigger").each(function (i, popupTrigger) {
                            var popup = <any>jQuery(popupTrigger);
                            popup.attr("data-placement", "bottom");
                            popup.popover({
                                html : true,
                                content : function () {
                                    return popup.find(".popupBox").html();
                                }
                            });
                        });
                        this.getLayerView(currentDisplayedLayer.getId()).append(content);
                        this._layerIdCallbackMap.remove(layerId); // remove this callback
                    }
                };

                if (this._layerIdCallbackMap.has(layerId)) {
                    this._layerIdCallbackMap.remove(layerId);
                }
                this._layerIdCallbackMap.set(layerId, onResolve);
                currentDisplayedLayer.resolveLayer(this.model.currentPage, onResolve);
            });
        }
    }

}
