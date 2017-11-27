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

import org.mycore.common.MCRSession;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRSessionEvent {

    public enum Type {
        activated, created, destroyed, passivated
    }

    private Type type;

    private MCRSession session;

    private int concurrentAccessors;

    public MCRSessionEvent(MCRSession session, Type type, int concurrentAccessors) {
        this.session = session;
        this.type = type;
        this.concurrentAccessors = concurrentAccessors;
    }

    /**
     * Return how many threads accessed the session at time the event occured.
     */
    public int getConcurrentAccessors() {
        return concurrentAccessors;
    }

    /**
     * Return the MCRSession on which this event occured. 
     */
    public MCRSession getSession() {
        return session;
    }

    /**
     * Return the event type of this event. 
     */
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "MCRSessionEvent['" + getSession() + "'," + getType() + "," + getConcurrentAccessors() + "]'";
    }

}
