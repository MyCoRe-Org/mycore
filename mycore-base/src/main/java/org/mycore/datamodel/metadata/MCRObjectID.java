/*
 * 
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.datamodel.metadata;

import java.text.DecimalFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

/**
 * This class holds all informations and methods to handle the MyCoRe Object ID.
 * The MyCoRe Object ID is a special ID to identify each metadata object with
 * three parts, they are the project identifier, the type identifier and a
 * string with a number. The syntax of the ID is "<em>projectID</em>_
 * <em>typeID</em>_ <em>number</em>" as "<em>String_String_Integer</em>".
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date: 2010-12-29 09:18:22 +0100 (Wed, 29 Dec
 *          2010) $
 */
public final class MCRObjectID {
    /**
     * public constant value for the MCRObjectID length
     */
    public static final int MAX_LENGTH = 64;

    // configuration values
    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    // counter for the next IDs per project base ID
    private static HashMap<String, Integer> lastnumber = new HashMap<String, Integer>();

    // data of the ID
    private String mcr_project_id = null;

    private String mcr_type_id = null;

    private String mcr_id = null;

    private int mcr_number = -1;

    private static final MCRObjectIDFormat idFormat = new MCRObjectIDDefaultFormat();

    private static final Logger LOGGER = Logger.getLogger(MCRObjectID.class);

    public interface MCRObjectIDFormat {
        public int numberDistance();

        public DecimalFormat numberFormat();
    }

    private static class MCRObjectIDDefaultFormat implements MCRObjectIDFormat {

        private int numberDistance;

        /**
         * First invocation may return MCR.Metadata.ObjectID.InitialNumberDistance if set,
         * following invocations will return MCR.Metadata.ObjectID.NumberDistance.
         * The default for both is 1.
         */
        @Override
        public int numberDistance() {
            if (numberDistance == 0) {
                MCRConfiguration config = MCRConfiguration.instance();
                numberDistance = config.getInt("MCR.Metadata.ObjectID.NumberDistance", 1);
                return config.getInt("MCR.Metadata.ObjectID.InitialNumberDistance", numberDistance);
            }
            return numberDistance;
        }

        @Override
        public DecimalFormat numberFormat() {
            String numberPattern = MCRConfiguration.instance().getString("MCR.Metadata.ObjectID.NumberPattern", "0000000000");
            return new DecimalFormat(numberPattern);
        }

    }

    /**
     * The constructor for MCRObjectID from a given string.
     * 
     * @exception MCRException
     *                if the given string is not valid.
     * @deprecated use {@link #getInstance(String)} instead
     */
    public MCRObjectID(String id) throws MCRException {
        if (!setID(id)) {
            throw new MCRException("The ID is not valid: " + id + " , it should match the pattern String_String_Integer");
        }
    }

    /**
     * Returns a MCRObjectID from a given base ID string. A base ID is
     * <em>project_id</em>_<em>type_id</em>. The number is computed by this
     * method. It is the next free number of an item in the database for the
     * given project ID and type ID, with the following additional restriction:
     * The ID returned can be divided by idFormat.numberDistance without rest.
     * The ID returned minus the last ID returned is at least idFormat.numberDistance.
     * 
     * Example for number distance of 1 (default):
     *   last ID = 7, next ID = 8
     *   last ID = 8, next ID = 9
     *   
     * Example for number distance of 2:
     *   last ID = 7, next ID = 10
     *   last ID = 8, next ID = 10
     *   last ID = 10, next ID = 20
     * 
     * @param base_id
     *            <em>project_id</em>_<em>type_id</em>
     */
    public static synchronized MCRObjectID getNextFreeId(String base_id) {
        return getNextFreeId(base_id, 0);
    }

    /**
     * Returns the next free MCRObjectID based on the given type string.
     *  
     * @param type
     * 
     * @see @link {@link MCRObjectID#getNextFreeId(String)} 
     * 
     */
    public static synchronized MCRObjectID getNextFreeIdByType(String type) {
        String prefix = CONFIG.getString("MCR.SWF.Project.ID." + type, CONFIG.getString("MCR.SWF.Project.ID"));
        return MCRObjectID.getNextFreeId(prefix + "_" + type);
    }

