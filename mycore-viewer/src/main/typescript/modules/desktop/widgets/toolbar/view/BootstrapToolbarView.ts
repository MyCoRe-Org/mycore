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


import {ToolbarView} from "../../../../base/widgets/toolbar/view/ToolbarView";

export class BootstrapToolbarView implements ToolbarView {

  private _toolbar: HTMLElement;
  private _toolbarContainer: HTMLElement;

  constructor() {
    this._toolbar = document.createElement('nav');
    this._toolbar.classList.add('navbar', 'navbar-expand-lg', 'navbar-light', 'bg-light');

    this._toolbarContainer = document.createElement('div');
    this._toolbarContainer.classList.add('container-fluid');
    this._toolbar.append(this._toolbarContainer);
  }

  //navbar-header
  public addChild(child: HTMLElement): void {
    this._toolbarContainer.append(child);
    this._toolbarContainer.querySelectorAll('.btn-group.right.ms-auto')
        .forEach(el => el.classList.remove('ms-auto'));

    const sortedElements = Array.from(this._toolbarContainer.querySelectorAll('.btn-group.right[data-tb-order]'))
      .sort((a, b) => {
        const orderA = parseInt(a.getAttribute('data-tb-order'));
        const orderB = parseInt(b.getAttribute('data-tb-order'));
        return orderA - orderB;
      });

    if (sortedElements.length > 0) {
      sortedElements[0].classList.add('ms-auto');
    }
  }

  public removeChild(child: HTMLElement): void {
    child.remove();
  }

  public getElement(): HTMLElement {
    return this._toolbar;
  }

  public getToolbarContainer(): HTMLElement {
    return this._toolbarContainer;
  }
}

