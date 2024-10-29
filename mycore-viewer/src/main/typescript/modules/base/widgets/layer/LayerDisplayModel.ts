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

import {Layer} from "../../components/model/Layer";


export class LayerDisplayModel {

    constructor() {
    }

    public onLayerAdd = Array<LayerCallback>();
    public onLayerRemove = Array<LayerCallback>();
    public currentPage: string = null;

    public addLayer(layer: Layer) {
        if (this.layerList.indexOf(layer) != -1) {
            throw `the layer ${layer.getId()} is already in model!`;
        }

        this.layerList.push(layer);
        this.onLayerAdd.forEach((callback) => callback(layer));
    }

    public removeLayer(layer: Layer) {
        const layerIndex = this.layerList.indexOf(layer);

        if (layerIndex == -1) {
            throw `the layer ${layer.getId()} is not present in model!`;
        }

        this.layerList.splice(layerIndex, 1);
        this.onLayerRemove.forEach((callback) => callback(layer));
    }

    public getLayerList() {
        return this.layerList.slice(0);
    }

    private layerList = new Array<Layer>();
}

export interface LayerCallback {
    (layer: Layer): void;
}

