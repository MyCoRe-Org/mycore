/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

///<reference path="ModelChange.ts"/>
///<reference path="../model/simple/MCRMetsPage.ts"/>
namespace org.mycore.mets.model.state {
    export class PagesMoveChange extends ModelChange {

        private rangeFromLabel: string;
        private rangeToLabel: string;
        private moveToLabel: string;
        private moveFrom: PagesMoveChangeDestination;

        constructor(private pageList: model.simple.MCRMetsPage[],
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

        private static copy(pl: model.simple.MCRMetsPage[], range: PagesMoveChangeRange, to: PagesMoveChangeDestination) {
            const startIndex = pl.indexOf(range.from);
            const count = this.getRangeCount(pl, range, startIndex);
            const pages = pl.splice(startIndex, count);
            const destinationIndex = pl.indexOf(to.element) + (to.before ? 0 : 1);
            const args = (<any> [ destinationIndex, 0 ]).concat(pages);
            pl.splice.apply(pl, args);
        }

        private static getRangeCount(pl: model.simple.MCRMetsPage[], range: PagesMoveChangeRange,
                                     startIndex: number = pl.indexOf(range.from)) {
            return pl.indexOf(range.to) - startIndex + 1;
        }

        public doChange() {
            PagesMoveChange.copy(this.pageList, this.range, this.moveTo);
        }

        public unDoChange() {
            PagesMoveChange.copy(this.pageList, this.range, this.moveFrom);
        }

        public getDescription(messages: any) {
            return (messages.PagesMoveChangeDescription
                .replace('{range.from}', this.rangeFromLabel) || '???PagesMoveChangeDescription???')
                .replace('{range.to}', this.rangeToLabel)
                .replace('{move}', this.moveToLabel)
                .replace('{position}', this.moveTo.before ? messages.before : messages.after);
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
