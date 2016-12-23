///<reference path="../model/simple/MCRMetsSection.ts"/>
namespace org.mycore.mets.controller {

    import MCRMetsSection = org.mycore.mets.model.simple.MCRMetsSection;

    export interface StartEditLabel {
        ofSection: MCRMetsSection;
    }

    export interface EditLabelStarted {
        ofSection: MCRMetsSection;
    }

    export interface SectionAdded {
        parent: MCRMetsSection;
        addedSection: MCRMetsSection;
    }

}
