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

namespace org.mycore.mets.tests {
    describe("Pagination", () => {

        let paginationExample = {
            arabicNumbering : [ "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" ],
            rectoVerso_lowercase : [ "1r", "1v", "2r", "2v", "3r", "3v", "4r", "4v", "5r", "5v" ],
            ab_lowercase : [ "1a", "1b", "2a", "2b", "3a", "3b", "4a", "4b", "5a", "5b" ],
            letter : [ "U1", "U2", "U3", "U4", "U5", "U6", "U7", "U8", "U9", "U10" ],
            rome : [ "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x" ],
            rome_uppercase : [ "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" ]
        };

        beforeAll(() => {
            expect(org.mycore.mets.model.Pagination).toBeDefined();
            expect(org.mycore.mets.model.Pagination.paginationMethods).toBeDefined();
        });

        it("has a list of Pagination Methods", () => {
            expect(org.mycore.mets.model.Pagination.paginationMethods.length).toBeGreaterThan(1);
        });

        for (let paginationNameOuter in paginationExample) {
            let paginationName = paginationNameOuter + "";
            ((paginationName) => {
                it("can get can get Pagination Methods by name  (method : " + paginationName + ")",
                    () => {
                        expect(org.mycore.mets.model.Pagination.getPaginationMethodByName(paginationName)).toBeDefined();
                    });
            })(paginationNameOuter);
        }

        for (let paginationNameOuter in paginationExample) {
            ((paginationName) => {
                it("can parse numbers of the specific pagination method (method : " + paginationName + ")", () => {
                    let numberExamples = paginationExample[ paginationName ];
                    let method = org.mycore.mets.model.Pagination.getPaginationMethodByName(paginationName);

                    for (let i = 1; i <= 10; i++) {
                        let numberExample = numberExamples[ i - 1 ];
                        let arabicPageNumber = method.getArabicPageNumber(numberExample);
                        expect(arabicPageNumber).toBe(i);
                    }
                });
            })(paginationNameOuter);
        }


        for (let paginationNameOuter in paginationExample) {
            ((paginationName) => {
                it("can convert arabic numbers to numbers of the specific pagination method (method : " +
                    paginationName + ")", () => {
                    let method = org.mycore.mets.model.Pagination.getPaginationMethodByName(paginationName);
                    let numberExamples = paginationExample[ paginationName ];

                    for (let i = 1; i <= 10; i++) {
                        expect(method.paginate(i)).toBe(numberExamples[ i - 1 ]);
                    }
                });
            })(paginationNameOuter);
        }

    });
}
