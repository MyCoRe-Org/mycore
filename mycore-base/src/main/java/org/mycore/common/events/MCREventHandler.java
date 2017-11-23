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

package org.mycore.common.events;

import org.mycore.common.MCRException;

/**
 * Objects that implement this interface can react when some kind of predefined
 * event happens in MyCoRe. Implementing classes are registered using the
 * configuration property
 * 
 * MCR.EventHandler.[objType].X.Class=[package and class name]
 * 
 * where [objType] is the object type like "MCRObject" or "MCRFile" and X is a
 * number starting from 1. For event handlers that are indexers of the searcher
 * package, there is a special syntax
 * 
 * MCR.EventHandler.[objType].X.Indexer=[searcherID]
 * 
 * where [searcherID] is the ID of the searcher that also is an indexer. Event
 * handlers are called in the same order as they are registered in the
 * properties file.
 * 
 * @author Frank Lützenkirchen
 */
public interface MCREventHandler {
    /**
     * Handles an event. The handler is responsible for filtering the event type
     * it is interested in and wants to react on.
     * 
     * @param evt
     *            the Event object containing information about the event
     */
    void doHandleEvent(MCREvent evt) throws MCRException;

    /**
     * Handles rollback of event handling. The handler should roll back the
     * changes that previously were made for this event, because a successor in
     * the event handler list caused an exception.
     * 
     * @param evt
     *            the Event object containing information about the event
     */
    void undoHandleEvent(MCREvent evt) throws MCRException;
}
