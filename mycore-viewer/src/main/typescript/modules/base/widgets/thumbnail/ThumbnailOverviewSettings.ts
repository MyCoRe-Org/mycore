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

import { ThumbnailOverviewThumbnail } from "./ThumbnailOverviewThumbnail";
import { Size2D } from "../../Utils";
import { ThumbnailOverviewInputHandler } from "./ThumbnailOverviewInputHandler";

export interface ThumbnailOverviewSettings {
  /**
   * Array of all Thumbnails
   */
  thumbnails: Array<ThumbnailOverviewThumbnail>;

  /**
   * Container needs a size(width/height).
   */
  container: HTMLElement;

  /**
   * Maximal Size a Thumbnail should displayed. (this should include the gaps between thumbnails)
   */
  maxThumbnailSize: Size2D;

  inputHandler: ThumbnailOverviewInputHandler;
}

export class DefaultThumbnailOverviewSettings implements ThumbnailOverviewSettings {
  constructor(private _thumbnails: Array<ThumbnailOverviewThumbnail>,
    private _container: HTMLElement,
    private _inputHandler: ThumbnailOverviewInputHandler = {
      addedThumbnail: function(id: string, element: HTMLElement) {
      }
    },
    private _maxThumbnailSize = new Size2D(255, 255)) {
  }

  public get thumbnails() {
    return this._thumbnails;
  }

  public get container() {
    return this._container;
  }

  public get maxThumbnailSize() {
    return this._maxThumbnailSize;
  }

  public get inputHandler(): ThumbnailOverviewInputHandler {
    return this._inputHandler;
  }

}

