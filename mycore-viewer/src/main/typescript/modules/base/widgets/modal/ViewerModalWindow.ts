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


import { LanguageModel } from "../../components/model/LanguageModel";
import type { Modal } from "bootstrap";

export class ViewerModalWindow {
  constructor(private _mobile: boolean, _title: string, parent: HTMLElement = document.body) {

    this._wrapper = document.createElement("div");
    this._wrapper.classList.add("modal", "fade", "bs-modal-sm");
    this._wrapper.tabIndex= -1;
    this._wrapper.setAttribute("role", "dialog");
    this._wrapper.setAttribute("aria-labeleby", "permalinkLabel");
    this._wrapper.setAttribute("aria-hidden", "true");
    this._wrapper.addEventListener("click", (e) => {
      if (e.target == this._wrapper[0]) {
        this.hide();
      }
    });

    this._box = document.createElement("div");
    this._box.classList.add("modal-dialog", "modal-sm");
    this.wrapper.append(this._box);

    this._content = document.createElement("div");
    this._content.classList.add("modal-content");
    this._box.append(this._content);

    this._header = document.createElement("div");
    this._header.classList.add("modal-header");
    this._content.append(this._header);


    this._headerTitle = document.createElement("h4");
    this._headerTitle.classList.add("modal-title");
    this._headerTitle.setAttribute("data-i18n", _title);
    this._headerTitle.textContent = _title;
    this._header.append(this._headerTitle);

    this._body = document.createElement("div");
    this._body.classList.add("modal-body");
    this._content.append(this._body);

    this._footer = document.createElement("div");
    this._footer.classList.add("modal-footer");
    this._content.append(this._footer);

    this._close = document.createElement("button");
    this._close.setAttribute("type", "button");
    this._close.classList.add("btn", "btn-secondary");
    this._close.setAttribute("data-i18n", "modal.close");
    this._close.textContent = "Close";
    this._footer.append(this._close);

    this._close.addEventListener("click", () => {
      this.hide();
    });

    parent.prepend(this._wrapper);

    this._modal = new (window["bootstrap"]).Modal(this._wrapper) as Modal;
  }

  private _modal: Modal;
  private _wrapper: HTMLElement;
  private _box: HTMLElement;
  private _content: HTMLElement;
  private _header: HTMLElement;
  private _headerTitle: HTMLElement;
  private _body: HTMLElement;
  private _footer: HTMLElement;
  private _close: HTMLElement;

  public get box() {
    return this._box;
  }

  public get wrapper() {
    return this._wrapper;
  }

  public get modalContent() {
    return this._content;
  }

  public get modalHeader() {
    return this._header;
  }

  public get modalHeaderTitle() {
    return this._headerTitle;
  }

  public get modalBody() {
    return this._body;
  }

  public get modalFooter() {
    return this._footer;
  }

  public show(): void {
    this._modal.show();
  }

  public hide(): void {
   this._modal.hide();
  }

  public get closeButton() {
    return this._close;
  }

  public set closeLabel(label: string) {
    this._close.innerText = label;
  }

  public get title() {
    return this._headerTitle.innerText;
  }

  public set title(title: string) {
    this._headerTitle.innerText = title;
  }

  public updateI18n(languageModel: LanguageModel): ViewerModalWindow {
    languageModel.translate(this._wrapper);
    return this;
  }

}

