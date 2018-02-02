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

    import MCRMetsSection = org.mycore.mets.model.simple.MCRMetsSection;

    describe('SectionAddChange', () => {

        let model: org.mycore.mets.model.MetsEditorModel;
        let simpleChange: org.mycore.mets.model.state.SectionAddChange;
        let newSection: MCRMetsSection;
        const emptyMessages = {};

        beforeEach(() => {
            model = utils.createDefaultModel();
            newSection = new MCRMetsSection('id', 'type', 'NewSection');
            simpleChange = new org.mycore.mets.model.state.SectionAddChange(newSection, model.metsModel.rootSection);
        });

        it('can be executed', () => {
            model.stateEngine.changeModel(simpleChange);
            expect(newSection.parent).toBe(model.metsModel.rootSection);
            expect(model.metsModel.rootSection.metsSectionList).toContain(newSection);
        });

        it('can be executed and reverted', () => {
            model.stateEngine.changeModel(simpleChange);
            expect(newSection.parent).toBe(model.metsModel.rootSection);
            expect(model.metsModel.rootSection.metsSectionList).toContain(newSection);
            expect(model.stateEngine.canBack()).toBeTruthy();
            model.stateEngine.back();
            expect(newSection.parent).not.toBe(model.metsModel.rootSection);
            expect(model.metsModel.rootSection.metsSectionList).not.toContain(newSection);
        });

        it('has a description', () => {
            const descriptionWords = utils.getWords(simpleChange.getDescription(emptyMessages));
            expect(descriptionWords).toContain(newSection.label);
            expect(descriptionWords).toContain(model.metsModel.rootSection.label);
        });
    });
}
