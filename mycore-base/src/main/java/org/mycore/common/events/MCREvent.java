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

/**
 * Represents an event that occured in the MyCoRe system. Events are of a
 * predefined event type like create, update, delete and an object type like
 * object or file. They can be handled by MCREventHandler implementations.
 * Events are automatically created by some MyCoRe components and are forwarded
 * to the handlers by MCREventManager.
 * 
 * @author Frank Lützenkirchen
 */
public class MCREvent extends java.util.Hashtable<String, Object> {
    /**
     * Default version ID
     */
    private static final long serialVersionUID = 1L;

    /** Pre-defined event types * */
    public static final String CREATE_EVENT = "create";

    public static final String UPDATE_EVENT = "update";

    public static final String DELETE_EVENT = "delete";

    public static final String REPAIR_EVENT = "repair";

    public static final String INDEX_EVENT = "index";

    public static final String OBJECT_TYPE = "MCRObject";

    public static final String DERIVATE_TYPE = "MCRDerivate";

    public static final String CLASS_TYPE = "MCRClassification";

    public static final String PATH_TYPE = "MCRPath";

    public static final String MOVE_EVENT = "move";

    public static final String PATH_KEY = PATH_TYPE;

    public static final String FILEATTR_KEY = PATH_TYPE + ":attr";

    public static final String OBJECT_KEY = "object";

    public static final String OBJECT_OLD_KEY = "object.old";

    public static final String DERIVATE_KEY = "derivate";

    public static final String DERIVATE_OLD_KEY = "derivate.old";

    /** The object type like object or file * */
    private String objType;

    /** The event type like create, update or delete * */
    private String evtType;

    /**
     * Creates a new event object of the given object type (object, file) and
     * event type (create, update, delete)
     */
    public MCREvent(String objType, String evtType) {
        this.objType = objType;
        this.evtType = evtType;
    }

    /**
     * Returns the object type of this event
     * 
     * @return the object type of this event
     */
    public String getObjectType() {
        return objType;
    }

    /**
     * Returns the event type of this event
     * 
     * @return the event type of this event
     */
    public String getEventType() {
        return evtType;
    }
}
