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

    import MCRMetsPage = org.mycore.mets.model.simple.MCRMetsPage;
    import PagesMoveChange = org.mycore.mets.model.state.PagesMoveChange;

    describe('PagesMoveChange', () => {

        const p0 = new MCRMetsPage(Math.random().toString(16), 'p0', '', false);
        const p1 = new MCRMetsPage(Math.random().toString(16), 'p1', '', false);
        const p2 = new MCRMetsPage(Math.random().toString(16), 'p2', '', false);
        const p3 = new MCRMetsPage(Math.random().toString(16), 'p3', '', false);
        const p4 = new MCRMetsPage(Math.random().toString(16), 'p4', '', false);
        const p5 = new MCRMetsPage(Math.random().toString(16), 'p5', '', false);
        const p6 = new MCRMetsPage(Math.random().toString(16), 'p6', '', false);
        const p7 = new MCRMetsPage(Math.random().toString(16), 'p7', '', false);
        const p8 = new MCRMetsPage(Math.random().toString(16), 'p8', '', false);
        const p9 = new MCRMetsPage(Math.random().toString(16), 'p9', '', false);
        const p10 = new MCRMetsPage(Math.random().toString(16), 'p10', '', false);

        let pageList: MCRMetsPage[];
        let moveChange: PagesMoveChange;

        beforeEach(() => {
            pageList = [ p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10 ];
        });

        const copyFromEndToBegin = () => {
            moveChange = new PagesMoveChange(pageList, {from : p6, to : p8}, {before : true, element : p0});
            moveChange.doChange();

            expect(pageList[ 0 ]).toBe(p6);
            expect(pageList[ 1 ]).toBe(p7);
            expect(pageList[ 2 ]).toBe(p8);
            expect(pageList[ 3 ]).toBe(p0);
            expect(pageList[ 4 ]).toBe(p1);
            expect(pageList[ 5 ]).toBe(p2);
            expect(pageList[ 6 ]).toBe(p3);
            expect(pageList[ 7 ]).toBe(p4);
            expect(pageList[ 8 ]).toBe(p5);
        };

        const copyFromBeginToEnd = () => {
            moveChange = new PagesMoveChange(pageList, {from : p0, to : p3}, {before : false, element : p9});
            moveChange.doChange();

            expect(pageList[ 0 ]).toBe(p4);
            expect(pageList[ 1 ]).toBe(p5);
            expect(pageList[ 2 ]).toBe(p6);
            expect(pageList[ 3 ]).toBe(p7);
            expect(pageList[ 4 ]).toBe(p8);
            expect(pageList[ 5 ]).toBe(p9);
            expect(pageList[ 6 ]).toBe(p0);
            expect(pageList[ 7 ]).toBe(p1);
            expect(pageList[ 8 ]).toBe(p2);
            expect(pageList[ 9 ]).toBe(p3);
            expect(pageList[ 10 ]).toBe(p10);
        };

        const checkPagesInitialOrder = () => {
            expect(pageList[ 0 ]).toBe(p0);
            expect(pageList[ 1 ]).toBe(p1);
            expect(pageList[ 2 ]).toBe(p2);
            expect(pageList[ 3 ]).toBe(p3);
            expect(pageList[ 4 ]).toBe(p4);
            expect(pageList[ 5 ]).toBe(p5);
            expect(pageList[ 6 ]).toBe(p6);
            expect(pageList[ 7 ]).toBe(p7);
            expect(pageList[ 8 ]).toBe(p8);
            expect(pageList[ 9 ]).toBe(p9);
            expect(pageList[ 10 ]).toBe(p10);
        };

        it('can be executed (copy from end to begin)', copyFromEndToBegin);
        it('can be executed (copy from begin to end)', copyFromBeginToEnd);
        it('can be reverted (depends on: \'copy from end to begin\')', () => {
            copyFromEndToBegin();
            moveChange.unDoChange();
            checkPagesInitialOrder();
        });

        it('can be reverted (depends on: \'copy from begin to end\')', () => {
            copyFromBeginToEnd();
            moveChange.unDoChange();
            checkPagesInitialOrder();
        });
    });
}
