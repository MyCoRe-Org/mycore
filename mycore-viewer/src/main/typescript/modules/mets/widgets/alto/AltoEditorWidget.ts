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


import { MyCoReMap } from "../../../base/Utils";
import { AltoChange, AltoWordChange } from "./AltoChange";
import { LanguageModel } from "../../../base/components/model/LanguageModel";

export class AltoEditorWidget {
  private widgetElement: HTMLElement;

  private tableContainer: HTMLElement;

  private buttonContainer: HTMLElement;

  public changeWordButton: HTMLElement;

  private idChangeMap = new MyCoReMap<string, AltoChange>();
  private fileChangeMap = new MyCoReMap<string, Array<AltoChange>>();

  private idViewMap = new MyCoReMap<string, HTMLElement>();
  private pageHeading: HTMLElement;

  private actionHeading: HTMLElement;

  private infoHeading: HTMLElement;

  private changeClickHandler: Array<(change: AltoChange) => void> = new Array<(change: AltoChange) => void>();
  private changeRemoveClickHandler: Array<(change: AltoChange) => void> = new Array<(change: AltoChange) => void>();
  private submitClickHandler: Array<() => void> = new Array<() => void>();
  private applyClickHandler: Array<() => void> = new Array<() => void>();
  private deleteClickHandler: Array<() => void> = new Array<() => void>();

  private submitButton: HTMLElement;
  private applyButton: HTMLElement;
  private deleteButton: HTMLElement;

  constructor(container: HTMLElement, private i18n: LanguageModel) {
    this.widgetElement = this.createHTML();
    container.append(this.widgetElement);

    this.tableContainer = this.widgetElement.querySelector("tbody.table-line-container");
    this.buttonContainer = this.widgetElement.querySelector("div.button-group-container");
    this.changeWordButton = this.widgetElement.querySelector("button.changeWord");
    this.submitButton = this.widgetElement.querySelector(".submit-button");
    this.applyButton = this.widgetElement.querySelector(".apply-button");
    this.deleteButton = this.widgetElement.querySelector(".delete-button");

    this.pageHeading = this.widgetElement.querySelector("[data-sort=pageHeading]");
    this.actionHeading = this.widgetElement.querySelector("[data-sort=actionHeading]");
    this.infoHeading = this.widgetElement.querySelector("[data-sort=infoHeading]");

    this.pageHeading.addEventListener('click', this.getSortClickEventHandler('pageHeading'));
    this.actionHeading.addEventListener('click', this.getSortClickEventHandler('actionHeading'));
    this.infoHeading.addEventListener('click', this.getSortClickEventHandler('infoHeading'));

    this.submitButton.addEventListener('click', () => {
      this.submitClickHandler.forEach((e) => {
        e();
      });
    });

    this.applyButton.addEventListener('click', () => {
      this.applyClickHandler.forEach((e) => {
        e();
      });
    });

    this.deleteButton.addEventListener('click', () => {
      this.deleteClickHandler.forEach((e) => {
        e();
      });
    });
  }

  public enableApplyButton(enabled: boolean = true) {
    if (enabled) {
      this.applyButton.style.display = "block";
    } else {
      this.applyButton.style.display = "none";
    }
  }

  public addChangeClickedEventHandler(handler: (change: AltoChange) => void) {
    this.changeClickHandler.push(handler);
  }

  public addSubmitClickHandler(handler: () => void) {
    this.submitClickHandler.push(handler);
  }

  public addApplyClickHandler(handler: () => void) {
    this.applyClickHandler.push(handler);
  }

  public addDeleteClickHandler(handler: () => void) {
    this.deleteClickHandler.push(handler);
  }

  public addChangeRemoveClickHandler(handler: (change: AltoChange) => void) {
    this.changeRemoveClickHandler.push(handler);
  }

