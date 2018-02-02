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

namespace org.mycore.mets.model.pagination {

    export interface PaginationMethod {
        name: string;

        test(label: string): boolean;

        getArabicPageNumber(val: string): number;

        paginate(val: number): string;
    }

    export abstract class RegexpPagination implements PaginationMethod {
        public abstract name: string;

        public abstract getTestExpr(): RegExp;

        public test(label: string): boolean {
            return this.getTestExpr().test(label);
        }

        public abstract getArabicPageNumber(val: string): number;

        public abstract paginate(val: number): string;

    }

    export class RectoVersoLowerCasePaginationMethod extends RegexpPagination {
        public name: string = 'rectoVerso_lowercase';

        public getArabicPageNumber(label: string): number {
            const result = this.getTestExpr().exec(label);
            const parsed = {page : parseInt(result[ 1 ], 10), appendix : result[ 2 ]};
            return parsed.page * 2 - (parsed.appendix === 'r' ? 1 : 0);
        }

        public getTestExpr(): RegExp {
            return /([0-9]+)([rv])/;
        }

        public paginate(pageNumber: number): string {
            return Math.ceil(pageNumber / 2) + (pageNumber % 2 === 1 ? 'r' : 'v');
        }
    }

    export class ArabicNumberingPaginationMethod extends RegexpPagination {
        public name: string = 'arabicNumbering';

        public getArabicPageNumber(label: string): number {
            return parseInt(label, 10);
        }

        public getTestExpr(): RegExp {
            return /^([0-9]+)$/;
        }

        public paginate(val: number): string {
            return val.toString();
        }

    }

    export class ABLowerCasePaginationMethod extends RegexpPagination {
        public name: string = 'ab_lowercase';

        public getArabicPageNumber(label: string): number {
            const result = this.getTestExpr().exec(label);
            const parsed = {page : parseInt(result[ 1 ], 10), appendix : result[ 2 ]};
            return parsed.page * 2 - (parsed.appendix === 'a' ? 1 : 0);
        }

        public getTestExpr(): RegExp {
            return /^([0-9]+)([ab])$/;
        }

        public paginate(pageNumber: number): string {
            return Math.ceil(pageNumber / 2) + (pageNumber % 2 === 1 ? 'a' : 'b');
        }
    }

    export class LetterPaginationMethod extends RegexpPagination {
        public name: string = 'letter';

        public getArabicPageNumber(label: string): number {
            const result = this.getTestExpr().exec(label);
            const parsed = {page : parseInt(result[ 2 ], 10), appendix : result[ 1 ]};
            return parsed.page;
        }

        public getTestExpr(): RegExp {
            return /^(U)([0-9]+)$/;
        }

        public paginate(pageNumber: number): string {
            return 'U' + pageNumber;
        }
    }

    export class RomePaginationMethod extends RegexpPagination {
        public name: string = 'rome';

        public getArabicPageNumber(label: string): number {
            const token = this.getToken();
            const key = {
                m : 1000,
                cm : 900,
                d : 500,
                cd : 400,
                c : 100,
                xc : 90,
                l : 50,
                xl : 40,
                x : 10,
                ix : 9,
                v : 5,
                iv : 4,
                i : 1
            };
            let num = 0;
            let m;

            if (!this.getTestExpr().test(label)) {
                return null;

            }

            m = token.exec(label);
            while (m) {
                num += key[ m[ 0 ].toLowerCase() ];
                m = token.exec(label);
            }

            return num;
        }

        public getToken(): RegExp {
            return /[mdlv]|c[md]?|x[cl]?|i[xv]?/g;
        }

        public getTestExpr(): RegExp {
            return /^m*(?:d?c{0,3}|c[md])(?:l?x{0,3}|x[cl])(?:v?i{0,3}|i[xv])$/;
        }

        public test(label: string) {
            return label !== '' && super.test(label);
        }

