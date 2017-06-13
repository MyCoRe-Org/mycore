///<reference path="TestUtils.ts"/>
namespace org.mycore.mets.tests {
    describe("PageLabelChange", () => {


        let model: org.mycore.mets.model.MetsEditorModel;
        let simpleChange: org.mycore.mets.model.state.PageLabelChange;

        let newLabel = "new_page_name";
        let oldLabel;
        let emptyMessages = {};

        beforeEach(() => {
            model = TestUtils.createDefaultModel();
            simpleChange = new org.mycore.mets.model.state.PageLabelChange(model.metsModel.metsPageList[ 0 ], newLabel);
            oldLabel = model.metsModel.metsPageList[ 0 ].orderLabel;
            expect(oldLabel).toBeDefined();
        });

        it("can be executed", () => {
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.metsPageList[ 0 ].orderLabel).toBe(newLabel);

        });

        it("can be executed and reverted", () => {
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.metsPageList[ 0 ].orderLabel).toBe(newLabel);
            expect(model.stateEngine.canBack).toBeTruthy();
            model.stateEngine.back();
            expect(model.metsModel.metsPageList[ 0 ].orderLabel).toBe(oldLabel);
        });

        it("has a description", () => {
            let changeDescription = TestUtils.getWords(simpleChange.getDescription(emptyMessages));
            expect(changeDescription).toContain(newLabel);
            expect(changeDescription).toContain(oldLabel);
        });

    });
}
