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

package org.mycore.datamodel.metadata;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * This class holds all informations and methods to handle the MyCoRe Object ID.
 * The MyCoRe Object ID is a special ID to identify each metadata object with
 * three parts, they are the project identifier, the type identifier and a
 * string with a number.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public final class MCRObjectID {
    /**
     * public constant value for the MCRObjectID length
     */
    public static final int MAX_LENGTH = 64;

    // configuration values
    protected static MCRConfiguration CONFIG;

    private static final Logger LOGGER = Logger.getLogger(MCRObjectID.class);

    // counter for the next IDs per project base ID
    private static HashMap lastnumber = new HashMap();

    // data of the ID
    private String mcr_project_id = null;

    private String mcr_type_id = null;

    private String mcr_id = null;

    private int mcr_number = -1;

    private boolean mcr_valid_id = false;

    private static int number_distance = 1;

    private static String number_pattern = null;

    private static DecimalFormat number_format = null;

    /**
     * Static method to load the configuration.
     */
    static {
        CONFIG = MCRConfiguration.instance();
        number_distance = CONFIG.getInt("MCR.metadata_objectid_number_distance", 1);
        number_pattern = CONFIG.getString("MCR.metadata_objectid_number_pattern", "0000000000");
        number_format = new DecimalFormat(number_pattern);
    }

    /**
     * The constructor for an empty MCRObjectId.
     */
    public MCRObjectID() {
    }

    /**
     * The constructor for MCRObjectID from a given string.
     * 
     * @exception MCRException
     *                if the given string is not valid.
     */
    public MCRObjectID(String id) throws MCRException {
        mcr_valid_id = false;

        if (!setID(id)) {
            throw new MCRException("The ID is not valid: " + id);
        }

        mcr_valid_id = true;
    }

    /**
     * The method set the MCRObjectID from a given base ID string. A base ID is
     * <em>project_id</em>_<em>type_id</em>. The number was computed from
     * this methode. It is the next free number of an item in the database for
     * the given project ID and type ID.
     * 
     * @param base_id
     *            the basic ID
     * @exception MCRException
     *                if the given string is not valid or can't connect to the
     *                MCRXMLTableManager.
     */
    public void setNextFreeId(String base_id) throws MCRException {
        manageFreeId(base_id, 1);
    }

    /**
     * The method decrement the temporary stored last number of a project ID
     * with 1.
     */
    public void decrementOneFreeId(String base_id) throws MCRException {
        manageFreeId(base_id, -1);
    }

    /**
     * The method manage the MCRObjectID for a given base ID string. A base ID
     * is <em>project_id</em>_<em>type_id</em>. The number was computed
     * from this methode. If the step integer is higher than 0 it is the next
     * free number of an item in the database for the given project ID and type
     * ID. Otherwise the last temporary stored number for the base ID string
     * will be decrement with the integer value.
     * 
     * @param base_id
     *            the basic ID
     * @exception MCRException
     *                if the given string is not valid or can't connect to the
     *                MCRXMLTableManager.
     */
    private synchronized void manageFreeId(String base_id, int dec) throws MCRException {
        // check the base_id
        mcr_valid_id = false;
        StringBuffer mcrid = new StringBuffer(base_id).append('_').append(1);
        if (!setID(mcrid.toString())) {
            throw new MCRException("Error in project base string for the new ID:" + mcrid);
        }
        mcrid.deleteCharAt(mcrid.length() - 1);

        if (dec > 0) {
            // get the next number
            try {
                MCRXMLTableManager xmltable = MCRXMLTableManager.instance();
                int i = xmltable.getNextFreeIdInt(mcr_project_id, mcr_type_id);
                Integer j = (Integer) lastnumber.get(base_id);
                int mylastnumber = -1;

                if (j != null) {
                    mylastnumber = j.intValue();
                }

                if (mylastnumber < i) {
                    mcrid.append(i);
                    mylastnumber = i;
                } else {
                    mylastnumber += 1;

                    while ((mylastnumber % number_distance) != 0) {
                        mylastnumber += 1;
                    }

                    mcrid.append(mylastnumber);
                }

                lastnumber.put(base_id, new Integer(mylastnumber));
            } catch (Exception e) {
                throw new MCRException(e.getMessage(), e);
            }

            if (!setID(mcrid.toString())) {
                throw new MCRException("Error setting to new ID:" + mcrid);
            }

            mcr_valid_id = true;
            return;
        }
        // decrement with dec
        else {
            try {
                int j = ((Integer) lastnumber.get(base_id)).intValue();
                if (j >= (-dec)) {
                    j += dec;
                }
                lastnumber.put(base_id, new Integer(j));
            } catch (Exception e) {
                LOGGER.warn("Cant'd decrement last ID for project id " + base_id + " cause it's not initialized.");
            }

        }
    }

    /**
     * The method decrement the temporary stored last number of a project ID
     * with 1.
     */
    /**
     * This method set a new type in a existing MCRObjectID.
     * 
     * @param type
     *            the new type
     */
    public final boolean setType(String type) {
        if (type == null) {
            return false;
        }

        String test = type.toLowerCase().intern();

        if (!CONFIG.getBoolean("MCR.type_" + test, false)) {
            return false;
        }

        mcr_type_id = test;

        return true;
    }

    /**
     * This method set a new number in a existing MCRObjectID.
     * 
     * @param num
     *            the new number
     */
    public final boolean setNumber(int num) {
        if (!mcr_valid_id) {
            return false;
        }

        if (num < 0) {
            return false;
        }

        mcr_number = num;
        mcr_id = null;

        return true;
    }

    /**
     * This method get the string with <em>project_id</em>. If the ID is not
     * valid, an empty string was returned.
     * 
     * @return the string of the project id
     */
    public final String getProjectId() {
        if (!mcr_valid_id) {
            return "";
        }

        return mcr_project_id;
    }

    /**
     * This method get the string with <em>type_id</em>. If the ID is not
     * valid, an empty string was returned.
     * 
     * @return the string of the type id
     */
    public final String getTypeId() {
        if (!mcr_valid_id) {
            return "";
        }

        return mcr_type_id;
    }

    /**
     * This method get the string with <em>number</em>. If the ID is not
     * valid, an empty string was returned.
     * 
     * @return the string of the number
     */
    public final String getNumberAsString() {
        if (!mcr_valid_id) {
            return "";
        }

        return number_format.format(mcr_number);
    }

    /**
     * This method get the integer with <em>number</em>. If the ID is not
     * valid, a -1 was returned.
     * 
     * @return the number as integer
     */
    public final int getNumberAsInteger() {
        if (!mcr_valid_id) {
            return -1;
        }

        return mcr_number;
    }

    /**
     * This method get the basic string with <em>project_id</em>_
     * <em>type_id</em>. If the Id is not valid, an empty string was
     * returned.
     * 
     * @return the string of the schema name
     */
    public String getBase() {
        if (!mcr_valid_id) {
            return "";
        }

        return mcr_project_id + "_" + mcr_type_id;
    }

    /**
     * This method get the ID string with <em>project_id</em>_
     * <em>type_id</em>_<em>number</em>. If the ID is not valid, an empty
     * string was returned.
     * 
     * @return the string of the schema name
     */
    public String getId() {
        if (!mcr_valid_id) {
            return "";
        }

        if (mcr_id == null) {
            mcr_id = new StringBuffer(MAX_LENGTH).append(mcr_project_id).append('_').append(mcr_type_id).append('_').append(number_format.format(mcr_number)).toString();
        }

        return mcr_id;
    }

    /**
     * This method return the validation value of a MCRObjectId. The MCRObjectID
     * is valid if:
     * <ul>
     * <li>The syntax of the ID is <em>project_id</em>_<em>type_id</em>_
     * <em>number</em> as <em>String_String_Integer</em>.
     * <li>The ID is not longer as MAX_LENGTH.
     * </ul>
     * 
     * @return the validation value, true if the MCRObjectId is correct,
     *         otherwise return false
     */
    public boolean isValid() {
        return mcr_valid_id;
    }

    /**
     * This method return the validation value of a MCRObjectId and store the
     * components in this class. The <em>type_id</em> was set to lower case.
     * The MCRObjectID is valid if:
     * <ul>
     * <li>The argument is not null.
     * <li>The syntax of the ID is <em>project_id</em>_<em>type_id</em>_
     * <em>number</em> as <em>String_String_Integer</em>.
     * <li>The ID is not longer as MAX_LENGTH.
     * <li>The ID has only characters, they must not encoded.
     * </ul>
     * 
     * @param id
     *            the MCRObjectId
     * @return the validation value, true if the MCRObjectId is correct,
     *         otherwise return false
     */
    public final boolean setID(String id) {
        mcr_valid_id = false;

        if ((id == null) || ((id = id.trim()).length() == 0)) {
            return false;
        }

        if (id.length() > MAX_LENGTH) {
            return false;
        }

        String mcr_id;

        try {
            mcr_id = URLEncoder.encode(id, CONFIG.getString("MCR.request_charencoding", "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            LOGGER.error("MCR.request_charencoding property does not contain a valid encoding:", e1);

            return false;
        }

        if (!mcr_id.equals(id)) {
            return false;
        }

        int len = mcr_id.length();
        int i = mcr_id.indexOf("_");

        if (i == -1) {
            return false;
        }

        mcr_project_id = mcr_id.substring(0, i).intern();

        int j = mcr_id.indexOf("_", i + 1);

        if (j == -1) {
            return false;
        }

        mcr_type_id = mcr_id.substring(i + 1, j).toLowerCase().intern();

        if (!CONFIG.getBoolean("MCR.type_" + mcr_type_id.toLowerCase(), false)) {
            return false;
        }

        mcr_number = -1;

        try {
            mcr_number = Integer.parseInt(mcr_id.substring(j + 1, len));
        } catch (NumberFormatException e) {
            return false;
        }

        if (mcr_number < 0) {
            return false;
        }

        this.mcr_id = null;
        mcr_valid_id = true;

        return mcr_valid_id;
    }

    /**
     * This method checks the value of a MCRObjectId. The MCRObjectId is valid
     * if:
     * <ul>
     * <li>The argument is not null.
     * <li>The syntax of the ID is <em>project_id</em>_<em>type_id</em>_
     * <em>number</em> as <em>String_String_Integer</em>.
     * <li>The ID is not longer as MAX_LENGTH. >li> The ID has only characters,
     * they must not encoded.
     * </ul>
     * 
     * @param id
     *            the MCRObjectId
     * @throws MCRException
     *             if ID is not valid
     */
    public static void isValidOrDie(String id) {
        new MCRObjectID(id);
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
        return (mcr_project_id.equals(in.mcr_project_id) && (mcr_type_id.equals(in.mcr_type_id)) && (mcr_number == in.mcr_number));
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
    public boolean equals(Object in) {
        if (!(in instanceof MCRObjectID)) {
            return false;
        }
        return equals((MCRObjectID) in);
    }

    /**
     * @see #getId()
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getId();
    }

    /**
     * returns getId().hashCode()
     * 
     * @see #getId()
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getId().hashCode();
    }
}
