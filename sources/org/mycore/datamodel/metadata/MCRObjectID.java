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
public final int MAX_LENGTH = 64;

private String mcr_id = null;
private String mcr_project_id = null;
private String mcr_type_id = null;
private String mcr_number = null;
private boolean mcr_valid_id;

/**
 * The constructor for an empty MCRObjectId.
 **/
public MCRObjectID ()
  {
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
  if ((id == null) || ((id = id.trim()).length() ==0)) {
    throw new MCRException("The ID \""+id+"\" is not valid"); }
  if (id.length()>MAX_LENGTH) {
    throw new MCRException("The ID \""+id+"\" is not valid"); }
  mcr_id = id;
  int len = mcr_id.length();
  int i = mcr_id.indexOf("_");
  if (i==-1) {
    throw new MCRException("The ID \""+id+"\" is not valid"); }
  mcr_project_id = mcr_id.substring(0,i);
  int j = mcr_id.indexOf("_",i+1);
  if (j==-1) {
    throw new MCRException("The ID \""+id+"\" is not valid"); }
  mcr_type_id = mcr_id.substring(i+1,j);
  mcr_number = mcr_id.substring(j+1,len);
  mcr_valid_id = true;
  }

/**
 * The constructor for MCRObjectId with a given project string and
 * a given type string. The number was computet from this methode.
 * The number is definite and is a merge of time stamp base 36 and 
 * a counter.
 *
 * @exception MCRException if the given string is not valid.
 **/
public MCRObjectID (String project, String type) throws MCRException
  {
  mcr_valid_id = false;
  if ((project == null) || ((project = project.trim()).length() ==0)) {
    throw new MCRException("The project ID \""+project+"\" is not valid"); }
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    throw new MCRException("The type ID \""+type+"\" is not valid"); }
  mcr_project_id = project;
  mcr_type_id = type;
  mcr_number = "1";
  mcr_id = project+"_"+type+"_"+mcr_number;
  if (mcr_id.length()>MAX_LENGTH-20) {
    throw new MCRException("The ID parts are too long"); }
  mcr_valid_id = true;
  }

/**
 * This methode get the string with "<em>project_id</em>". 
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
 * This methode get the string with "<em>type_id</em>". 
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
 * This methode get the string with "<em> object number</em>". 
 * If the Id is not valid, an empty string was returned.
 *
 * @return the string of the object number
 **/
public final String getObjectNumber()
  {
  if (!mcr_valid_id) { return ""; }
  return mcr_number;
  }

/**
 * This methode get the schema string with 
 * "<em>project_id</em>_<em>type_id</em>". If the Id is not valid,
 * an empty string was returned.
 *
 * @return the string of the schema name
 **/
public final String getSchema()
  {
  if (!mcr_valid_id) { return ""; }
  return mcr_project_id+"_"+mcr_type_id;
  }

/**
 * This methode get the ID string with 
 * "<em>project_id</em>_<em>type_id</em>_<em>number</em>". 
 * If the Id is not valid, an empty string was returned.
 *
 * @return the string of the schema name
 **/
public final String getId()
  {
  if (!mcr_valid_id) { return ""; }
  return mcr_id;
  }

/**
 * This methode retun the valid boolean value.
 *
 * @return the valid value
 **/
public final boolean isValid()
  { return mcr_valid_id; }

} 

