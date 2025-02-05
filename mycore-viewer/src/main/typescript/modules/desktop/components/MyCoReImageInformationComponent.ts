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


import { ViewerComponent } from "../../base/components/ViewerComponent";
import { MyCoReViewerSettings } from "../../base/MyCoReViewerSettings";
import { Utils, ViewerProperty } from "../../base/Utils";
import { ComponentInitializedEvent } from "../../base/components/events/ComponentInitializedEvent";
import { ShowContentEvent } from "../../base/components/events/ShowContentEvent";
import { WaitForEvent } from "../../base/components/events/WaitForEvent";
import { PageLayoutChangedEvent } from "../../base/components/events/PageLayoutChangedEvent";
import { ImageChangedEvent } from "../../base/components/events/ImageChangedEvent";
import { ViewportInitializedEvent } from "../../base/components/events/ViewportInitializedEvent";
import { StructureModelLoadedEvent } from "../../base/components/events/StructureModelLoadedEvent";
import { PageLayout } from "../../base/widgets/canvas/PageLayout";
import { ViewerEvent } from "../../base/widgets/events/ViewerEvent";

export class MyCoReImageInformationComponent extends ViewerComponent {

  constructor(private _settings: MyCoReViewerSettings) {
    super();
  }

  public init() {
    this._informationBar = document.createElement("div");
    this._informationBar.classList.add("informationBar");

    this._scale = document.createElement("span");
    this._scale.classList.add("scale");
    this._informationBar.append(this._scale);

    this._scaleEditForm = document.createElement("div")
    this._scaleEditForm.classList.add("form-group");

    this._scaleEdit = document.createElement("input");
    this._scaleEdit.setAttribute("type", "text");
    this._scaleEdit.classList.add("scale", "form-control");
    this._scaleEditForm.append(this._scaleEdit);
    this.initScaleChangeLogic();

    this._informationBar.classList.add("card");
    this._imageLabel = document.createElement("span");
    this._imageLabel.classList.add("imageLabel");
    this._informationBar.append(this._imageLabel);

    this._imageLabel.addEventListener('mousedown', Utils.stopPropagation);
    this._imageLabel.addEventListener('mouseup', Utils.stopPropagation);
    this._imageLabel.addEventListener('mousemove', Utils.stopPropagation);

    this._rotation = document.createElement("span");
    this._rotation.innerText = "0 °";
    this._rotation.classList.add("rotation");
    this._informationBar.append(this._rotation);

    this.trigger(new ComponentInitializedEvent(this));
    this.trigger(new ShowContentEvent(this, this._informationBar, ShowContentEvent.DIRECTION_SOUTH, 30));
    this.trigger(new WaitForEvent(this, PageLayoutChangedEvent.TYPE));
    this.trigger(new WaitForEvent(this, ImageChangedEvent.TYPE));
    this.trigger(new WaitForEvent(this, ViewportInitializedEvent.TYPE));
    this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));

    // TODO: find workarround for this hack (we need to know when this._pageLayout.getCurrentPageZoom() returns the real val)
  }

  private _informationBar: HTMLElement;
  private _imageLabel: HTMLElement;
  private _rotation: HTMLElement;
  private _scale: HTMLElement;

  private _scaleEditForm: HTMLElement;
  private _scaleEdit: HTMLInputElement;

  private _pageLayout: PageLayout;

  private _currentZoom = -1;
  private _currentRotation = -1;

  private initScaleChangeLogic() {
    this._scale.addEventListener('click', () => {
      this._scale.remove();
      this._scaleEdit.value = this._pageLayout.getCurrentPageZoom() * 100 + "";
      this._informationBar.append(this._scaleEdit);
      Utils.selectElementText(this._scaleEdit);

      this._scaleEdit.addEventListener('keyup', (ev) => {
        const isValid = this.validateScaleEdit();

        if (ev.keyCode == 13) {
          if (isValid) {
            this.applyNewZoom();
            this.endEdit();
          }
        } else if (ev.keyCode == 27) {
          this.endEdit();
        }
      });
    });
  }

  private endEdit() {
    this._scaleEdit.remove();
    this._informationBar.append(this._scale);
  }

  private applyNewZoom() {
    const zoom = parseFloat((this._scaleEdit.value + "").trim());
    if (typeof this._pageLayout != "undefined" && this._pageLayout != null) {
      this._pageLayout.setCurrentPageZoom(zoom / 100)
    }
  }

  public validateScaleEdit() {
    const zoom = parseFloat((this._scaleEdit.value + "").trim());

    if (isNaN(zoom)) {
      this._scaleEdit.classList.add("error");
      return false;
    }

    const zoomNumber = (zoom * 1);
    if (zoomNumber < 50 || zoomNumber > 400) {
      this._scaleEdit.classList.add("error");
      return false;
    }

    this._scaleEdit.classList.remove("error");
    return true;
  }

  public get handlesEvents(): string[] {
    const handles = new Array<any>();
    handles.push(ImageChangedEvent.TYPE);
    handles.push(StructureModelLoadedEvent.TYPE);
    handles.push(ViewportInitializedEvent.TYPE);
    handles.push(PageLayoutChangedEvent.TYPE);

    return handles;
  }

  public handle(e: ViewerEvent): void {
    if (e.type == StructureModelLoadedEvent.TYPE) {
      this.updateLayoutInformation();
    }

    if (e.type == ViewportInitializedEvent.TYPE) {
      const vie = e as ViewportInitializedEvent;

      vie.viewport.scaleProperty.addObserver({
        propertyChanged: (oldScale: ViewerProperty<number>, newScale: ViewerProperty<number>) => {
          this.updateLayoutInformation();
        }
      });

      vie.viewport.rotationProperty.addObserver({
        propertyChanged: (oldRotation: ViewerProperty<number>, newRotation: ViewerProperty<number>) => {
          this.updateLayoutInformation();
        }
      });

      vie.viewport.positionProperty.addObserver({
        propertyChanged: (oldPos, newPos) => {
          this.updateLayoutInformation();
        }
      });

      vie.viewport.sizeProperty.addObserver({
        propertyChanged: (oldSize, newSize) => {
          this.updateLayoutInformation();
        }
      });
    }

    if (e.type == PageLayoutChangedEvent.TYPE) {
      const plce = e as PageLayoutChangedEvent;
      this._pageLayout = plce.pageLayout;
      this.updateLayoutInformation();
    }


    if (e.type == ImageChangedEvent.TYPE) {
      const imageChangedEvent = e as ImageChangedEvent;
      if (typeof imageChangedEvent.image != "undefined" && imageChangedEvent.image != null) {
        let text = (imageChangedEvent.image.orderLabel || imageChangedEvent.image.order)+"";
        if (imageChangedEvent.image.uniqueIdentifier != null) {
          text += " - " + imageChangedEvent.image.uniqueIdentifier;
        }
        this._imageLabel.innerText = text;
      }
      this.updateLayoutInformation();
    }

  }

  public updateLayoutInformation() {
    if (typeof this._pageLayout != "undefined") {
      const currentPageZoom = this._pageLayout.getCurrentPageZoom();
      if (this._currentZoom != currentPageZoom) {
        this._scale.innerText = Math.round(currentPageZoom * 100) + "%";
        this._currentZoom = currentPageZoom;
      }

      const currentPageRotation = this._pageLayout.getCurrentPageRotation();
      if (this._currentRotation != currentPageRotation) {
        this._rotation.innerText = currentPageRotation + " °";
        this._currentRotation = currentPageRotation;
      }

    }
  }

  public get container() {
    return this._informationBar;
  }
}



