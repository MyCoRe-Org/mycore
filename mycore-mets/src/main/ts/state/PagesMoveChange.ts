///<reference path="ModelChange.ts"/>
///<reference path="../model/simple/MCRMetsPage.ts"/>
namespace org.mycore.mets.model.state {
    export class PagesMoveChange extends ModelChange {

        constructor(private pageList: Array<model.simple.MCRMetsPage>,
                    private range: PagesMoveChangeRange,
                    private moveTo: PagesMoveChangeDestination) {
            super();

            const moveFromIndex = this.pageList.indexOf(range.from);
            if (moveFromIndex > 0) {
                this.moveFrom = {
                    before : false,
                    element : this.pageList[ moveFromIndex - 1 ]
                };
            } else {
                this.moveFrom = {
                    before : true,
                    element : this.pageList[ pageList.indexOf(range.to) + 1 ]
                };
            }

            let alternativeLabel = (range.from.fileList.length > 0) ?
                range.from.fileList[ 0 ].href : (pageList.indexOf(range.from) + 1).toString(10);
            this.rangeFromLabel = range.from.orderLabel || alternativeLabel;

            alternativeLabel = (range.to.fileList.length > 0) ?
                range.to.fileList[ 0 ].href : (pageList.indexOf(range.to) + 1).toString(10);
            this.rangeToLabel = range.to.orderLabel || alternativeLabel;

            alternativeLabel = (moveTo.element.fileList.length > 0) ?
                moveTo.element.fileList[ 0 ].href : (pageList.indexOf(moveTo.element) + 1).toString(10);
            this.moveToLabel = moveTo.element.orderLabel || alternativeLabel;
        }

        private rangeFromLabel: string;
        private rangeToLabel: string;
        private moveToLabel: string;

        private moveFrom: PagesMoveChangeDestination;

        public doChange() {
            PagesMoveChange.copy(this.pageList, this.range, this.moveTo);
        }

        public unDoChange() {
            PagesMoveChange.copy(this.pageList, this.range, this.moveFrom);
        }

        public getDescription(messages: any) {
            return (messages[ "PagesMoveChangeDescription" ]
                .replace("{range.from}", this.rangeFromLabel) || "???PagesMoveChangeDescription???")
                .replace("{range.to}", this.rangeToLabel)
                .replace("{move}", this.moveToLabel)
                .replace("{position}", this.moveTo.before ? messages[ "before" ] : messages[ "after" ]);
        }

        private static copy(pl: Array<model.simple.MCRMetsPage>, range: PagesMoveChangeRange, to: PagesMoveChangeDestination) {
            const startIndex = pl.indexOf(range.from);
            const count = this.getRangeCount(pl, range, startIndex);
            const pages = pl.splice(startIndex, count);
            const destinationIndex = pl.indexOf(to.element) + (to.before ? 0 : 1 );
            const args = (<any> [ destinationIndex, 0 ]).concat(pages);
            pl.splice.apply(pl, args);
        }

        private static getRangeCount(pl, range, startIndex = pl.indexOf(range.from)) {
            return pl.indexOf(range.to) - startIndex + 1;
        }


    }
    export interface PagesMoveChangeDestination {
        before: boolean;
        element: model.simple.MCRMetsPage;
    }

    export interface PagesMoveChangeRange {
        from: model.simple.MCRMetsPage;
        to: model.simple.MCRMetsPage;
    }
}
