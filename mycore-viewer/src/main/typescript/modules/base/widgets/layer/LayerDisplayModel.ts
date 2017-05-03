/// <reference path="../../components/model/Layer.ts" />
/// <reference path="../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.layer {

    export class LayerDisplayModel {

        constructor() {
        }

        public onLayerAdd = Array<LayerCallback>();
        public onLayerRemove = Array<LayerCallback>();
        public currentPage:string = null;

        public addLayer(layer:model.Layer) {
            if (this.layerList.indexOf(layer) != -1) {
                throw `the layer ${layer.getId()} is already in model!`;
            }

            this.layerList.push(layer);
            this.onLayerAdd.forEach((callback)=>callback(layer));
        }

        public removeLayer(layer:model.Layer) {
            var layerIndex = this.layerList.indexOf(layer);

            if (layerIndex == -1) {
                throw `the layer ${layer.getId()} is not present in model!`;
            }

            this.layerList.splice(layerIndex, 1);
            this.onLayerRemove.forEach((callback)=>callback(layer));
        }

        public getLayerList() {
            return this.layerList.slice(0);
        }

        private layerList = new Array<model.Layer>();
    }

    export interface LayerCallback {
        (layer:model.Layer):void;
    }
}