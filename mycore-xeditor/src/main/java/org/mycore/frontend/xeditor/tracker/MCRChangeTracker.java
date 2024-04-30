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

import java.util.List;
import java.util.Stack;

/**
 * Tracks changes to the edited xml, allowing to undo them step by step.  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRChangeTracker {

    private Stack<MCRChange> changes = new Stack<MCRChange>();

    public void track(MCRChange change) {
        this.changes.add(change);
    }

    public void track(List<MCRChange> changes) {
        this.changes.addAll(changes);
    }

    public int getChangeCount() {
        return changes.size();
    }

    public void undoChanges() {
        while (!changes.isEmpty())
            undoLastChange();
    }

    public void undoChanges(int stepNumber) {
        while (getChangeCount() > stepNumber) {
            undoLastChange();
        }
    }

    public String undoLastBreakpoint() {
        while (getChangeCount() > 0) {
            MCRChange change = undoLastChange();
            if (change instanceof MCRBreakpoint) {
                return change.getMessage();
            }
        }
        return null;
    }

    public MCRChange undoLastChange() {
        MCRChange change = changes.pop();
        change.undo();
        return change;
    }

    public MCRChange getLastChange() {
        return changes.peek();
    }
}
