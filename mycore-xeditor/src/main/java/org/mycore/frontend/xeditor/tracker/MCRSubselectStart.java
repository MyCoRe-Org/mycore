package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Element;

public class MCRSubselectStart implements MCRChange {

    public static MCRChangeData startSubselect(Element context, String xPath) {
        return new MCRChangeData("subselect-start", xPath, 0, context);
    }

    public void undo(MCRChangeData data) {
    }
}