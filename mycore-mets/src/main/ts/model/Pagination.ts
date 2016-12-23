namespace org.mycore.mets.model {
    export class Pagination {

        private static regExpTestMethod = function (label) {
            return this.testExpr.test(label);
        };

        private static suffixRegExprParsing = function rvBegin(label) {
            const result = this.testExpr.exec(label);
            return {page : parseInt(result[ 1 ], 10), appendix : result[ 2 ]};
        };

        private static fromToRegExprParsing = function fromTo(label) {
            const result = this.testExpr.exec(label);
            return {
                fromRow : parseInt(result[ 1 ], 10),
                toRow : parseInt(result[ 2 ], 10)
            };
        };

        private static prefixRegExprParsing = function rvBegin(label) {
            const result = this.testExpr.exec(label);
            return {page : parseInt(result[ 2 ], 10), appendix : result[ 1 ]};
        };

        private static romanVars = {
            UNITS : [ "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX" ],
            TENS : [ "", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC" ],
            HUNDREDS : [ "", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM" ],
            NUMERALS : {I : 1, V : 5, X : 10, L : 50, C : 100, D : 500, M : 1000}
        };

        public static getPaginationMethodByName(name: string) {
            const arrayOfMethodsWithName = Pagination.paginationMethods.filter((pm: PaginationMethod) => pm.name === name);
            return arrayOfMethodsWithName[ 0 ];
        }

        public static detectPaginationMethodByPageLabel(label: string) {
            return this.paginationMethods.filter((method) => method.test(label))[ 0 ] || null;
        }

        public static getChanges(from: number,
                                 to: number,
                                 changed: number,
                                 changedTo: string,
                                 method: PaginationMethod,
                                 reverse: boolean = false) {
            const changedVal = method.getArabicPageNumber(changedTo);
            const pagination = new Array<string>();

            for (let i = from; i <= to; i++) {
                const relativeNumber = changedVal + (reverse ? -(i - changed) : (i - changed));
                pagination.push(method.paginate(relativeNumber));
            }

            return pagination;
        }

        public static paginationMethods: Array<PaginationMethod> = <any> [
            {
                name : "rectoVerso_lowercase",
                testExpr : /([0-9]+)([rv])/,
                test : Pagination.regExpTestMethod,
                parseExpr : Pagination.suffixRegExprParsing,
                paginate : function (nextPageNumber) {
                    return Math.ceil(nextPageNumber / 2) + (nextPageNumber % 2 === 1 ? "r" : "v" );
                },
                getArabicPageNumber : function (val) {
                    const parsed = this.parseExpr(val);
                    return parsed.page * 2 - (parsed.appendix === "r" ? 1 : 0);
                }
            },
            {
                name : "arabicNumbering",
                testExpr : /^([0-9]+)$/,
                test : Pagination.regExpTestMethod,
                parseExpr : function (label) {
                    return parseInt(label, 10);
                },
                paginate : function (nextPageNumber) {
                    return nextPageNumber.toString();
                },
                getArabicPageNumber : function (val) {
                    return parseInt(val, 10);
                }
            },
            {
                name : "ab_lowercase",
                testExpr : /^([0-9]+)([ab])$/,
                test : Pagination.regExpTestMethod,
                parseExpr : Pagination.suffixRegExprParsing,
                paginate : function (nextPageNumber) {
                    return Math.ceil(nextPageNumber / 2) + (nextPageNumber % 2 === 1 ? "a" : "b" );
                },
                getArabicPageNumber : function (val) {
                    const parsed = this.parseExpr(val);
                    return parsed.page * 2 - (parsed.appendix === "a" ? 1 : 0);
                }
            },
            {
                name : "letter",
                testExpr : /^(U)([0-9]+)$/,
                test : Pagination.regExpTestMethod,
                parseExpr : Pagination.prefixRegExprParsing,
                paginate : function (nextPageNumber) {
                    return "U" + nextPageNumber;
                },
                getArabicPageNumber : function (val) {
                    return this.parseExpr(val).page;
                }
            },
            {
                name : "rome",
                vars : Pagination.romanVars,
                testExpr : /^m*(?:d?c{0,3}|c[md])(?:l?x{0,3}|x[cl])(?:v?i{0,3}|i[xv])$/,
                test : function (rome: string) {
                    return rome !== "" && Pagination.regExpTestMethod.apply(this, [ rome ]);
                },
                paginate : function (arabic: number) {
                    // break down number into units, tens, hundreds, thousands
                    let digits = String(+arabic).split(""),
                        count = 3,
                        roman = "";

                    // lookup numeral for units, tend and hundreds. pop off last 3 array elements in turn, lookup numeral and
                    // prepend to previous.
                    while (count--) {
                        if (2 === count) {
                            roman = (this.vars.UNITS[ +digits.pop() ] || "") + roman;
                        } else if (1 === count) {
                            roman = (this.vars.TENS[ +digits.pop() ] || "") + roman;
                        } else {
                            roman = (this.vars.HUNDREDS[ +digits.pop() ] || "") + roman;
                        }
                    }

                    // now deal with thousands, if required. If we have an array element left, this represents the thousands. We
                    // need an M for each thousand
                    let thousands = digits.pop();
                    if (thousands) {
                        // convert int to array and join. Add 1 to int to create array of correct length
                        roman = new Array(+thousands + 1).join("M") + roman;
                    }

                    return roman.toLowerCase();

                },
                getArabicPageNumber : function (rome: string) {
                    const token = /[mdlv]|c[md]?|x[cl]?|i[xv]?/g;
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

                    if (!this.testExpr.test(rome)) {
                        return null;

                    }

                    while (m = token.exec(rome)) {
                        num += key[ m[ 0 ] ];
                    }

                    return num;
                }
            }, {
                name : "rome_uppercase",
                vars : Pagination.romanVars,
                testExpr : /^M*(?:D?C{0,3}|C[MD])(?:L?X{0,3}|X[CL])(?:V?I{0,3}|I[XV])$/,
                test : function (rome: string) {
                    return rome !== "" && Pagination.regExpTestMethod.apply(this, [ rome ]);
                },
                paginate : function (arabic: number) {
                    // break down number into units, tens, hundreds, thousands
                    let digits = String(+arabic).split(""),
                        count = 3,
                        roman = "";

                    // lookup numeral for units, tend and hundreds. pop off last 3 array elements in turn, lookup numeral and
                    // prepend to previous.
                    while (count--) {
                        if (2 === count) {
                            roman = (this.vars.UNITS[ +digits.pop() ] || "") + roman;
                        } else if (1 === count) {
                            roman = (this.vars.TENS[ +digits.pop() ] || "") + roman;
                        } else {
                            roman = (this.vars.HUNDREDS[ +digits.pop() ] || "") + roman;
                        }
                    }

                    // now deal with thousands, if required. If we have an array element left, this represents the thousands. We
                    // need an M for each thousand
                    let thousands = digits.pop();
                    if (thousands) {
                        // convert int to array and join. Add 1 to int to create array of correct length
                        roman = new Array(+thousands + 1).join("M") + roman;
                    }

                    return roman.toUpperCase();

                },
                getArabicPageNumber : function (rome: string) {
                    const token = /[MDLV]|C[MD]?|X[CL]?|I[XV]?/g;
                    const key = {
                        M : 1000,
                        CM : 900,
                        D : 500,
                        CD : 400,
                        C : 100,
                        XC : 90,
                        L : 50,
                        XL : 40,
                        X : 10,
                        IX : 9,
                        V : 5,
                        IV : 4,
                        I : 1
                    };
                    let num = 0;
                    let m;

                    if (!this.testExpr.test(rome)) {
                        return null;
                    }

                    while (m = token.exec(rome)) {
                        num += key[ m[ 0 ] ];
                    }

                    return num;
                }
            }, {
                name : "cols",
                testExpr : /^Sp\. ([0-9]+)-([0-9]+)$/,
                test : Pagination.regExpTestMethod,
                getArabicPageNumber : function (pageFromTo) {
                    const fromToRegExprParsing = Pagination.fromToRegExprParsing.apply(this, [ pageFromTo ]);
                    const rowsPerPage = (fromToRegExprParsing.toRow - fromToRegExprParsing.fromRow) + 1;
                    const actualPageNumber = fromToRegExprParsing.toRow / rowsPerPage;

                    if (rowsPerPage !== 2) {
                        throw `rowsPerPage must be 2 (${rowsPerPage})`;
                    }

                    return actualPageNumber;
                },
                paginate : function (arabic: number) {
                    return `Sp. ${(arabic * 2) - 1}-${arabic * 2}`;
                }
            } ];
    }


    export interface PaginationMethod {
        name: string;
        test(label: string): boolean;
        getArabicPageNumber(val: string): number;
        paginate(val: number): string;
    }
}
