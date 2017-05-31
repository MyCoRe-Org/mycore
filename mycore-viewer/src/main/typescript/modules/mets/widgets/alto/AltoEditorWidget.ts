namespace mycore.viewer.widgets.alto {

    export class AltoEditorWidget {
        private widgetElement: JQuery;

        private tableContainer: JQuery;

        private buttonContainer: JQuery;

        public changeWordButton: JQuery;

        private idChangeMap = new MyCoReMap<string, AltoChange>();
        private idViewMap = new MyCoReMap<string, JQuery>();
        private pageHeading: JQuery;

        private actionHeading: JQuery;

        private infoHeading: JQuery;

        private changeClickHandler: Array<(change: AltoChange) => void> = new Array<(change: AltoChange) => void>();

        constructor(container: JQuery, private i18n: model.LanguageModel) {
            this.widgetElement = jQuery(this.createHTML());
            this.widgetElement.appendTo(container);
            this.tableContainer = this.widgetElement.find("tbody.table-line-container");
            this.buttonContainer = this.widgetElement.find("div.button-group-container");
            this.changeWordButton = this.widgetElement.find("button.changeWord");

            this.pageHeading = this.widgetElement.find("[data-sort=pageHeading]");
            this.actionHeading = this.widgetElement.find("[data-sort=actionHeading]");
            this.infoHeading = this.widgetElement.find("[data-sort=infoHeading]");

            this.pageHeading.click(this.getSortClickEventHandler('pageHeading'));
            this.actionHeading.click(this.getSortClickEventHandler('actionHeading'));
            this.infoHeading.click(this.getSortClickEventHandler('infoHeading'));

        }

        public addChangeClickedEventHandler(handler: (change: AltoChange) => void) {
            this.changeClickHandler.push(handler);
        }

        private getSortClickEventHandler(byClicked: string) {
            return (ev) => {
                console.log(this.getCurrentSortMethod());
                let currentSort = this.getCurrentSortMethod();
                if (currentSort == null || currentSort.sortBy !== byClicked) {
                    this.sortBy(byClicked, true)
                } else {
                    this.sortBy(byClicked, !currentSort.down)
                }
            };
        }

        private getCurrentSortMethod() {
            let headerAttached, arrowAttached;
            if ((headerAttached = (arrowAttached = this.downArrow).parent()).length > 0 ||
                (headerAttached = (arrowAttached = this.upArrow).parent()).length > 0) {
                let sortBy = headerAttached.attr("data-sort");

                return {sortBy : sortBy, down : arrowAttached[ 0 ] === this.downArrow[ 0 ]}
            }

            return null;
        }

        private sortBy(by: string, down: boolean) {
            this.downArrow.detach();
            this.upArrow.detach();

            let elem = this.widgetElement.find(`[data-sort=${by}]`);
            if (down) {
                elem.append(this.downArrow);
            } else {
                elem.append(this.upArrow);
            }

            let sortedList = [];
            this.idViewMap.forEach((k, v) => {
                v.detach();
                sortedList.push(v);
            });

            sortedList.sort(this.getSortFn(by, down)).forEach((v) => {
                this.tableContainer.append(v);
            });

        }

        private getSortFn(by: string, down: boolean): (x, y) => number {
            let headerIndex = [ "pageHeading", "actionHeading", "infoHeading" ];
            switch (by) {
                case headerIndex[ 0 ]:
                    return (x: JQuery, y: JQuery) => {
                        let order1 = this.idChangeMap.get(x.attr("data-id")).getPageOrder();
                        let order2 = this.idChangeMap.get(y.attr("data-id")).getPageOrder();
                        return (down ? 1 : -1) * (order1 - order2);
                    };
                case headerIndex[ 1 ]:
                case headerIndex[ 2 ]:
                    return (x: JQuery, y: JQuery) => {
                        let text1 = jQuery(x.children("td").get(headerIndex.indexOf(by))).text();
                        let text2 = jQuery(y.children("td").get(headerIndex.indexOf(by))).text();

                        return (down ? 1 : -1) * text1.localeCompare(text2);
                    }
            }
            return (x, y) => -1;
        }

        private downArrow = jQuery(`
<span class='glyphicon glyphicon-arrow-down sortArrow'>
</span> 
`);

        private upArrow = jQuery(`    
<span class='glyphicon glyphicon-arrow-up sortArrow'>
</span> 
`);


        private createHTML() {
            return `
<div class="alto-editor-widget container-fluid">
    <h3 class="small-heading">${this.getLabel("altoWidget.heading")}</h3>     
    <div class="btn-toolbar">
        <div class="btn-group btn-group-xs button-group-container">
            <button type="button" class="btn btn-default changeWord">${this.getLabel("altoWidget.changeWord")}</button>
        </div>
    </div>   
    <h3 class="small-heading">${this.getLabel("altoWidget.changesHeading")}</h3>
    <div class="table-responsive">
        <table class="table table-condensed">
            <thead>
                <tr>
                    <th data-sort="pageHeading">${this.getLabel("altoWidget.table.page")}</th>
                    <th data-sort="actionHeading">${this.getLabel("altoWidget.table.action")}</th>
                    <th data-sort="infoHeading">${this.getLabel("altoWidget.table.info")}</th>
                </tr>
            </thead>
            <tbody class="table-line-container">
                
            </tbody>
        </table>
    </div>
</div>
`;
        }

        private getLabel(id: string) {
            return this.i18n.getTranslation(id);
        }

        public addChange(change: AltoChange) {
            if (this.idChangeMap.values.indexOf(change) != -1) {
                return;
            }

            let id = Math.random().toString(16);
            this.idChangeMap.set(id, change);

            this.addRow(id, change);
        }

        private addRow(id: string, change: AltoChange) {
            let view = jQuery(`
<tr data-id="${id}">
    ${this.getChangeHTMLContent(change)}
</tr>
`);
            let sortMethod = this.getCurrentSortMethod();
            if (sortMethod != null) {
                let sortFn = this.getSortFn(sortMethod.sortBy, sortMethod.down);
                let inserted = false;
                this.tableContainer.children("tr").each((i, elem) => {
                    let jqElem = jQuery(elem);
                    if (!inserted && sortFn(view, jqElem) == -1) {
                        view.insertBefore(jqElem);
                        inserted = true;
                    }
                });
                if (!inserted) {
                    this.tableContainer.append(view);
                }

            } else {
                this.tableContainer.append(view);
            }

            view.click(()=>{
                this.changeClickHandler.forEach((handler)=>{
                   handler(change);
                });
            });

            this.idViewMap.set(id, view);
        }

        private getChangeText(change: mycore.viewer.widgets.alto.AltoChange) {
            if (change.getType() == AltoWordChange.TYPE) {
                let wc = <AltoWordChange>change;
                return `${wc.from} => ${wc.to}`;
            }
        }

        updateChange(change: mycore.viewer.widgets.alto.AltoChange) {
            let changeID = this.getChangeID(change);

            this.idViewMap.get(changeID).html(this.getChangeHTMLContent(change));

        }

        getChangeID(change: mycore.viewer.widgets.alto.AltoChange) {
            let changeID = null;
            this.idChangeMap.forEach((k, v) => {
                if (v == change) {
                    changeID = k;
                }
            });
            return changeID;
        }

        private getChangeHTMLContent(change: mycore.viewer.widgets.alto.AltoChange) {
            return `
<td>${change.getPageOrder()}</td>
<td>${change.getType()}</td>
<td>${this.getChangeText(change)}</td>
`
        }

        removeChange(wordChange: mycore.viewer.widgets.alto.AltoChange) {
            let changeID = this.getChangeID(wordChange);
            this.idViewMap.get(changeID).remove();
            this.idViewMap.remove(changeID);
            this.idChangeMap.remove(changeID);
        }
    }


}
