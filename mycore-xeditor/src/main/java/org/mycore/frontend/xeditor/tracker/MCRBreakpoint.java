package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Element;

public class MCRBreakpoint implements MCRChange {

    public static MCRChangeData setBreakpoint(Element context, String label) {
        return new MCRChangeData("breakpoint", label, context.getContentSize(), context);
    }

    public void undo(MCRChangeData data) {
    }
}
