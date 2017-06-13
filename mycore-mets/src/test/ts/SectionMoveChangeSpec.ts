///<reference path="TestUtils.ts"/>

namespace org.mycore.mets.tests {
    describe("SectionMoveChange", () => {

        let model: org.mycore.mets.model.MetsEditorModel;
        let simpleChange: org.mycore.mets.model.state.SectionMoveChange;
        let sectionToMove: org.mycore.mets.model.simple.MCRMetsSection;
        let moveTarget: org.mycore.mets.model.DropTarget;
        let moveToIndex = 1;

        let emptyMessages = {};

        beforeEach(() => {
            model = TestUtils.createDefaultModel();
            sectionToMove = model.metsModel.rootSection.metsSectionList[ 0 ];
        });

        it("can be executed(position=after)", () => {
            moveTarget = {position : "after", element : model.metsModel.rootSection.metsSectionList[ moveToIndex ]};
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

        it("can be executed(position=after) and reverted", () => {
            moveTarget = {position : "after", element : model.metsModel.rootSection.metsSectionList[ moveToIndex ]};
            simpleChange = new org.mycore.mets.model.state.SectionMoveChange(sectionToMove, moveTarget);

            expect(model.metsModel.rootSection.metsSectionList.indexOf(sectionToMove)).toBe(0);
            model.stateEngine.changeModel(simpleChange);
            expect(model.metsModel.rootSection.metsSectionList.indexOf(sectionToMove)).toBe(1);
            model.stateEngine.back();
            expect(model.metsModel.rootSection.metsSectionList.indexOf(sectionToMove)).toBe(0);
        });

        it("has a description", () => {
            moveTarget = {position : "after", element : model.metsModel.rootSection.metsSectionList[ moveToIndex ]};
            simpleChange = new org.mycore.mets.model.state.SectionMoveChange(sectionToMove, moveTarget);

            let changeDescription = TestUtils.getWords(simpleChange.getDescription(emptyMessages));
            expect(changeDescription).toContain(model.metsModel.rootSection.label);
            expect(changeDescription).toContain(moveTarget.position);
            expect(changeDescription).toContain(moveTarget.element.label);

        });
    });
}
