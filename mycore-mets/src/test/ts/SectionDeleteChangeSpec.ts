///<reference path="TestUtils.ts"/>

namespace org.mycore.mets.tests {

    describe("SectionDeleteChange", () => {

        let model: org.mycore.mets.model.MetsEditorModel;
        let simpleChange: org.mycore.mets.model.state.SectionDeleteChange;
        let elementToDelete: org.mycore.mets.model.simple.MCRMetsSection;

        let emptyMessages = {};

        beforeEach(() => {
            model = TestUtils.createDefaultModel();
            elementToDelete = model.metsModel.rootSection.metsSectionList[ 0 ];
            simpleChange = new org.mycore.mets.model.state.SectionDeleteChange(elementToDelete);
        });

        it("can be executed", () => {
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).toBe(elementToDelete);
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).not.toBe(elementToDelete);
        });

        it("can be executed and reverted", () => {
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).toBe(elementToDelete);
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).not.toBe(elementToDelete);
            model.stateEngine.back();
            expect(model.metsModel.rootSection.metsSectionList[ 0 ]).toBe(elementToDelete);
        });

        it("has a description", () => {
            let description = TestUtils.getWords(simpleChange.getDescription(emptyMessages));
            expect(description).toContain(elementToDelete.label);
            expect(description).toContain(model.metsModel.rootSection.label);
        });
    });
}
