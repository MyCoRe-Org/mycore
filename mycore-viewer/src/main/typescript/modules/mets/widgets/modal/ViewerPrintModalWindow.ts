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


import { ViewerModalWindow } from "../../../base/widgets/modal/ViewerModalWindow";

export class ViewerPrintModalWindow extends ViewerModalWindow {

  constructor(_mobile: boolean) {
    super(_mobile, 'CreatePDF');

    this._inputRow = document.createElement('div');
    this._inputRow.classList.add('row');
    this.modalBody.append(this._inputRow);

    this._previewBox = document.createElement('div');
    this._previewBox.classList.add('printPreview');
    this._previewBox.classList.add('col-sm-12');
    this._inputRow.append(this._previewBox);

    this._previewImage = document.createElement('img');
    this._previewImage.classList.add('img-thumbnail', 'mx-auto', 'd-block');
    this._previewBox.append(this._previewImage);

    this._pageSelectBox = document.createElement('form');
    this._pageSelectBox.classList.add('printForm');
    this._pageSelectBox.classList.add('col-sm-12');
    this._inputRow.append(this._pageSelectBox);

    this._selectGroup = document.createElement('div');
    this._selectGroup.classList.add('form-group');
    this._pageSelectBox.append(this._selectGroup);

    this._createRadioAllPages();
    this._createRadioCurrentPage();
    this._createRadioRangePages();
    this._createRadioChapters();

    this._validationRow = document.createElement('div');
    this._validationRow.classList.add('row');
    this.modalBody.append(this._validationRow);

    this._validationMessage = document.createElement('p');
    this._validationMessage.classList.add('col-sm-12');
    this._validationMessage.classList.add('float-end');
    this._validationRow.append(this._validationMessage);

    this._okayButton = document.createElement('a');
    this._okayButton.innerText = 'Ok';
    this._okayButton.setAttribute('type', 'button');
    this._okayButton.classList.add('btn', 'btn-secondary');
    this.modalFooter.append(this._okayButton)

    this._maximalPageMessage = document.createElement(`div`);
    this._maximalPageMessage.classList.add('row');
    this.modalBody.append(this._maximalPageMessage);

    const maxPageMessageSpan = document.createElement('span');
    maxPageMessageSpan.classList.add('message', 'col-sm-12');
    this._maximalPageMessage.append(maxPageMessageSpan);


    this._maximalPageNumber = document.createElement('span');
    this._maximalPageNumber.innerText = '';
    maxPageMessageSpan.append(this._maximalPageNumber);

    this._okayButton.addEventListener('click',() => {
      if (this.okayClickHandler != null) {
        this.okayClickHandler();
      }
    });

  }

  private static INPUT_IDENTIFIER = 'pages';
  private static INPUT_RANGE_IDENTIFIER = 'range';
  private static INPUT_RANGE_TEXT_IDENTIFIER = ViewerPrintModalWindow.INPUT_RANGE_IDENTIFIER + '-text';
  private static INPUT_CHAPTER_VALUE = 'chapter';

  public static INPUT_ALL_VALUE = 'all';
  public static INPUT_RANGE_VALUE = 'range';
  public static INPUT_CURRENT_VALUE = 'current';

  private _validationRow: HTMLElement;
  private _inputRow: HTMLElement;

  private _previewBox: HTMLElement;
  private _previewImage: HTMLElement;
  private _pageSelectBox: HTMLElement;
  private _okayButton: HTMLElement;
  private _selectGroup: HTMLElement;

  private _radioAllPages: HTMLElement;
  private _radioAllPagesLabelElement: HTMLElement;
  private _radioAllPagesInput: HTMLInputElement;

  private _radioCurrentPage: HTMLElement;
  private _radioCurrentPageLabelElement: HTMLElement;
  private _radioCurrentPageInput: HTMLInputElement;

  private _radioRangePages: HTMLElement;
  private _radioRangePagesLabelElement: HTMLElement;
  private _radioRangePagesInput: HTMLInputElement;
  private _radioRangePagesInputText: HTMLInputElement;

  private _radioChapter: HTMLElement;
  private _radioChapterInput: HTMLInputElement;
  private _chapterLabelElement: HTMLElement;
  private _chapterSelect: HTMLSelectElement;

  private _validationMessage: HTMLElement;
  private _maximalPageMessage: HTMLElement;
  private _maximalPageNumber: HTMLElement;

  public checkEventHandler: (id: string) => void = null;
  public rangeInputEventHandler: (text: string) => void = null;
  public chapterInputEventHandler: (id: string) => void = null;

  public okayClickHandler: () => void = null;

  public get currentPageLabel() {
    return this._radioCurrentPageLabelElement.innerText;
  }

  public set currentPageLabel(label: string) {
    this._radioCurrentPageLabelElement.innerText = label;
  }

  public get allPagesLabel() {
    return this._radioAllPagesLabelElement.innerText;
  }

  public set allPagesLabel(label: string) {
    this._radioAllPagesLabelElement.innerText = label;
  }

  public set rangeChecked(checked: boolean) {
    this._radioRangePagesInput.checked = checked;

  }

