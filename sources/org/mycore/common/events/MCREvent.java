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

/**
 * Represents an event that occured in the MyCoRe system. Events are of a
 * predefined type like create, update, delete and can be handled by
 * MCREventHandler implementations. Events are automatically created by some
 * MyCoRe components and are forwarded to the handlers by MCREventManager.
 * 
 * @author Frank Lützenkirchen
 */
public class MCREvent extends java.util.Hashtable {
    /** Predefined event type metadata object created * */
    public static final int OBJECT_CREATED = 0;

    /** Predefined event type metadata object updated * */
    public static final int OBJECT_UPDATED = 1;

    /** Predefined event type metadata object deleted * */
    public static final int OBJECT_DELETED = 2;

    /** Predefined event type file content created * */
    public static final int FILE_CREATED = 3;

    /** Predefined event type file content updated * */
    public static final int FILE_UPDATED = 4;

    /** Predefined event type file content deleted * */
    public static final int FILE_DELETED = 5;

    private String[] typeStrings = { "object created", "object updated",
            "object deleted", "file created", "file updated", "file deleted" };

    private int type;

    /**
     * Creates a new event object of the given type
     */
    public MCREvent(int type) {
        this.type = type;
    }

    /**
     * Returns the type of this event
     * 
     * @return the type of this event
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the type of this event as a String for debugging
     * 
     * @return the type of this event as a String for debugging
     */
    public String getTypeString() {
        return typeStrings[type];
    }
}