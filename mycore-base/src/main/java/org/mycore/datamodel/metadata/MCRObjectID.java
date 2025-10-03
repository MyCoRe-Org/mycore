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

package org.mycore.datamodel.metadata;

import java.io.Serial;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * This class holds all information and methods to handle the MyCoRe Object ID.
 * The MyCoRe Object ID is a special ID to identify each metadata object with
 * three parts, they are the project identifier, the type identifier and a
 * string with a number. The syntax of the ID is "<em>projectID</em>_
 * <em>typeID</em>_ <em>number</em>" as "<em>String_String_Integer</em>".
 *
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 */
@JsonClassDescription("MyCoRe ObjectID in form {project}_{type}_{int32}, "
    + "where project is a namespace and type defines the datamodel")
@JsonFormat(shape = JsonFormat.Shape.STRING)
public final class MCRObjectID implements Comparable<MCRObjectID>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * public constant value for the MCRObjectID length
     */
    public static final int MAX_LENGTH = 64;

    private static NumberFormat numberFormat;

    private static final Logger LOGGER = LogManager.getLogger();

    /** ID pattern with named capturing groups */
    private static final Pattern ID_PATTERN = Pattern
        .compile("^(?<projectId>[a-zA-Z][a-zA-Z0-9]*)_(?<objectType>[a-zA-Z0-9]+)_(?<numberPart>[0-9]+)$");

    private static final Set<String> VALID_TYPE_LIST;

    private static final Comparator<MCRObjectID> COMPARATOR_FOR_MCR_OBJECT_ID = Comparator
        .comparing(MCRObjectID::getProjectId)
        .thenComparing(MCRObjectID::getTypeId)
        .thenComparingInt(MCRObjectID::getNumberAsInteger);

    static {
        final String confPrefix = "MCR.Metadata.Type.";
        VALID_TYPE_LIST = MCRConfiguration2.getSubPropertiesMap(confPrefix)
            .entrySet()
            .stream()
            .filter(p -> Boolean.parseBoolean(p.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(HashSet::new));
        numberFormat = initNumberFormat();
    }

    // parts of the ID
    private final String projectId;

    private final String objectType;

    private final int numberPart;

    /**
     * The constructor for MCRObjectID from a given string.
     *
     * @exception MCRException
     *                if the given string is not valid.
     */
    MCRObjectID(String id) throws MCRException {
        if (!isValid(id)) {
            throw new MCRException("The ID is not valid: " + id
                + " , it should match the pattern String_String_Integer");
        }
        String[] idParts = getIDParts(id.trim());
        projectId = idParts[0].intern();
        objectType = idParts[1].toLowerCase(Locale.ROOT).intern();
        numberPart = Integer.parseInt(idParts[2]);
    }

    /**
     * This method instantiates this class with a given identifier in MyCoRe schema.
     *
     * @param id
     *          the MCRObjectID
     * @return an MCRObjectID class instance
     * @exception MCRException if the given identifier is not valid
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    @SuppressWarnings({ "PMD.MCR.Singleton.PrivateConstructor", "PMD.MCR.Singleton.NonPrivateConstructors" })
    public static MCRObjectID getInstance(String id) {
        return MCRObjectIDPool.getMCRObjectID(Objects.requireNonNull(id, "'id' must not be null."));
    }

    /**
     * Normalizes to an object ID of form <em>project_id</em>_ <em>type_id</em>_
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
        synchronized (numberFormat) {
            return projectID + '_' + type.toLowerCase(Locale.ROOT) + '_' + numberFormat.format(number);
        }
    }

    /**
     * Normalizes to an object ID of form <em>project_id</em>_ <em>type_id</em>_
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
        if (id.length() > MAX_LENGTH) {
            return false;
        }
        Matcher m = ID_PATTERN.matcher(id);
        if (m.matches()) {
            String objectType = m.group("objectType").toLowerCase(Locale.ROOT).intern();
            if (!MCRConfiguration2.getBoolean("MCR.Metadata.Type." + objectType).orElse(false)) {
                LOGGER.warn("Property MCR.Metadata.Type.{} is not set to 'true'. Thus {} cannot be a valid id.",
                    objectType, id);
                return false;
            }
            try {
                int numberPart = Integer.parseInt(m.group("numberPart"));
                if (numberPart < 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * This method get the string with <em>project_id</em>. If the ID is not
     * valid, an empty string was returned.
     *
     * @return the string of the project id
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * This method gets the string with <em>type_id</em>.
     *
     * @return the string of the type id
     */
    public String getTypeId() {
        return objectType;
    }

    /**
     * This method gets the string with <em>number</em>.
     *
     * @return the string of the number
     */
    public String getNumberAsString() {
        synchronized (numberFormat) {
            return numberFormat.format(numberPart);
        }
    }

    /**
     * This method gets the integer with <em>number</em>.
     *
     * @return the number as integer
     */
    public int getNumberAsInteger() {
        return numberPart;
    }

    /**
     * This method gets the basic string with <em>project_id</em>_
     * <em>type_id</em>.
     *
     * @return the string of the schema name
     */
    public String getBase() {
        return projectId + "_" + objectType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MCRObjectID other)) {
            return false;
        }
        return numberPart == other.numberPart
            && projectId.equals(other.projectId)
            && objectType.equals(other.objectType);
    }

    @Override
    public int compareTo(MCRObjectID o) {
        return COMPARATOR_FOR_MCR_OBJECT_ID.compare(this, o);
    }

    @Override
    @JsonValue
    public String toString() {
        return formatID(projectId, objectType, numberPart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, objectType, numberPart);
    }

    private static NumberFormat initNumberFormat() {
        String numberPattern = MCRConfiguration2.getStringOrThrow("MCR.Metadata.ObjectID.NumberPattern");
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.ROOT);
        format.setGroupingUsed(false);
        format.setMinimumIntegerDigits(numberPattern.length());
        return format;
    }

}
