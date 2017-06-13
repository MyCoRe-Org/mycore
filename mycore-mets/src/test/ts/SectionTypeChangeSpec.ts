///<reference path="TestUtils.ts"/>

namespace org.mycore.mets.tests {
    describe("SectionTypeChange", () => {

        let model: org.mycore.mets.model.MetsEditorModel;
        let simpleChange: org.mycore.mets.model.state.SectionTypeChange;

        let newType = "new_section_type";
        let oldType;
        let emptyMessages = {};

        beforeEach(() => {
            model = TestUtils.createDefaultModel();
            simpleChange = new org.mycore.mets.model.state.SectionTypeChange(model.metsModel.rootSection, newType);
            oldType = model.metsModel.rootSection.type;
            expect(oldType).toBeDefined();
        });

        it("can be executed", () => {
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.type).toBe(newType);
        });

        it("can be executed and reverted", () => {
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.type).toBe(newType);
            expect(model.stateEngine.canBack).toBeTruthy();
            model.stateEngine.back();
            expect(model.metsModel.rootSection.type).toBe(oldType);
        });

        it("has a description", () => {
            let changeDescription = TestUtils.getWords(simpleChange.getDescription(emptyMessages));
            expect(changeDescription).toContain(newType);
            expect(changeDescription).toContain(oldType);
            expect(changeDescription).toContain(model.metsModel.rootSection.label);
        });
    });
}