    /**
     * Returns a MCRObjectID from a given base ID string. Same as
     * {@link #getNextFreeId(String)} but the additional parameter acts as a
     * lower limit for integer part of the ID.
     * 
     * @param base_id
     *            <em>project_id</em>_<em>type_id</em>
     * @param maxInWorkflow
     *            returned integer part of id will be at least
     *            <code>maxInWorkflow + 1</code>
     */
    public static synchronized MCRObjectID getNextFreeId(String base_id, int maxInWorkflow) {
        int last = Math.max(getLastIDNumber(base_id), maxInWorkflow);
        int numberDistance = idFormat.numberDistance();
        int next = last + numberDistance;

        int rest = next % numberDistance;
        if (rest != 0)
            next += numberDistance - rest;

        lastnumber.put(base_id, next);
        String[] idParts = getIDParts(base_id);
        return getInstance(formatID(idParts[0], idParts[1], next));
    }

    /**
     * Returns the last ID number used or reserved for the given object base
     * type. This may return the value 0 when there is no ID last used or in the
     * store.
     */
    private static int getLastIDNumber(String base_id) {
        int lastIDKnown = lastnumber.containsKey(base_id) ? lastnumber.get(base_id) : 0;

        String[] idParts = getIDParts(base_id);
        int highestStoredID = MCRXMLMetadataManager.instance().getHighestStoredID(idParts[0], idParts[1]);

        return Math.max(lastIDKnown, highestStoredID);
    }

    /**
     * Returns the last ID used or reserved for the given object base type.
     * 
     * @return a valid MCRObjectID, or null when there is no ID for the given
     *         type
     */
    public static MCRObjectID getLastID(String base_id) {
        int lastIDNumber = getLastIDNumber(base_id);
        if (lastIDNumber == 0)
            return null;

        String[] idParts = getIDParts(base_id);
        return getInstance(formatID(idParts[0], idParts[1], lastIDNumber));
    }

    /**
     * This method instantiate this class with a given identifier in MyCoRe schema.
     * 
     * @param id
     *          the MCRObjectID
     * @return an MCRObjectID class instance
     * @exception MCRException if the given identifier is not valid
     */
    public static MCRObjectID getInstance(String id) {
        return MCRObjectIDPool.getMCRObjectID(id);
    }

    public static void main(String[] a) {
        MCRObjectID.getInstance("Bluadash");
    }

    /**
     * This method get the string with <em>project_id</em>. If the ID is not
     * valid, an empty string was returned.
     * 
     * @return the string of the project id
     */
    public final String getProjectId() {
        return mcr_project_id;
    }

    /**
     * This method gets the string with <em>type_id</em>. If the ID is not
     * valid, an empty string will be returned.
     * 
     * @return the string of the type id
     */
    public final String getTypeId() {
        return mcr_type_id;
    }

    /**
     * This method gets the string with <em>number</em>. If the ID is not valid,
     * an empty string will be returned.
     * 
     * @return the string of the number
     */
    public final String getNumberAsString() {
        return idFormat.numberFormat().format(mcr_number);
    }

    /**
     * This method gets the integer with <em>number</em>. If the ID is not
     * valid, -1 will be returned.
     * 
     * @return the number as integer
     */
    public final int getNumberAsInteger() {
        return mcr_number;
    }

    /**
     * This method gets the basic string with <em>project_id</em>_
     * <em>type_id</em>. If the Id is not valid, an empty string will be
     * returned.
     * 
     * @return the string of the schema name
     */
    public String getBase() {
        return mcr_project_id + "_" + mcr_type_id;
    }

