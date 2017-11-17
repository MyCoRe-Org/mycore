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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
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
 * @version $Revision$ $Date$
 */
public final class MCRObjectID implements Comparable<MCRObjectID> {
    /**
     * public constant value for the MCRObjectID length
     */
    public static final int MAX_LENGTH = 64;

    // configuration values
    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    // counter for the next IDs per project base ID
    private static HashMap<String, Integer> lastNumber = new HashMap<>();

    // data of the ID
    private String projectId, objectType, combinedId;

    private int numberPart;

    private static final MCRObjectIDFormat idFormat = new MCRObjectIDDefaultFormat();

    private static final Logger LOGGER = LogManager.getLogger(MCRObjectID.class);

    private static HashSet<String> VALID_TYPE_LIST;
    static {
        VALID_TYPE_LIST = new HashSet<>();
        Map<String, String> properties = CONFIG.getPropertiesMap("MCR.Metadata.Type");
        for (Entry<String, String> prop : properties.entrySet()) {
            if (!prop.getValue().equalsIgnoreCase("true")) {
                continue;
            }
            VALID_TYPE_LIST.add(prop.getKey().substring(prop.getKey().lastIndexOf('.') + 1).trim());
        }
    }

    public interface MCRObjectIDFormat {
        int numberDistance();

        NumberFormat numberFormat();
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
        public NumberFormat numberFormat() {
            String numberPattern = MCRConfiguration.instance()
                .getString("MCR.Metadata.ObjectID.NumberPattern", "0000000000").trim();
            NumberFormat format = NumberFormat.getIntegerInstance(Locale.ROOT);
            format.setGroupingUsed(false);
            format.setMinimumIntegerDigits(numberPattern.length());
            return format;
        }

    }

    /**
     * The constructor for MCRObjectID from a given string.
     *
     * @exception MCRException
     *                if the given string is not valid.
     */
    MCRObjectID(String id) throws MCRException {
        if (!setID(id)) {
            throw new MCRException("The ID is not valid: " + id
                + " , it should match the pattern String_String_Integer");
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

    public static synchronized MCRObjectID getNextFreeId(String base, String type) {
        return getNextFreeId(base + "_" + type);
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

        lastNumber.put(base_id, next);
        String[] idParts = getIDParts(base_id);
        return getInstance(formatID(idParts[0], idParts[1], next));
    }

    /**
     * Returns the last ID number used or reserved for the given object base
     * type. This may return the value 0 when there is no ID last used or in the
     * store.
     */
    private static int getLastIDNumber(String base_id) {
        int lastIDKnown = lastNumber.getOrDefault(base_id, 0);

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
        return MCRObjectIDPool.getMCRObjectID(Objects.requireNonNull(id, "'id' must not be null."));
    }

    /**
     * This method get the string with <em>project_id</em>. If the ID is not
     * valid, an empty string was returned.
     *
     * @return the string of the project id
     */
    public final String getProjectId() {
        return projectId;
    }

    /**
     * This method gets the string with <em>type_id</em>. If the ID is not
     * valid, an empty string will be returned.
     *
     * @return the string of the type id
     */
    public final String getTypeId() {
        return objectType;
    }

    /**
     * This method gets the string with <em>number</em>. If the ID is not valid,
     * an empty string will be returned.
     *
     * @return the string of the number
     */
    public final String getNumberAsString() {
        return idFormat.numberFormat().format(numberPart);
    }

    /**
     * This method gets the integer with <em>number</em>. If the ID is not
     * valid, -1 will be returned.
     *
     * @return the number as integer
     */
    public final int getNumberAsInteger() {
        return numberPart;
    }

    /**
     * This method gets the basic string with <em>project_id</em>_
     * <em>type_id</em>. If the Id is not valid, an empty string will be
     * returned.
     *
     * @return the string of the schema name
     */
    public String getBase() {
        return projectId + "_" + objectType;
    }

    /**
     * Normalizes to a object ID of form <em>project_id</em>_ <em>type_id</em>_
     * <em>number</em>, where number has leading zeros.
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
        return projectID + '_' + type.toLowerCase(Locale.ROOT) + '_' + idFormat.numberFormat().format(number);
    }

    /**
     * Normalizes to a object ID of form <em>project_id</em>_ <em>type_id</em>_
     * <em>number</em>, where number has leading zeros.
     *
     * @param baseID
     *            is <em>project_id</em>_<em>type_id</em>
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
        if (!isValid(id)) {
            return false;
        }
        String[] idParts = getIDParts(id.trim());
        projectId = idParts[0].intern();
        objectType = idParts[1].toLowerCase(Locale.ROOT).intern();
        numberPart = Integer.parseInt(idParts[2]);
        this.combinedId = formatID(projectId, objectType, numberPart);
        return true;
    }

    /**
     * This method check this data again the input and retuns the result as
     * boolean.
     *
     * @param in
     *            the MCRObjectID to check
     * @return true if all parts are equal, else return false
     */
    public boolean equals(MCRObjectID in) {
        return this == in || (in != null && toString().equals(in.toString()));
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
        if (in instanceof MCRObjectID) {
            return equals((MCRObjectID) in);
        }
        return false;
    }

    @Override
    public int compareTo(MCRObjectID o) {
        return MCRUtils.compareParts(this, o,
            MCRObjectID::getProjectId,
            MCRObjectID::getTypeId,
            MCRObjectID::getNumberAsInteger);
    }

    /**
     * @see java.lang.Object#toString()
     * @return {@link #formatID(String, String, int)} with
     *         {@link #getProjectId()}, {@link #getTypeId()},
     *         {@link #getNumberAsInteger()}
     */
    @Override
    public String toString() {
        return combinedId;
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

    /**
     * Returns a list of available mycore object types.
     */
    public static List<String> listTypes() {
        return new ArrayList<>(VALID_TYPE_LIST);
    }

    /**
     * Check whether the type passed is a valid type in the current mycore environment.
     * That being said property <code>MCR.Metadata.Type.&#60;type&#62;</code> must be set to <code>true</code> in mycore.properties.
     *
     * @param type the type to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidType(String type) {
        return VALID_TYPE_LIST.contains(type);
    }

    /**
     * Checks if the given id is a valid mycore id in the form of {project}_{object_type}_{number}.
     * 
     * @param id the id to check
     * @return true if the id is valid, false otherwise
     */
    public static boolean isValid(String id) {
        if (id == null) {
            return false;
        }
        String mcr_id = id.trim();
        if (mcr_id.length() > MAX_LENGTH) {
            return false;
        }
        String[] idParts = getIDParts(mcr_id);
        if (idParts.length != 3) {
            return false;
        }
        String objectType = idParts[1].toLowerCase(Locale.ROOT).intern();
        if (!CONFIG.getBoolean("MCR.Metadata.Type." + objectType, false)) {
            LOGGER.warn("Property MCR.Metadata.Type.{} is not set. Thus {} cannot be a valid id", objectType, id);
            return false;
        }
        try {
            Integer numberPart = Integer.parseInt(idParts[2]);
            if (numberPart < 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

}