        public paginate(pageNumber: number): string {
            const romanVars = {
                UNITS : [ '', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX' ],
                TENS : [ '', 'X', 'XX', 'XXX', 'XL', 'L', 'LX', 'LXX', 'LXXX', 'XC' ],
                HUNDREDS : [ '', 'C', 'CC', 'CCC', 'CD', 'D', 'DC', 'DCC', 'DCCC', 'CM' ],
                NUMERALS : {I : 1, V : 5, X : 10, L : 50, C : 100, D : 500, M : 1000}
            };

            // break down number into units, tens, hundreds, thousands
            const digits = String(+pageNumber).split('');
            let count = 3;
            let roman = '';

            // lookup numeral for units, tend and hundreds. pop off last 3 array elements in turn, lookup numeral and
            // prepend to previous.
            while (count--) {
                if (2 === count) {
                    roman = (romanVars.UNITS[ +digits.pop() ] || '') + roman;
                } else if (1 === count) {
                    roman = (romanVars.TENS[ +digits.pop() ] || '') + roman;
                } else {
                    roman = (romanVars.HUNDREDS[ +digits.pop() ] || '') + roman;
                }
            }

            // now deal with thousands, if required. If we have an array element left, this represents the thousands. We
            // need an M for each thousand
            const thousands = digits.pop();
            if (thousands) {
                // convert int to array and join. Add 1 to int to create array of correct length
                roman = new Array(+thousands + 1).join('M') + roman;
            }

            return roman.toLowerCase();
        }

    }

    export class RomeUppercasePaginationMethod extends RomePaginationMethod {
        public name: string = 'rome_uppercase';

        public getTestExpr() {
            return /^M*(?:D?C{0,3}|C[MD])(?:L?X{0,3}|X[CL])(?:V?I{0,3}|I[XV])$/;
        }

        public getToken() {
            return /[MDLV]|C[MD]?|X[CL]?|I[XV]?/g;
        }

        public paginate(pageNumber: number): string {
            return super.paginate(pageNumber).toUpperCase();
        }
    }

    export class ColsPaginationMethod extends RegexpPagination {
        public name: string = 'cols';

        public getArabicPageNumber(label: string): number {
            const result = this.getTestExpr().exec(label);

            const range = {
                fromRow : parseInt(result[ 1 ], 10),
                toRow : parseInt(result[ 2 ], 10)
            };
            const rowsPerPage = (range.toRow - range.fromRow) + 1;
            const actualPageNumber = range.toRow / rowsPerPage;

            if (rowsPerPage !== 2) {
                throw new Error(`rowsPerPage must be 2 (${rowsPerPage})`);
            }

            return actualPageNumber;
        }

        public paginate(pageNumber: number): string {
            return `Sp. ${(pageNumber * 2) - 1}-${pageNumber * 2}`;
        }

        public getTestExpr() {
            return /^Sp\. ([0-9]+)-([0-9]+)$/;
        }

    }

    export const paginationMethods: PaginationMethod[] = [
        new RectoVersoLowerCasePaginationMethod(),
        new ArabicNumberingPaginationMethod(),
        new ABLowerCasePaginationMethod(),
        new LetterPaginationMethod(),
        new RomePaginationMethod(),
        new RomeUppercasePaginationMethod(),
        new ColsPaginationMethod() ];

    export const getPaginationMethodByName = (name: string) => {
        const arrayOfMethodsWithName = paginationMethods.filter((pm: PaginationMethod) => pm.name === name);
        return arrayOfMethodsWithName[ 0 ];
    };

    export const detectPaginationMethodByPageLabel = (label: string) => {
        return paginationMethods.filter((method) => method.test(label))[ 0 ] || null;
    };

    export const getChanges = (from: number,
                               to: number,
                               changed: number,
                               changedTo: string,
                               method: PaginationMethod,
                               reverse: boolean = false) => {

        const changedVal = method.getArabicPageNumber(changedTo);
        const pagination = [];

        for (let i = from; i <= to; i++) {
            const relativeNumber = changedVal + (reverse ? -(i - changed) : (i - changed));
            pagination.push(method.paginate(relativeNumber));
        }

        return pagination;
    };

}
