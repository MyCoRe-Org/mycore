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

package org.mycore.datamodel.metadata;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;

/**
 * This class holds all information an methode to handle the MyCoRe Object ID.
 * The MyCoRe Object ID is a special ID with three parts, they are a project
 * identifier, a type identifier and a string with a number.
 *
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 **/
public final class MCRObjectID {

	protected static MCRConfiguration CONFIG;
	private static final Logger LOGGER = Logger.getLogger(MCRObjectID.class);
	/**
	 * constant value for the object id length
	 **/
	public static final int MAX_LENGTH = 64;

	// constant definitions
	private static final String NL =
		new String((System.getProperties()).getProperty("line.separator"));
	private static MCRConfiguration conf = null;

	private String mcr_project_id = null;
	private String mcr_type_id = null;
	private String mcr_id = null;
	private int mcr_number = -1;
	private boolean mcr_valid_id = false;

	private static String number_pattern = null;
	private static DecimalFormat number_format = null;

	/**
	 * Static mthode to load the configuration.
	 **/
	static {
		conf = MCRConfiguration.instance();
		number_pattern =
			conf.getString("MCR.metadata_objectid_number_pattern", "0000000000");
		number_format = new DecimalFormat(number_pattern);
	}

	/**
	 * The constructor for an empty MCRObjectId.
	 **/
	public MCRObjectID() {
		init();
	}

	/**
	 * The constructor for MCRObjectId with a given string.
	 *
	 * @exception MCRException if the given string is not valid.
	 **/
	public MCRObjectID(String id) throws MCRException {
		init();
		if (!setID(id)) {
			throw new MCRException("The ID is not valid: " + id);
		}
		mcr_valid_id = true;
	}
	
	private void init(){
		if (CONFIG==null)
			CONFIG=MCRConfiguration.instance();
	}

	/**
	 * The methode set the MCRObjectId with a given base ID string.
	 * A base ID is <em>project_id</em>_<em>type_id</em>. 
	 * The number was computet from this methode.
	 * It is the next free number of an item in the database for the given
	 * project ID and type ID.
	 *
	 * @param base_id         the basic ID 
	 * @exception MCRUsageException if the given string is not valid.
	 **/
	public void setNextFreeId(String base_id) throws MCRUsageException {
		mcr_valid_id = false;
		StringBuffer mcrid = new StringBuffer(base_id).append('_').append(1);
		MCRObjectID test = new MCRObjectID(mcrid.toString());
		mcrid.deleteCharAt(mcrid.length() - 1);
		try {
			MCRXMLTableManager xmltable = MCRXMLTableManager.instance();
			mcrid.append(
				xmltable.getNextFreeIdInt(
					test.mcr_type_id,
					test.mcr_project_id,
					test.mcr_type_id));
			test=null;
			if (!setID(mcrid.toString()))
				throw new MCRException("Error setting to new ID:" + mcrid);
		} catch (Exception e) {
			throw new MCRUsageException(e.getMessage(), e);
		}
		mcr_valid_id = true;
	}

	/**
	 * This method set a new type in a existing MCRObjectID.
	 *
	 * @param type the new type
	 **/
	public final boolean setType(String type) {
          if (type == null) return false;
	  String test = type.toLowerCase().intern();
          if (!conf.getBoolean("MCR.type_" + test, false)) { return false; }
          mcr_type_id = test;
          return true;
          }

	/**
	 * This method set a new number in a existing MCRObjectID.
	 *
	 * @param number the new number
	 **/
	public final boolean setNumber(int num) {
		if (!mcr_valid_id) {
			return false;
		}
		if (num < 0) {
			return false;
		}
		mcr_number = num;
		mcr_id=null;
		return true;
	}

	/**
	 * This method get the string with <em>project_id</em>. 
	 * If the Id is not valid, an empty string was returned.
	 *
	 * @return the string of the project id
	 **/
	public final String getProjectId() {
		if (!mcr_valid_id) {
			return "";
		}
		return mcr_project_id;
	}

	/**
	 * This methode get the string with <em>type_id</em>. 
	 * If the Id is not valid, an empty string was returned.
	 *
	 * @return the string of the type id
	 **/
	public final String getTypeId() {
		if (!mcr_valid_id) {
			return "";
		}
		return mcr_type_id;
	}

	/**
	 * This methode get the string with <em>number</em>. 
	 * If the Id is not valid, an empty string was returned.
	 *
	 * @return the string of the number
	 **/
	public final String getNumberAsString() {
		if (!mcr_valid_id) {
			return "";
		}
		return number_format.format((long) mcr_number);
	}

	/**
	 * This methode get the integer with <em>number</em>. 
	 * If the Id is not valid, a -1 was returned.
	 *
	 * @return the number as integer
	 **/
	public final int getNumberAsInteger() {
		if (!mcr_valid_id) {
			return -1;
		}
		return mcr_number;
	}

