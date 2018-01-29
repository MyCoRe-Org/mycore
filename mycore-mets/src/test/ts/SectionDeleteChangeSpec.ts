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

    describe('SectionDeleteChange', () => {

        let model: org.mycore.mets.model.MetsEditorModel;
        let simpleChange: org.mycore.mets.model.state.SectionDeleteChange;
        let elementToDelete: org.mycore.mets.model.simple.MCRMetsSection;

        const emptyMessages = {};

        beforeEach(() => {
            model = utils.createDefaultModel();
            elementToDelete = model.metsModel.rootSection.metsSectionList[ 0 ];
            simpleChange = new org.mycore.mets.model.state.SectionDeleteChange(elementToDelete);
        });

        it('can be executed', () => {
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).toBe(elementToDelete);
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).not.toBe(elementToDelete);
        });

        it('can be executed and reverted', () => {
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).toBe(elementToDelete);
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).not.toBe(elementToDelete);
            model.stateEngine.back();
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).toBe(elementToDelete);
        });

        it('has a description', () => {
            const description = utils.getWords(simpleChange.getDescription(emptyMessages));
            expect(description).toContain(elementToDelete.label);
            expect(description).toContain(model.metsModel.rootSection.label);
        });
    });
}
