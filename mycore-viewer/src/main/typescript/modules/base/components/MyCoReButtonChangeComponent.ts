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


import { ViewerComponent } from "./ViewerComponent";
import { MyCoReViewerSettings } from "../MyCoReViewerSettings";
import { StructureModelLoadedEvent } from "./events/StructureModelLoadedEvent";
import { ImageChangedEvent } from "./events/ImageChangedEvent";
import { ProvideToolbarModelEvent } from "./events/ProvideToolbarModelEvent";
import { StructureModel } from "./model/StructureModel";
import { ToolbarButton } from "../widgets/toolbar/model/ToolbarButton";
import { StructureImage } from "./model/StructureImage";
import { Utils } from "../Utils";
import { ViewerEvent } from "../widgets/events/ViewerEvent";

export class MyCoReButtonChangeComponent extends ViewerComponent {

  constructor(private _settings: MyCoReViewerSettings) {
    super()
  }

  public init() {

  }

  public get handlesEvents(): string[] {
    const handles = new Array<string>();

    handles.push(StructureModelLoadedEvent.TYPE);
    handles.push(ImageChangedEvent.TYPE);
    handles.push(ProvideToolbarModelEvent.TYPE);

    return handles;
  }

  private _nextImageButton: ToolbarButton = null;
  private _previousImageButton: ToolbarButton = null;
  private _structureModel: StructureModel = null;
  private _currentImage: StructureImage = null;

  private _checkAndDisableSynchronize = Utils.synchronize<MyCoReButtonChangeComponent>([
    (context: MyCoReButtonChangeComponent) => context._nextImageButton != null,
    (context: MyCoReButtonChangeComponent) => context._previousImageButton != null,
    (context: MyCoReButtonChangeComponent) => context._structureModel != null,
    (context: MyCoReButtonChangeComponent) => context._currentImage != null
  ], (context) => {
    const positionOfImage = this._structureModel._imageList.indexOf(this._currentImage);
    if (positionOfImage == 0) {
      this._previousImageButton.disabled = true;
    } else {
      this._previousImageButton.disabled = false;
    }

    if (positionOfImage == this._structureModel.imageList.length - 1) {
      this._nextImageButton.disabled = true;
    } else {
      this._nextImageButton.disabled = false;
    }
  });

  public handle(e: ViewerEvent): void {
    if (e.type == ProvideToolbarModelEvent.TYPE) {
      const ptme = e as ProvideToolbarModelEvent;
      this._nextImageButton = ptme.model._nextImageButton;
      this._previousImageButton = ptme.model._previousImageButton;
      if (this._structureModel == null) {
        this._nextImageButton.disabled = true;
        this._previousImageButton.disabled = true;
      }
      this._checkAndDisableSynchronize(this);
    }

    if (e.type == StructureModelLoadedEvent.TYPE) {
      const structureModelLoadedEvent = e as StructureModelLoadedEvent;
      this._structureModel = structureModelLoadedEvent.structureModel;
      this._nextImageButton.disabled = false;
      this._previousImageButton.disabled = false;
      this._checkAndDisableSynchronize(this);
    }

    if (e.type == ImageChangedEvent.TYPE) {
      const imageChangedEvent = e as ImageChangedEvent;
      if (imageChangedEvent.image != null) {
        this._currentImage = imageChangedEvent.image;
        this._checkAndDisableSynchronize(this);
      }
    }
  }
}