	/**
	 * This methode get the basic string with 
	 * <em>project_id</em>_<em>type_id</em>. If the Id is not valid,
	 * an empty string was returned.
	 *
	 * @return the string of the schema name
	 **/
	public String getBase() {
		if (!mcr_valid_id) {
			return "";
		}
		return mcr_project_id + "_" + mcr_type_id;
	}

	/**
	 * This methode get the ID string with 
	 * <em>project_id</em>_<em>type_id</em>_<em>number</em>. 
	 * If the Id is not valid, an empty string was returned.
	 *
	 * @return the string of the schema name
	 **/
	public String getId() {
		if (!mcr_valid_id) {
			return "";
		}
		if (mcr_id == null) {
			mcr_id =
				new StringBuffer(MAX_LENGTH)
					.append(mcr_project_id)
					.append('_')
					.append(mcr_type_id)
					.append('_')
					.append(number_format.format((long) mcr_number))
					.toString();
		}
		return mcr_id;
	}

	/**
	 * This methode return the validation value of a MCRObjectId.
	 * The MCRObjectId is valid if:
	 * <ul>
	 * <li> The syntax of the ID is 
	 * <em>project_id</em>_<em>type_id</em>_<em>number</em> as
	 * <em>String_String_Integer</em>.
	 * <li> The ID is not longer as MAX_LENGTH.
	 * </ul>
	 *
	 * @return the validation value, true if the MCRObjectId is correct,
	 * otherwise return false
	 **/
	public boolean isValid() {
		return mcr_valid_id;
	}

	/**
	 * This method return the validation value of a MCRObjectId and store the
	 * components in this class. The <em>type_id</em> was set to lower case.
	 * The MCRObjectId is valid if:
	 * <ul>
	 * <li> The argument is not null.
	 * <li> The syntax of the ID is 
	 * <em>project_id</em>_<em>type_id</em>_<em>number</em> as
	 * <em>String_String_Integer</em>.
	 * <li> The ID is not longer as MAX_LENGTH.
	 * >li> The ID has only characters, they must not encoded.
	 * </ul>
	 *
	 * @param id   the MCRObjectId
	 * @return the validation value, true if the MCRObjectId is correct,
	 * otherwise return false
	 **/
	private final boolean setID(String id) {
		mcr_valid_id = false;
		if ((id == null) || ((id = id.trim()).length() == 0)) {
			return false;
		}
		if (id.length() > MAX_LENGTH) {
			return false;
		}
		String mcr_id;
		try {
			mcr_id = URLEncoder.encode(id,CONFIG.getString("MCR.request_charencoding", "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			LOGGER.error("MCR.request_charencoding property does not contain a valid encoding:",e1);
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
		if (!conf.getBoolean("MCR.type_" + mcr_type_id.toLowerCase(), false)) {
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
		this.mcr_id=null;
		mcr_valid_id = true;
		return mcr_valid_id;
	}

	/**
	 * This method checks the value of a MCRObjectId.
	 * The MCRObjectId is valid if:
	 * <ul>
	 * <li> The argument is not null.
	 * <li> The syntax of the ID is 
	 * <em>project_id</em>_<em>type_id</em>_<em>number</em> as
	 * <em>String_String_Integer</em>.
	 * <li> The ID is not longer as MAX_LENGTH.
	 * >li> The ID has only characters, they must not encoded.
	 * </ul>
	 *
	 * @param id   the MCRObjectId
	 * @throws MCRException if ID is not valid
	 **/
	public static void isValidOrDie(String id) {
		MCRObjectID obj=new MCRObjectID(id);
		obj=null;
	}

	/**
	 * This method check this data again the  input and retuns the result as
	 * boolean.
	 * @param in the MCRObjectID to check
	 * @return true if all parts are equal, else return false.
	 **/
	public boolean equals(MCRObjectID in) {
		if ((mcr_project_id == in.mcr_project_id)
			&& (mcr_type_id == in.mcr_type_id)
			&& (mcr_number == in.mcr_number))
			return true;
		else
			return false;
	}

	/**
	 * This method check this data again the  input and retuns the result as
	 * boolean.
	 * @param in the MCRObjectID to check
	 * @return true if all parts are equal, else return false.
	 * @see java.lang.Object#equals(Object)
	 **/
	public boolean equals(Object in) {
		if (!(in instanceof MCRObjectID))
			return false;
		else {
			return equals((MCRObjectID) in);
		}
	}

	/** @see #getId()
	 * @see java.lang.Object#toString()
	 **/
	public String toString() {
		return getId();
	}

	/** returns getId().hashCode()
	 * @see #getId()
	 * @see java.lang.Object#hashCode()
	 **/
	public int hashCode() {
		return getId().hashCode();
	}
}
