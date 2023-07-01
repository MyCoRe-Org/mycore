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

package org.mycore.datamodel.common;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class contains a set of utility functions for MCRObjectID.
 * 
 * They are useful for formatting and validation.
 * 
 * @author Robert Stephan
 */
public class MCRObjectIDHelper{
    
    private static final Logger LOGGER = LogManager.getLogger(MCRObjectID.class);
    
    private static NumberFormat NUMBER_FORMAT = initNumberFormat();
    
    private static Set<String> VALID_TYPES = initValidTypes();
    
    /** maximal character length of the whole id */
    public static final int MAX_LENGTH = 64;
    
    
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
        return projectID + '_' + type.toLowerCase(Locale.ROOT) + '_' + NUMBER_FORMAT.format(number);
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
    * This method gets the string with <em>numberPart</em> 
    * of the given MCRObjectID.
    * 
    * replaces MCRObjectID.getNumberAsString()
    *
    * @return the string of the number
    */
   public static String formatNumberPart(MCRObjectID id) {
       return NUMBER_FORMAT.format(id.getNumberAsInteger());
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
        return new ArrayList<>(VALID_TYPES);
    }

    /**
     * Check whether the type passed is a valid type in the current mycore environment.
     * That being said property <code>MCR.Metadata.Type.&#60;type&#62;</code> must be set to <code>true</code> in mycore.properties.
     *
     * @param type the type to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidType(String type) {
        return VALID_TYPES.contains(type);
    }

    /**
     * Checks if the given id is a valid mycore id in the form of {project}_{object_type}_{number}.
     *
     * @param id the id to check
     * @return true if the id is valid, false otherwise
     */
    public static boolean isValidID(String id) {
        if (id == null) {
            return false;
        }
        String mcrId = id.trim();
        if (mcrId.length() > MAX_LENGTH) {
            return false;
        }
        String[] idParts = getIDParts(mcrId);
        if (idParts.length != 3) {
            return false;
        }
        String objectType = idParts[1].toLowerCase(Locale.ROOT).intern();
        if (!MCRConfiguration2.getBoolean("MCR.Metadata.Type." + objectType).orElse(false)) {
            LOGGER.warn("Property MCR.Metadata.Type.{} is not set. Thus {} cannot be a valid id", objectType, id);
            return false;
        }
        try {
            int numberPart = Integer.parseInt(idParts[2]);
            if (numberPart < 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    
    private static NumberFormat initNumberFormat() {
        String numberPattern = MCRConfiguration2.getString("MCR.Metadata.ObjectID.NumberPattern")
            .orElse("0000000000").trim();
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.ROOT);
        format.setGroupingUsed(false);
        format.setMinimumIntegerDigits(numberPattern.length());
        return format;
    }
    
    private static Set<String> initValidTypes() {
        final String confPrefix = "MCR.Metadata.Type.";
        return (HashSet<String>) MCRConfiguration2.getPropertiesMap()
            .entrySet()
            .stream()
            .filter(p -> p.getKey().startsWith(confPrefix))
            .filter(p -> Boolean.parseBoolean(p.getValue()))
            .map(prop -> prop.getKey().substring(confPrefix.length()))
            .collect(Collectors.toCollection(HashSet::new));
    }
    
}