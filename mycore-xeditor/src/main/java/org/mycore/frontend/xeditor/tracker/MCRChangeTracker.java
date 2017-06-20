package org.mycore.frontend.xeditor.tracker;

import java.util.Iterator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.ProcessingInstruction;
import org.jdom2.filter.Filters;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;

public class MCRChangeTracker implements Cloneable {

    private static final String CONFIG_PREFIX = "MCR.XEditor.ChangeTracker.";

    public static final String PREFIX = "xed-tracker-";

    private int counter = 0;

    public void track(MCRChangeData data) {
        ProcessingInstruction pi = data.getProcessingInstruction();
        pi.setTarget(PREFIX + (++counter) + "-" + pi.getTarget());
        data.getContext().addContent(data.getPosition(), pi);
    }

    public int getChangeCounter() {
        return counter;
    }

    public void undoChanges(Document doc) {
        undoChanges(doc, 0);
    }

    public void undoChanges(Document doc, int stepNumber) {
        while (counter > stepNumber)
            undoLastChange(doc);
    }

    public String undoLastBreakpoint(Document doc) {
        while (counter > 0) {
            MCRChangeData change = undoLastChange(doc);
            if ("breakpoint".equals(change.getType()))
                return change.getText();
        }
        return null;
    }

    public MCRChangeData undoLastChange(Document doc) {
        MCRChangeData data = findLastChange(doc);
        data.getProcessingInstruction().detach();
        counter--;

        String property = CONFIG_PREFIX + data.getType() + ".Class";
        MCRChange change = (MCRChange) (MCRConfiguration.instance().getSingleInstanceOf(property));
        change.undo(data);
        return data;
    }

    public MCRChangeData findLastChange(Document doc) {
        String typePrefix = PREFIX + counter + "-";
        for (ProcessingInstruction instruction : doc.getDescendants(Filters.processinginstruction())) {
            String target = instruction.getTarget();

            if (target.startsWith(typePrefix))
                return new MCRChangeData(instruction, typePrefix);
        }
        throw new MCRException("Lost processing instruction for undo, not found: " + typePrefix);
    }

    public static Document removeChangeTracking(Document doc) {
        Document clone = doc.clone();
        removeChangeTracking(clone.getRootElement());
        return clone;
    }

    public static void removeChangeTracking(Element element) {
        for (Iterator<ProcessingInstruction> iter = element.getDescendants(Filters.processinginstruction())
            .iterator(); iter.hasNext();) {
            if (iter.next().getTarget().startsWith(PREFIX))
                iter.remove();
        }
    }

    @Override
    public MCRChangeTracker clone() {
        MCRChangeTracker tracker = new MCRChangeTracker();
        tracker.counter = this.counter;
        return tracker;
    }
}
