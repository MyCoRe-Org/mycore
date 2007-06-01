/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
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
        activated, created, destroyed, passivated;
    }

    private Type type;

    private MCRSession session;

    public MCRSessionEvent(MCRSession session, Type type) {
        this.session = session;
        this.type = type;
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

    public String toString() {
        StringBuilder sb=new StringBuilder();
        sb.append("MCRSessionEvent['");
        sb.append(getSession());
        sb.append("','");
        sb.append(getType());
        sb.append("']'");
        return sb.toString();
    }

}