  private getSortClickEventHandler(byClicked: string) {
    return (ev) => {
      let currentSort = this.getCurrentSortMethod();
      if (currentSort == null || currentSort.sortBy !== byClicked) {
        this.sortBy(byClicked, true)
      } else {
        this.sortBy(byClicked, !currentSort.down)
      }
    };
  }

  private getCurrentSortMethod() {
    let headerAttached;
    let arrowAttached
    if (this.downArrow.parentElement != null) {
      headerAttached = this.downArrow.parentElement.parentElement;
      arrowAttached = this.downArrow;
    } else if (this.upArrow.parentElement != null) {
      headerAttached = this.upArrow.parentElement.parentElement;
      arrowAttached = this.upArrow;
    } else {
      return null;
    }

    let sortBy = headerAttached.attr("data-sort");
    return {sortBy: sortBy, down: arrowAttached[0] === this.downArrow[0]}
  }

  private sortBy(by: string, down: boolean) {
    this.downArrow.remove();
    this.upArrow.remove();

    let elem = this.widgetElement.querySelector(`[data-sort=${by}]`);
    if (down) {
      elem.append(this.downArrow);
    } else {
      elem.append(this.upArrow);
    }

    let sortedList = [];
    this.idViewMap.forEach((k, v) => {
      v.remove();
      sortedList.push(v);
    });

    sortedList.sort(this.getSortFn(by, down)).forEach((v) => {
      this.tableContainer.append(v);
    });

  }

  private getSortFn(by: string, down: boolean): (x: HTMLElement, y:HTMLElement) => number {
    let headerIndex = ["action", "pageHeading", "actionHeading", "infoHeading"];
    switch (by) {
      case headerIndex[1]:
        return (x: HTMLElement, y: HTMLElement) => {
          let order1 = this.idChangeMap.get(x.getAttribute("data-id")).pageOrder;
          let order2 = this.idChangeMap.get(y.getAttribute("data-id")).pageOrder;
          return (down ? 1 : -1) * (order1 - order2);
        };
      case headerIndex[2]:
      case headerIndex[3]:
        return (x: HTMLElement, y: HTMLElement) => {
          let text1 = Array.from(x.children)
              .filter(el => el.tagName == 'TD')
              .map(el => el as HTMLTableDataCellElement)[headerIndex.indexOf(by)].innerText;
          let text2 = Array.from(y.children)
              .filter(el => el.tagName == 'TD')
              .map(el => el as HTMLTableDataCellElement)[headerIndex.indexOf(by)].innerText;

          return (down ? 1 : -1) * text1.localeCompare(text2);
        }
    }
    return (x, y) => -1;
  }

  private createDownArrow() {
    const downArrow = document.createElement("span");
    downArrow.classList.add("fas", "fa-caret-down", "sortArrow");
    return downArrow;
  }

  private downArrow = this.createDownArrow();

    private createUpArrow() {
        const upArrow = document.createElement("span");
        upArrow.classList.add("fas", "fa-caret-up", "sortArrow");
        return upArrow;
    }

  private upArrow = this.createUpArrow();


  private createHTML() {
    const html = `
    <h3 class="small-heading">${this.getLabel("altoWidget.heading")}</h3>     
    <div class="btn-toolbar edit">
        <div class="btn-group btn-group-xs button-group-container">
            <button type="button" class="btn btn-secondary changeWord">${this.getLabel("altoWidget.changeWord")}</button>
        </div>
    </div>
    <h3 class="small-heading">${this.getLabel("altoWidget.changesHeading")}</h3>
    <div class="table-responsive">
        <table class="table table-condensed">
            <thead>
                <tr>
                    <th></th>
                    <th data-sort="pageHeading">${this.getLabel("altoWidget.table.page")}</th>
                    <th data-sort="actionHeading">${this.getLabel("altoWidget.table.action")}</th>
                    <th data-sort="infoHeading">${this.getLabel("altoWidget.table.info")}</th>
                </tr>
            </thead>
            <tbody class="table-line-container">
                
            </tbody>
        </table>
    </div>
    <div class="btn-toolbar action">
        <div class="btn-group btn-group-xs button-group-container">
            <button type="button" class="btn btn-primary apply-button">${this.getLabel("altoWidget.apply")}</button>
            <button type="button" class="btn btn-success submit-button">${this.getLabel("altoWidget.submit")}</button>
            <button type="button" class="btn btn-danger delete-button">${this.getLabel("altoWidget.delete")}</button>
        </div>
    </div>
`;
    const element = document.createElement("div");
    element.innerHTML = html;
    return element;
  }