  public get rangeChecked() {
    return this._radioRangePagesInput.checked;
  }

  public set allChecked(checked: boolean) {
    this._radioAllPagesInput.checked = checked;
  }

  public get allChecked() {
    return this._radioAllPagesInput.checked;
  }

  public set currentChecked(checked: boolean) {
    this._radioCurrentPageInput.checked = checked;
  }

  public get currentChecked() {
    return this._radioCurrentPageInput.checked;
  }

  public set chapterChecked(checked: boolean) {
    this._radioChapterInput.checked = checked;
  }

  public get chapterChecked() {
    return this._radioChapterInput.checked;
  }

  public set validationResult(success: boolean) {
    if (success) {
      this._validationMessage.classList.remove('text-danger');
      this._validationMessage.classList.add('text-success');
      this._okayButton.classList.remove('disabled');
    } else {
      this._validationMessage.classList.remove('text-success');
      this._validationMessage.classList.add('text-danger');
      this._okayButton.classList.add('disabled');
    }
  }

  public set validationMessage(message: string) {
    this._validationMessage.innerText = message;
  }

  public get validationMessage() {
    return this._validationMessage.innerText;
  }

  public get rangeLabel() {
    return this._radioRangePagesLabelElement.innerText;
  }

  public set rangeLabel(label: string) {
    this._radioRangePagesLabelElement.innerText = label;
  }

  public get chapterLabel() {
    return this._chapterLabelElement.innerText;
  }

  public set chapterLabel(label: string) {
    this._chapterLabelElement.innerText = label;
  }

  private _createRadioRangePages() {
    const radioIdentifier = `${ViewerPrintModalWindow.INPUT_IDENTIFIER}_radio_range`;

    this._radioRangePages = document.createElement('div');
    this._radioRangePages.classList.add('form-check');

    this._radioRangePagesLabelElement = document.createElement('label');
    this._radioRangePagesLabelElement.classList.add('form-check-label');
    this._radioRangePagesLabelElement.setAttribute('for', radioIdentifier);

    this._radioRangePagesInput = document.createElement('input');
    this._radioRangePagesInput.classList.add('form-check-input');
    this._radioRangePagesInput.setAttribute('type', 'radio');
    this._radioRangePagesInput.setAttribute('name', ViewerPrintModalWindow.INPUT_IDENTIFIER);
    this._radioRangePagesInput.setAttribute('id', radioIdentifier);
    this._radioRangePagesInput.setAttribute('value', ViewerPrintModalWindow.INPUT_RANGE_VALUE);

    this._radioRangePagesInputText = document.createElement('input');
    this._radioRangePagesInputText.classList.add('form-control');
    this._radioRangePagesInputText.setAttribute('type', 'text');
    this._radioRangePagesInputText.setAttribute('name', ViewerPrintModalWindow.INPUT_RANGE_TEXT_IDENTIFIER);
    this._radioRangePagesInputText.setAttribute('id', ViewerPrintModalWindow.INPUT_RANGE_TEXT_IDENTIFIER);
    this._radioRangePagesInputText.setAttribute('placeholder', '1,3-5,8');

    const that = this;
    const onActivateHandler = () => {
      if (that.checkEventHandler != null) {
        that.checkEventHandler(ViewerPrintModalWindow.INPUT_RANGE_VALUE);
      }
      this._radioRangePagesInputText.focus();
    };
    this._radioRangePagesInput.addEventListener('change', onActivateHandler);
    this._radioRangePagesInput.addEventListener('click', onActivateHandler);

    this._radioRangePagesInputText.addEventListener('click', () => {
      this.allChecked = false;
      this.currentChecked = false;
      this.rangeChecked = true;
      onActivateHandler();
      this._radioRangePagesInputText.focus();
    });

    this._radioRangePagesInputText.addEventListener('keyup', () => {
      if (that.rangeInputEventHandler != null) {
        this.rangeInputEventHandler(this._radioRangePagesInputText.value + "");
      }
    });

    this._radioRangePages.append(this._radioRangePagesInput);
    this._radioRangePages.append(this._radioRangePagesLabelElement);
    this._radioRangePages.append(this._radioRangePagesInputText);
    this._selectGroup.append(this._radioRangePages);
  }

