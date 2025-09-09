/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.util.ArrayDeque;
import java.util.Deque;


/**
 * Tracks changes to the edited xml, allowing to undo them step by step.  
 * 
 * @author Frank Lützenkirchen
 */
public class MCRChangeTracker {


    /** A stack of performed changes **/
    private Deque<MCRTrackedAction> changes = new ArrayDeque<>();

    public void track(MCRTrackedAction change) {
        this.changes.push(change);
    }

    /** Convenience method to track progress **/
    public void setBreakpoint(String msg) {
        track(new MCRBreakpoint(msg));
    }

    /**
     * Returns the number of changes tracked 
     */
    public int getChangeCount() {
        return changes.size();
    }

    /**
     * Undo all changes tracked so far
     */
    public void undoChanges() {
        while (!changes.isEmpty()) {
            undoLastChange();
        }
    }

    /**
     * Undo all changes back to a given change count.
     * This allows traveling back in time of the XML's change history.
     * 
     * @param stepNumber the number of the change that should be gone back to
     */
    public void undoChanges(int stepNumber) {
        while (getChangeCount() > stepNumber) {
            undoLastChange();
        }
    }

    /**
     * Undo all changes back to before the last breakpoint 
     */
    public String undoLastBreakpoint() {
        while (getChangeCount() > 0) {
            MCRTrackedAction change = undoLastChange();
            if (change instanceof MCRBreakpoint) {
                return change.getMessage();
            }
        }
        return null;
    }

    /**
     * Undo the last change
     */
    public MCRTrackedAction undoLastChange() {
        MCRTrackedAction ta = changes.pop();
        if (ta instanceof MCRChange change) {
            change.undo();
        }
        return ta;
    }

    /**
     * Returns the last change that has been tracked
     */
    public MCRTrackedAction getLastChange() {
        return changes.peek();
    }
}
