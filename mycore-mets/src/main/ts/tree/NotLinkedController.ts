///<reference path="../model/simple/MCRMetsSection.ts"/>
///<reference path="../model/simple/MCRMetsSimpleModel.ts"/>
///<reference path="../model/MetsEditorModel.ts"/>
///<reference path="../model/simple/MCRMetsPage.ts"/>


namespace org.mycore.mets.controller {

    import MCRMetsSection = org.mycore.mets.model.simple.MCRMetsSection;
    import MCRMetsSimpleModel = org.mycore.mets.model.simple.MCRMetsSimpleModel;
    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;
    import MCRMetsPage = org.mycore.mets.model.simple.MCRMetsPage;

    export class NotLinkedController {
        constructor() {
            /* */
        }

        private model: MetsEditorModel;

        public init(model: MetsEditorModel) {
            this.model = model;
        }

        public getNotLinkedPages() {
            let linkedPages = [];

            const addLinkedPages = (section: MCRMetsSection) => {
                section.linkedPages.forEach((p: MCRMetsPage) => {
                    linkedPages[ p.id ] = true;
                });

                section.metsSectionList.forEach(addLinkedPages);
            };
            addLinkedPages(this.model.metsModel.rootSection);

            return this.model.metsModel.metsPageList.filter((p: MCRMetsPage) => {
                return linkedPages[ p.id ] !== true;
            });
        }

        public getPageLabel(p: MCRMetsPage) {
            return p.orderLabel || "[" + (this.model.metsModel.metsPageList.indexOf(p) + 1) + "]";
        }
    }
}


