namespace mycore.viewer.widgets.alto {

    export class AltoEditorWidget {
        private widgetElement: JQuery;

        private tableContainer: JQuery;

        private buttonContainer: JQuery;

        private changeWord: JQuery;

        constructor(container: JQuery, private i18n: model.LanguageModel) {
            this.widgetElement = jQuery(this.createHTML());
            this.widgetElement.appendTo(container);
            this.tableContainer = this.widgetElement.find("tbody.table-line-container");
            this.buttonContainer = this.widgetElement.find("div.button-group-container");
            this.changeWord = this.widgetElement.find("button.changeWord");
        }


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
                    <th>${this.getLabel("altoWidget.table.page")}</th>
                    <th>${this.getLabel("altoWidget.table.action")}</th>
                    <th>${this.getLabel("altoWidget.table.info")}</th>
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

        private initEventHandler() {
            this.changeWord.click((e) => {

            });
        }

    }

}
