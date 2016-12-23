///<reference path="MCRMetsFile.ts"/>

namespace org.mycore.mets.model.simple {
    export class MCRMetsPage {

        constructor(public id: string,
                    public orderLabel: string,
                    public contentIds: string,
                    public hidden: boolean,
                    public fileList: Array<MCRMetsFile> = new Array<MCRMetsFile>()) {
        }

        public static copy(page: MCRMetsPage) {
            return new MCRMetsPage(page.id, page.orderLabel, page.contentIds, page.hidden, page.fileList.slice()
                .map((file: MCRMetsFile) => {
                    return MCRMetsFile.copy(file);
                }));
        }
    }
}
