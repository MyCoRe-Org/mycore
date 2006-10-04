/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user2;

import java.sql.Timestamp;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * This is the abstract super class of MCRUser and MCRGroup
 * 
 * @see org.mycore.user2.MCRUser
 * @see org.mycore.user2.MCRGroup
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
abstract class MCRUserObject {
    protected static Logger logger = Logger.getLogger(MCRUserObject.class.getName());

    protected static MCRConfiguration config = null;
    
    protected static MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

    /** The maximum length of user and group names */
    public static final int id_len = 20;

    /** The maximum length of a password */
    public static final int password_len = 128;

    /** The maximum length of a decription */
    public static final int description_len = 200;

    /** The maximum length of a privilege */
    public static final int privilege_len = 100;

    /** The ID of the MyCoRe user object (either user ID or group ID) */
    protected String ID = "";

    /** Specifies the user responsible for the creation of this user object */
    protected String creator = "";

    /** The date and time of creation of the user object in the MyCoRe system */
    protected Timestamp creationDate = null;

    /** The date and time of the last modification of this user object */
    protected Timestamp modifiedDate = null;

    /** Description of the user object */
    protected String description = "";

    /**
     * The constructor for an empty object. Only the logger is defined.
     */
    public MCRUserObject() {
        ID = "";
        creator = "";
        creationDate = new Timestamp(new GregorianCalendar().getTime().getTime());
        modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());
        description = "";
        config = MCRConfiguration.instance();
    }

    /**
     * This method sets the attribute creationDate to the time provided as a
     * parameter.
     * 
     * @param time
     *            Timestamp to set the creation date
     */
    public final void setCreationDate(Timestamp time) {
        if (modificationIsAllowed()) {
            creationDate = time;
        }
    }

    /**
     * This method sets the attribute creationDate to the time the method is
     * called.
     */
    public final void setCreationDate() {
        if (modificationIsAllowed()) {
            creationDate = new Timestamp(new GregorianCalendar().getTime().getTime());
        }
    }

    /**
     * This method sets the attribute modifiedDate to the time provided as a
     * parameter.
     * 
     * @param time
     *            Timestamp to set the modified date
     */
    public final void setModifiedDate(Timestamp time) {
        if (modificationIsAllowed()) {
            modifiedDate = time;
        }
    }

    /**
     * This method sets the attribute modifiedDate to the time the method is
     * called.
     */
    public final void setModifiedDate() {
        if (modificationIsAllowed()) {
            modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());
        }
    }

    /**
     * This method sets the creator.
     * 
     * @param creator
     *            the creator of a user or group
     */
    public final void setCreator(String creator) {
        if (modificationIsAllowed()) {
            this.creator = trim(creator);
        }
    }

    /**
     * @return This method returns the creation date (timestamp) of the user
     *         object.
     */
    public final Timestamp getCreationDate() {
        return creationDate;
    }

    /**
     * @return This method returns the time of the last modifications
     *         (timestamp) of the user object.
     */
    public final Timestamp getModifiedDate() {
        return modifiedDate;
    }

    /**
     * @return This method returns the user ID of the creator of this user
     *         object.
     */
    public final String getCreator() {
        return creator;
    }

    /**
     * @return This method returns the description of the user object.
     */
    public final String getDescription() {
        return description;
    }
    
    public final void setDescription(String value) {
        description = value;
    }

    /**
     * This method must be implemented by a subclass and then returns the user
     * or group object as a JDOM document.
     */
    abstract public org.jdom.Document toJDOMDocument() throws MCRException;

    /**
     * This method must be implemented by a subclass and then returns the user
     * or group object as a JDOM element.
     */
    abstract public org.jdom.Element toJDOMElement() throws MCRException;

    /**
     * This method must be implemented by a subclass and the returns true if the
     * current user or session may modify the current user object, false
     * otherwise.
     */
    abstract public boolean modificationIsAllowed() throws MCRException;

    /**
     * This method must be implemented by a subclass and returns the the ID of
     * the object.
     */
    abstract public String getID(); // Although the implementation just returns

    // the ID-string
    // it cannot be implemented only in this super class of MCRUser und MCRGroup
    // due to the ACL system.

    /**
     * This helper method replaces null with an empty string and trims
     * whitespace from non-null strings.
     */
    protected final static String trim(String s) {
        return (s != null) ? s.trim() : "";
    }

    /**
     * This helper method replaces null with an empty string and trims
     * whitespace from non-null strings.
     */
    protected final static String trim(String s, int len) {
        String sn = (s != null) ? s.trim() : "";

        if (sn.length() > len) {
            logger.warn("The string \'" + sn + "\' is too long (max. " + Integer.toString(len) + ").");

            return sn.substring(0, len);
        }

        return sn;
    }

    /**
     * This method checks if all required fields have been provided. In a later
     * stage of the software development a User Policy object will be asked,
     * which fields exactly are the required fields. This will be configurable.
     * 
     * @return returns true if all required fields have been provided
     */
    abstract public boolean isValid();
    
    /**
     * This method sends debug data to the logger (for the debug mode).
     */
    public final void debugDefault() {
        logger.debug("ID                 = " + ID);
        logger.debug("creator            = " + creator);
        logger.debug("creationDate       = " + String.valueOf(creationDate));
        logger.debug("modifiedDate       = " + String.valueOf(modifiedDate));
        logger.debug("description        = " + description);
    }

    /**
     * This method is only used for providing error messages in the access
     * control component and should be removed later.
     */
    public String toString() {
        return ID;
    }
}