    /**
     * Normalizes to a object ID of form <em>project_id</em>_ <em>type_id</em>_
     * <em>number</em>, where number has leading zeros.
     * 
     * @param projectID
     * @param type
     * @param number
     * @return <em>project_id</em>_<em>type_id</em>_<em>number</em>
     */
    public static String formatID(String projectID, String type, int number) {
        if (projectID == null) {
            throw new IllegalArgumentException("projectID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (number < 0) {
            throw new IllegalArgumentException("number must be non negative integer");
        }
        return projectID + '_' + type.toLowerCase() + '_' + idFormat.numberFormat().format(number);
    }

    /**
     * Normalizes to a object ID of form <em>project_id</em>_ <em>type_id</em>_
     * <em>number</em>, where number has leading zeros.
     * 
     * @param baseID
     *            is <em>project_id</em>_<em>type_id</em>
     * @param number
     * @return <em>project_id</em>_<em>type_id</em>_<em>number</em>
     */
    public static String formatID(String baseID, int number) {
        String[] idParts = getIDParts(baseID);
        return formatID(idParts[0], idParts[1], number);
    }

    /**
     * Splits the submitted <code>id</code> in its parts.
     * <code>MyCoRe_document_00000001</code> would be transformed in { "MyCoRe",
     * "document", "00000001" }
     * 
     * @param id
     *            either baseID or complete ID
     */
    public static String[] getIDParts(String id) {
        return id.split("_");
    }

    /**
     * This method return the validation value of a MCRObjectId and store the
     * components in this class. The <em>type_id</em> was set to lower case. The
     * MCRObjectID is valid if:
     * <ul>
     * <li>The argument is not null.
     * <li>The syntax of the ID is <em>project_id</em>_<em>type_id</em>_
     * <em>number</em> as <em>String_String_Integer</em>.
     * <li>The ID is not longer as MAX_LENGTH.
     * <li>The ID has only characters, they must not encoded.
     * </ul>
     * 
     * @param id
     *            the MCRObjectID
     * @return the validation value, true if the MCRObjectID is correct,
     *         otherwise return false
     */
    private boolean setID(String id) {
        if (id == null) {
            return false;
        }

        String mcr_id = id.trim();
        if (mcr_id.length() > MAX_LENGTH || mcr_id.length() == 0) {
            return false;
        }

        String[] idParts = getIDParts(mcr_id);

        if (idParts.length != 3) {
            return false;
        }

        mcr_project_id = idParts[0].intern();

        mcr_type_id = idParts[1].toLowerCase().intern();

        if (!CONFIG.getBoolean("MCR.Metadata.Type." + mcr_type_id, false)) {
            LOGGER.warn("Property MCR.Metadata.Type." + mcr_type_id + " is not set. Thus " + id + " cannot be a valid id");
            return false;
        }

        mcr_number = -1;

        try {
            mcr_number = Integer.parseInt(idParts[2]);
        } catch (NumberFormatException e) {
            return false;
        }

        if (mcr_number < 0) {
            return false;
        }
        this.mcr_id = formatID(mcr_project_id, mcr_type_id, mcr_number);

        return true;
    }

    /**
     * This method check the given identifier of following rules:<br/>
     * <ul>
     * <li>the <em>id</em> is not NULL</li>
     * <li>0 &gt; <em>length(id)</em> &lt; 64</li>
     * <li>the <em>id</em> has not three parts</li>
     * <li>the <em>id</em> has not a defined type via  MCR.Metadata.Type.<em>type</em> property</li>
     * <li>The number part of the <em>id</em> is lower 0 or not a number</li>
     * </ul>
     * 
     * @param id the given identifier
     * @return true if all rules are complied
     */
    public static boolean isValid(String id) {
        if (id == null) {
            return false;
        }

        String mcrId = id.trim();
        if (mcrId.length() > MAX_LENGTH || mcrId.length() == 0) {
            return false;
        }

        String[] idParts = getIDParts(mcrId);

        if (idParts.length != 3) {
            return false;
        }

        String type = idParts[1].toLowerCase().intern();

        if (!CONFIG.getBoolean("MCR.Metadata.Type." + type, false)) {
            return false;
        }

        try {
            if (Integer.parseInt(idParts[2]) < 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * This method check this data again the input and retuns the result as
     * boolean.
     * 
     * @param in
     *            the MCRObjectID to check
     * @return true if all parts are equal, else return false.
     */
    public boolean equals(MCRObjectID in) {
        return toString().equals(in.toString());
    }

    /**
     * This method check this data again the input and retuns the result as
     * boolean.
     * 
     * @param in
     *            the MCRObjectID to check
     * @return true if all parts are equal, else return false.
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(Object in) {
        if (!(in instanceof MCRObjectID)) {
            return false;
        }
        return equals((MCRObjectID) in);
    }

    /**
     * @see java.lang.Object#toString()
     * @return {@link #formatID(String, String, int)} with
     *         {@link #getProjectId()}, {@link #getTypeId()},
     *         {@link #getNumberAsInteger()}
     */
    @Override
    public String toString() {
        return mcr_id;
    }

    /**
     * returns toString().hashCode()
     * 
     * @see #toString()
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
