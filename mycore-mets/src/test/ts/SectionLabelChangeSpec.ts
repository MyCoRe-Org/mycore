///<reference path="TestUtils.ts"/>
namespace org.mycore.mets.tests {

    describe("SectionLabelChange", () => {

        let model: org.mycore.mets.model.MetsEditorModel;
        let simpleChange: org.mycore.mets.model.state.SectionLabelChange;

        let newLabel = "new_section_name";
        let oldLabel;
        let emptyMessages = {};

        beforeEach(() => {
            model = TestUtils.createDefaultModel();
            simpleChange = new org.mycore.mets.model.state.SectionLabelChange(model.metsModel.rootSection, newLabel);
            oldLabel = model.metsModel.rootSection.label;
            expect(oldLabel).toBeDefined();
        });

        it("can be executed", () => {
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.label).toBe(newLabel);
        });

        it("can be executed and reverted", () => {
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.label).toBe(newLabel);
            expect(model.stateEngine.canBack).toBeTruthy();
            model.stateEngine.back();
            expect(model.metsModel.rootSection.label).toBe(oldLabel);
        });

        it("has a description", () => {
            let changeDescription = TestUtils.getWords(simpleChange.getDescription(emptyMessages));
            expect(changeDescription).toContain(newLabel);
            expect(changeDescription).toContain(oldLabel);
        });
    });
}
