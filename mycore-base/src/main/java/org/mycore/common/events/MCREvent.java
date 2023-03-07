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

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * Represents an event that occured in the MyCoRe system. Events are of a
 * predefined event type like create, update, delete and an object type like
 * object or file. They can be handled by MCREventHandler implementations.
 * Events are automatically created by some MyCoRe components and are forwarded
 * to the handlers by MCREventManager.
 * 
 * @author Frank Lützenkirchen
 */
public class MCREvent {

    /** Pre-defined event types * */
    public enum EventType {
        CREATE,
        UPDATE,
        DELETE,
        REPAIR,
        INDEX,
        MOVE,
        /** required for ThULB/dbt, do not use **/
        INACTIVATE,
        /** required for ThULB/dbt, do not use **/
        REACTIVATE,
        /** required for ThULB/dbt, do not use **/
        OWNER_TRANSFER
    }

    /** Pre-defined event objects * */
    public enum ObjectType {
        OBJECT("MCRObject"),
        DERIVATE("MCRDerivate"),
        CLASS("MCRClassification"),
        PATH("MCRPath"),
        USER("MCRUser"),

        /** required for ThULB/dbt, do not use **/
        SLOT("slot"),

        /** required for ThULB/dbt, do not use **/
        ENTRY("entry");

        private String className;

        ObjectType(String className) {
            this.className = className;
        }

        public String getClassName() {
            return this.className;
        }

        public static ObjectType fromClassName(String className) {
            for (ObjectType b : ObjectType.values()) {
                if (b.className.equalsIgnoreCase(className)) {
                    return b;
                }
            }
            return null;
        }
    }

    public static final String PATH_KEY = "MCRPath";

    public static final String FILEATTR_KEY = PATH_KEY + ":attr";

    public static final String OBJECT_KEY = "object";

    public static final String OBJECT_OLD_KEY = "object.old";

    public static final String DERIVATE_KEY = "derivate";

    public static final String DERIVATE_OLD_KEY = "derivate.old";

    public static final String USER_KEY = "user";

    public static final String USER_OLD_KEY = "user.old";

    public static final String CLASS_KEY = "class";

    public static final String CLASS_OLD_KEY = "class.old";

    /** The object type like object or file * */
    private ObjectType objType;

    /** The event type like create, update or delete * */
    private EventType evtType;

    /** A hashtable to store event related, additional data */
    private Hashtable<String, Object> data = new Hashtable<>();

    /**
     * Creates a new event object of the given object type (object, file) and
     * event type (create, update, delete)
     */
    public MCREvent(ObjectType objType, EventType evtType) {
        this.objType = objType;
        this.evtType = evtType;
    }

    /**
     * Returns the object type of this event
     * 
     * @return the object type of this event
     */
    public ObjectType getObjectType() {
        return objType;
    }

    /**
     * Returns the event type of this event
     * 
     * @return the event type of this event
     */
    public EventType getEventType() {
        return evtType;
    }

    /**
     * returns an object from event data
     * 
     * @param key - the object key
     * @return an object from event data
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * adds an object to the event data
     * @param key - the key for the object
     * @param value - the object itself
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * return the entries of the event data
     * (1x called in wfc.mail.MCRMailEventhandler) 
     * @return the entrySet of the the data of the event
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        return data.entrySet();
    }
}
