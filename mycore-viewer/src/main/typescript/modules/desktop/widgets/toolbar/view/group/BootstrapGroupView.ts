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

export class BootstrapGroupView {

  private _element: HTMLElement;

  constructor(id: string, order: number, align: string) {
    this._element = document.createElement('div');
    this._element.setAttribute('data-id', id);
    this._element.classList.add('btn-group');

    if (align !== null && align === 'right') {
      this._element.classList.add('right');
    }

    this._element.setAttribute('data-tb-order', order+"");

  }

  public addChild(child: HTMLElement): void {
    this._element.append(child);
  }

  public removeChild(child: HTMLElement): void {
    child.remove();
  }

  public getElement(): HTMLElement {
    return this._element;
  }
}