  private getLabel(id: string) {
    return this.i18n.getTranslation(id);
  }

  public hasChange(key: string) {
    return this.idChangeMap.has(key);
  }

  public getChange(key: string) {
    return this.idChangeMap.get(key);
  }

  public getChanges() {
    return this.idChangeMap;
  }

  public getChangesInFile(file: string) {
    return this.fileChangeMap.get(file) || [];
  }

  public addChange(key: string, change: AltoChange) {
    if (this.idChangeMap.values.indexOf(change) != -1) {
      return;
    }

    this.idChangeMap.set(key, change);
    let changes = this.fileChangeMap.get(change.file);
    if (!this.fileChangeMap.has(change.file)) {
      changes = [];
      this.fileChangeMap.set(change.file, changes);
    }
    changes.push(change);

    this.addRow(key, change);
  }

  private addRow(id: string, change: AltoChange) {
    let view = document.createElement("tr");
    view.setAttribute("data-id", id);
    view.innerHTML = this.getChangeHTMLContent(change);

    let sortMethod = this.getCurrentSortMethod();
    if (sortMethod != null) {
      let sortFn = this.getSortFn(sortMethod.sortBy, sortMethod.down);
      let inserted = false;
      Array.from(this.tableContainer.children)
          .filter((e) => e.tagName == "TR")
          .forEach((elem) => {
            if (!inserted && sortFn(view, elem as HTMLElement) == -1) {
              elem.parentElement.insertBefore(view, elem);
              inserted = true;
            }
          });
      if (!inserted) {
        this.tableContainer.append(view);
      }

    } else {
      this.tableContainer.append(view);
    }

    view.addEventListener('click', (e) => {
      if("classList" in e.target && (e.target as HTMLElement).classList.contains("remove")) {
        this.changeRemoveClickHandler.forEach((handler) => {
          handler(change);
        });
      } else {
        this.changeClickHandler.forEach((handler) => {
          handler(change);
        });
      }

    });

    this.idViewMap.set(id, view);
  }

  private getChangeText(change: AltoChange) {
    if (change.type == AltoWordChange.TYPE) {
      let wc = change as AltoWordChange;
      return `${wc.from} => ${wc.to}`;
    }
  }

  public updateChange(change: AltoChange) {
    let changeID = this.getChangeID(change);

    this.idViewMap.get(changeID).innerHTML = this.getChangeHTMLContent(change);
  }

  public getChangeID(change: AltoChange) {
    let changeID = null;
    this.idChangeMap.forEach((k, v) => {
      if (v == change) {
        changeID = k;
      }
    });
    return changeID;
  }

  private getChangeHTMLContent(change: AltoChange) {
    return `
<td><span class="fas fa-ban remove"></span></td>
<td>${change.pageOrder}</td>
<td>${this.i18n.getTranslation("altoWidget.change." + change.type)}</td>
<td>${this.getChangeText(change)}</td>
`
  }

  removeChange(wordChange: AltoChange) {
    let changeID = this.getChangeID(wordChange);
    this.idViewMap.get(changeID).remove();
    this.idViewMap.remove(changeID);
    this.idChangeMap.remove(changeID);

    if (this.fileChangeMap.has(wordChange.file)) {
      let changes = this.fileChangeMap.get(wordChange.file);
      let index = 0;
      while ((index = changes.indexOf(wordChange, index)) != -1) {
        changes.splice(index, 1);
      }
    }
  }
}



