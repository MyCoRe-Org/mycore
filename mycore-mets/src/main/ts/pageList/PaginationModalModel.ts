namespace org.mycore.mets.model {
    export class PaginationModalModel {
        constructor(public messages: {[key: string]: string},
                    public selectedPages: Array<simple.MCRMetsPage>,
                    public selectedPagesIndex: number,
                    public begin: number = 0,
                    public method: PaginationMethod = null,
                    public value: string = "1") {
        }

        public methods = org.mycore.mets.model.Pagination.paginationMethods;
        public reverse: boolean = false;
    }
}
