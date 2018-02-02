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
    describe('SectionTypeChange', () => {

        let model: org.mycore.mets.model.MetsEditorModel;
        let simpleChange: org.mycore.mets.model.state.SectionTypeChange;

        const newType = 'new_section_type';
        let oldType;
        const emptyMessages = {};

        beforeEach(() => {
            model = utils.createDefaultModel();
            simpleChange = new org.mycore.mets.model.state.SectionTypeChange(model.metsModel.rootSection, newType);
            oldType = model.metsModel.rootSection.type;
            expect(oldType).toBeDefined();
        });

        it('can be executed', () => {
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.type).toBe(newType);
        });

        it('can be executed and reverted', () => {
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.type).toBe(newType);
            expect(model.stateEngine.canBack).toBeTruthy();
            model.stateEngine.back();
            expect(model.metsModel.rootSection.type).toBe(oldType);
        });

        it('has a description', () => {
            const changeDescription = utils.getWords(simpleChange.getDescription(emptyMessages));
            expect(changeDescription).toContain(newType);
            expect(changeDescription).toContain(oldType);
            expect(changeDescription).toContain(model.metsModel.rootSection.label);
        });
    });
}
