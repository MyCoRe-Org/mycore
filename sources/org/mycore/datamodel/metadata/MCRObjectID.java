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

package mycore.datamodel;

import mycore.common.MCRException;
import mycore.common.MCRConfiguration;
import mycore.common.MCRUsageException;
import mycore.datamodel.MCRObjectPersistenceInterface;

/**
 * This class holds all information an methode to handle the MyCoRe Object ID.
 * The MyCoRe Object ID is a special ID with three parts, they are a project
 * identifier, a type identifier and a string with a number.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public final class MCRObjectID 
{

/**
 * constant value for the object id length
 **/
public static final int MAX_LENGTH = 64;
public static final String [] NO_NUMBER_TYPE = { "classification","category" };

// constant definitions
private static final String NL =
  new String((System.getProperties()).getProperty("line.separator"));
private static MCRConfiguration conf = null;

private String mcr_project_id = null;
private String mcr_type_id = null;
private String mcr_number = null;
private boolean mcr_valid_id;

/**
 * Static mthode to load the configuration.
 **/
static
  {
  conf = MCRConfiguration.instance();
  }

/**
 * The constructor for an empty MCRObjectId.
 **/
public MCRObjectID ()
  {
  mcr_project_id = null;
  mcr_type_id = null;
  mcr_number = null;
  mcr_valid_id = false;
  }

/**
 * The constructor for MCRObjectId with a given string.
 *
 * @exception MCRException if the given string is not valid.
 **/
public MCRObjectID (String id) throws MCRException
  {
  mcr_valid_id = false;
  boolean is = isValid(id);
  if (!is) { throw new MCRException("The ID is not valid"); }
  mcr_valid_id = true;
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
public void setNextId(String base_id) throws MCRUsageException
  {
  mcr_valid_id = false;
  StringBuffer sb = new StringBuffer(MAX_LENGTH);
  sb.append(base_id).append("_1");
  boolean is = isValid(sb.toString());
  if (!is) { throw new MCRException("The ID is not valid"); }
  MCRObjectPersistenceInterface mcr_persist;
  try {
    String persist_type = conf.getString("MCR.persistence_type");
    String proppers = "MCR.persistence_"+persist_type.toLowerCase()+
      "_class_name";
    String persist_name = conf.getString(proppers);
    mcr_persist = (MCRObjectPersistenceInterface)Class.forName(persist_name)
      .newInstance(); 
    mcr_number = mcr_persist.getNextFreeId(mcr_project_id,mcr_type_id);
    }
  catch (Exception e) {
     throw new MCRUsageException(e.getMessage(),e); }
  mcr_valid_id = true;
  }

/**
 * This methode get the string with <em>project_id</em>. 
 * If the Id is not valid, an empty string was returned.
 *
 * @return the string of the project id
 **/
public final String getProjectId()
  {
  if (!mcr_valid_id) { return ""; }
  return mcr_project_id;
  }

/**
 * This methode get the string with <em>type_id</em>. 
 * If the Id is not valid, an empty string was returned.
 *
 * @return the string of the type id
 **/
public final String getTypeId()
  {
  if (!mcr_valid_id) { return ""; }
  return mcr_type_id;
  }

/**
 * This methode get the string with <em>number</em>. 
 * If the Id is not valid, an empty string was returned.
 *
 * @return the string of the number
 **/
public final String getNumber()
  {
  if (!mcr_valid_id) { return ""; }
  return mcr_number;
  }

/**
 * This methode get the integer with <em>number</em>. 
 * If the Id is not valid or in the NO_NUMBER_TYPE list, a -1 was returned.
 *
 * @return the number as integer
 **/
public final int getNumberAsInteger()
  {
  if (!mcr_valid_id) { return -1; }
  int i = -1;
  for (int j=0;j<NO_NUMBER_TYPE.length;j++) {
    if (NO_NUMBER_TYPE[j].equals(mcr_type_id)) { i = j; break; } }
  if (i==-1) { return i; }
  return (new Integer(mcr_number)).intValue();
  }

/**
 * This methode get the basic string with 
 * <em>project_id</em>_<em>type_id</em>. If the Id is not valid,
 * an empty string was returned.
 *
 * @return the string of the schema name
 **/
public final String getBase()
  {
  if (!mcr_valid_id) { return ""; }
  return mcr_project_id+"_"+mcr_type_id;
  }

/**
 * This methode get the ID string with 
 * <em>project_id</em>_<em>type_id</em>_<em>number</em>. 
 * If the Id is not valid, an empty string was returned.
 *
 * @return the string of the schema name
 **/
public final String getId()
  {
  if (!mcr_valid_id) { return ""; }
  return mcr_project_id+"_"+mcr_type_id+"_"+mcr_number;
  }

/**
 * This methode return the validation value of a MCRObjectId.
 * The MCRObjectId is valid if:
 * <ul>
 * <li> The syntax of the ID is 
 * <em>project_id</em>_<em>type_id</em>_<em>number</em> as
 * <em>String_String_Integer</em>.
 * (If the second string is in the NO_NUMBER_TYPE list, a string at the
 * third position is correct.)
 * <li> The ID is not longer as MAX_LENGTH.
 * </ul>
 *
 * @return the validation value, true if the MCRObjectId is correct,
 * otherwise return false
 **/
public final boolean isValid()
  { return mcr_valid_id; }

/**
 * This methode return the validation value of a MCRObjectId.
 * The MCRObjectId is valid if:
 * <ul>
 * <li> The argument is not null.
 * <li> The syntax of the ID is 
 * <em>project_id</em>_<em>type_id</em>_<em>number</em> as
 * <em>String_String_Integer</em>.
 * (If the second string is in the NO_NUMBER_TYPE list, a string at the
 * third position is correct.)
 * <li> The ID is not longer as MAX_LENGTH.
 * </ul>
 *
 * @param id   the MCRObjectId
 * @return the validation value, true if the MCRObjectId is correct,
 * otherwise return false
 **/
public final boolean isValid(String id)
  { 
  mcr_valid_id = false;
  if ((id == null) || ((id = id.trim()).length() ==0)) { return false; }
  if (id.length()>MAX_LENGTH) { return false; }
  String mcr_id = id;
  int len = mcr_id.length();
  int i = mcr_id.indexOf("_");
  if (i==-1) { return false; }
  mcr_project_id = mcr_id.substring(0,i);
  int j = mcr_id.indexOf("_",i+1);
  if (j==-1) { return false; }
  mcr_type_id = mcr_id.substring(i+1,j);
  if (!conf.getBoolean("MCR.type_"+mcr_type_id.toLowerCase(),false)) { 
    return false; }
  mcr_number = mcr_id.substring(j+1,len);
  i = -1;
  for (j=0;j<NO_NUMBER_TYPE.length;j++) {
    if (NO_NUMBER_TYPE[j].equals(mcr_type_id)) { i = j; break; } }
  if (i==-1) {
    try { j = (new Integer(mcr_number)).intValue(); }
    catch (NumberFormatException e) { return false; }
    }
  mcr_valid_id = true;
  return mcr_valid_id; 
  }

/**
 * This method print all data content from the MCRObjectId class.
 **/
public final void debug()
  {
  System.out.println("MCRObjectId debug start:");
  System.out.println("<ID>"+getId()+"</ID>");
  System.out.println("MCRObjectId debug end"+NL);
  }

} 

