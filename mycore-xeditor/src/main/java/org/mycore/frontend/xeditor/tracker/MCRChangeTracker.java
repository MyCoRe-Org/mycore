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

package org.mycore.frontend.xeditor.tracker;

import java.util.Stack;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.mycore.common.MCRException;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * Tracks changes to the edited xml, allowing to undo them step by step.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRChangeTracker implements Cloneable {

    private Stack<MCRChange> changes = new Stack<MCRChange>();

    public void track(MCRChange change) {
        changes.add(change);
    }

    public int getChangeCount() {
        return changes.size();
    }

    public void undoChanges(Document doc) {
        undoChanges(doc, 0);
    }

    public void undoChanges(Document doc, int stepNumber) {
        while (getChangeCount() > stepNumber) {
            undoLastChange(doc);
        }
    }

    public String undoLastBreakpoint(Document doc) {
        while (getChangeCount() > 0) {
            MCRChange change = undoLastChange(doc);
            if (change instanceof MCRBreakpoint) {
                return change.getMessage();
            }
        }
        return null;
    }

    public MCRChange undoLastChange(Document doc) {
        MCRChange change = changes.pop();
        try {
            change.undo(new MCRBinding(doc));
        } catch (JaxenException ex) {
            throw new MCRException(ex);
        }
        return change;
    }

    public MCRChange getLastChange() {
        return changes.peek();
    }

    @Override
    public MCRChangeTracker clone() {
        MCRChangeTracker tracker = new MCRChangeTracker();
        tracker.changes.addAll(this.changes);
        return tracker;
    }
}
