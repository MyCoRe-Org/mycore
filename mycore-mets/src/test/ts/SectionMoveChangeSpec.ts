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

///<reference path="TestUtils.ts"/>

namespace org.mycore.mets.tests {
    describe('SectionMoveChange', () => {

        let model: org.mycore.mets.model.MetsEditorModel;
        let simpleChange: org.mycore.mets.model.state.SectionMoveChange;
        let sectionToMove: org.mycore.mets.model.simple.MCRMetsSection;
        let moveTarget: org.mycore.mets.model.DropTarget;
        const moveToIndex = 1;

        const emptyMessages = {};

        beforeEach(() => {
            model = utils.createDefaultModel();
            sectionToMove = model.metsModel.rootSection.metsSectionList[ 0 ];
        });

        it('can be executed(position=after)', () => {
            moveTarget = {position : 'after', element : model.metsModel.rootSection.metsSectionList[ moveToIndex ]};
            simpleChange = new org.mycore.mets.model.state.SectionMoveChange(sectionToMove, moveTarget);

            /**
             * [0] -> [1]
             * [1] -> [0]
             * [2] -> [2]
             */
            expect(model.metsModel.rootSection.metsSectionList.indexOf(sectionToMove)).toBe(0);
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.metsSectionList.indexOf(sectionToMove)).toBe(1);
        });

        it('can be executed(position=after) and reverted', () => {
            moveTarget = {position : 'after', element : model.metsModel.rootSection.metsSectionList[ moveToIndex ]};
            simpleChange = new org.mycore.mets.model.state.SectionMoveChange(sectionToMove, moveTarget);

            expect(model.metsModel.rootSection.metsSectionList.indexOf(sectionToMove)).toBe(0);
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.metsSectionList.indexOf(sectionToMove)).toBe(1);
            model.stateEngine.back();
            expect(model.metsModel.rootSection.metsSectionList.indexOf(sectionToMove)).toBe(0);
        });

        it('has a description', () => {
            moveTarget = {position : 'after', element : model.metsModel.rootSection.metsSectionList[ moveToIndex ]};
            simpleChange = new org.mycore.mets.model.state.SectionMoveChange(sectionToMove, moveTarget);

            const changeDescription = utils.getWords(simpleChange.getDescription(emptyMessages));
            expect(changeDescription).toContain(model.metsModel.rootSection.label);
            expect(changeDescription).toContain(moveTarget.position);
            expect(changeDescription).toContain(moveTarget.element.label);

        });
    });
}