  private _createRadioChapters() {
    const radioIdentifier = `${ViewerPrintModalWindow.INPUT_IDENTIFIER}_radio_chapter`;

    this._radioChapter = document.createElement('div');
    this._radioChapter.classList.add('form-check');

    this._radioChapterInput = document.createElement('input');
    this._radioChapterInput.classList.add('form-check-input');
    this._radioChapterInput.setAttribute('type', 'radio');
    this._radioChapterInput.setAttribute('name', ViewerPrintModalWindow.INPUT_IDENTIFIER);
    this._radioChapterInput.setAttribute('id', radioIdentifier);
    this._radioChapterInput.setAttribute('value', ViewerPrintModalWindow.INPUT_CHAPTER_VALUE);

    this._chapterLabelElement = document.createElement('label');
    this._chapterLabelElement.classList.add('form-check-label');
    this._chapterLabelElement.setAttribute('for', radioIdentifier);

    this._chapterSelect = document.createElement('select');
    this._chapterSelect.classList.add('form-control');

    this._radioChapter.append(this._radioChapterInput);
    this._radioChapter.append(this._chapterLabelElement);
    this._radioChapter.append(this._chapterSelect);
    this._selectGroup.append(this._radioChapter)

    let onActivate = () => {
      if (this.checkEventHandler != null) {
        this.checkEventHandler(ViewerPrintModalWindow.INPUT_CHAPTER_VALUE);
      }
    };
    this._radioChapterInput.addEventListener('change', onActivate);

    this._chapterSelect.addEventListener('change', () => {
      onActivate();
      this.chapterChecked = true;
      if (this.chapterInputEventHandler != null) {
        this.chapterInputEventHandler(this._chapterSelect.value + "");
      }
    });
  }

  public set previewImageSrc(src: string) {
    this._previewImage.setAttribute('src', src);
  }

  public get rangeInputVal() {
    return this._radioRangePagesInputText.value;
  }

  public set rangeInputEnabled(enabled: boolean) {
    this._radioRangePagesInputText.value = '';
    this._radioRangePagesInputText.setAttribute('enabled', enabled + "");
  }

  public get rangeInputEnabled() {
    return this._radioRangePagesInputText.getAttribute('enabled') === 'true';
  }

  public get previewImageSrc() {
    return <string>this._previewImage.getAttribute('src');
  }

  private _createRadioAllPages() {
    const radioIdentifier = `${ViewerPrintModalWindow.INPUT_IDENTIFIER}_radio_all`;

    this._radioAllPages = document.createElement('div');
    this._radioAllPages.classList.add('form-check');

    this._radioAllPagesLabelElement = document.createElement(`label`);
    this._radioAllPagesLabelElement.classList.add('form-check-label');
    this._radioAllPagesLabelElement.setAttribute('for', radioIdentifier);

    this._radioAllPagesInput = document.createElement('input');
    this._radioAllPagesInput.setAttribute('type', 'radio');
    this._radioAllPagesInput.setAttribute('name', ViewerPrintModalWindow.INPUT_IDENTIFIER);
    this._radioAllPagesInput.setAttribute('id', radioIdentifier);
    this._radioAllPagesInput.setAttribute('value', ViewerPrintModalWindow.INPUT_ALL_VALUE);
    this._radioAllPagesInput.classList.add('form-check-input');

    this._radioAllPagesInput.addEventListener('change',() => {
      if (this.checkEventHandler != null) {
        this.checkEventHandler(ViewerPrintModalWindow.INPUT_ALL_VALUE);
      }
    });

    this._radioAllPages.append(this._radioAllPagesInput);
    this._radioAllPages.append(this._radioAllPagesLabelElement);

    this._selectGroup.append(this._radioAllPages);
  }

  private _createRadioCurrentPage() {
    const radioIdentifier = `${ViewerPrintModalWindow.INPUT_IDENTIFIER}_radio_current`;

    this._radioCurrentPage = document.createElement('div');
    this._radioCurrentPage.classList.add('form-check');

    this._radioCurrentPageLabelElement = document.createElement('label');
    this._radioCurrentPageLabelElement.classList.add('form-check-label');
    this._radioCurrentPageLabelElement.setAttribute('for', radioIdentifier);


    this._radioCurrentPageInput = document.createElement('input');
    this._radioCurrentPageInput.classList.add('form-check-input');
    this._radioCurrentPageInput.setAttribute('type', 'radio');
    this._radioCurrentPageInput.setAttribute('name', ViewerPrintModalWindow.INPUT_IDENTIFIER);
    this._radioCurrentPageInput.setAttribute('id', radioIdentifier);
    this._radioCurrentPageInput.setAttribute('value', ViewerPrintModalWindow.INPUT_CURRENT_VALUE);

    this._radioCurrentPageInput.addEventListener('change', () => {
      if (this.checkEventHandler != null) {
        this.checkEventHandler(ViewerPrintModalWindow.INPUT_CURRENT_VALUE);
      }
    });

    this._radioCurrentPage.append(this._radioCurrentPageInput);
    this._radioCurrentPage.append(this._radioCurrentPageLabelElement);

    this._selectGroup.append(this._radioCurrentPage);
  }

  public set maximalPages(number: string) {
    this._maximalPageNumber.innerText = number;
  }

  public set maximalPageMessage(message: string) {
    this._maximalPageNumber.remove();
    const messageDiv = this._maximalPageMessage.querySelector('.message') as HTMLElement;
    messageDiv.innerText = message + ' ';
    messageDiv.append(this._maximalPageNumber);
  }

  public setChapterTree(chapters: Array<{ id: string, label: string }>) {
    this._chapterSelect.innerHTML = (chapters.map((entry) => {
      return `<option value='${entry.id}'>${entry.label}</option>`;
    }).join(''));
  }

  public get chapterInputVal() {
    return this._chapterSelect.value;
  }

}

