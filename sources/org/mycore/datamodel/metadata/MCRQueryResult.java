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

import java.util.*;
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;
import mycore.common.MCRConfigurationException;
import mycore.datamodel.MCRObject;
import mycore.datamodel.MCRQueryInterface;

/**
 * This class is the result list of a XQuery question to the persistence
 * system. With the getElement methode you can get a MCRObjectID from
 * this list. The method use, depending of the persitence type property,
 * a interface to a query transformer form XQuery to the target system
 * query language.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRQueryResult
{
private int vec_max_length;
private Vector mcr_result = null;
private MCRQueryInterface mcr_transform = null;

/**
 * This constructor read the properties for the MCRObject type and call
 * the coresponding class for a query to the persistence layer. If
 * it was succesful, the vector of MCRObject's is filled with answers.
 *
 * @param type 	                the MCRObject type
 * @param query	                the Query string
 * @exception MCRException      general Exception of MyCoRe
 * @exception MCRConfigurationException      
 *                              an Exception of MyCoRe Configuration
 **/
public MCRQueryResult(String type, String query) 
  throws MCRException, MCRConfigurationException
  {
  vec_max_length = MCRConfiguration.instance()
    .getInt("MCR.query_max_results",10);
  mcr_result = new Vector(vec_max_length);
  String persist_type = "";
  String transform_name = "";
  String proptype = "MCR.persistence_type_"+type.toLowerCase();
  try {
    persist_type = MCRConfiguration.instance().getString(proptype);
    String proppers = "MCR.persistence_"+persist_type.toLowerCase()+
      "_query_name";
    transform_name = MCRConfiguration.instance().getString(proppers);
    mcr_transform = (MCRQueryInterface)Class.forName(transform_name)
      .newInstance(); 
    mcr_result = mcr_transform.getResultList(query,type,vec_max_length);
    }
  catch (ClassNotFoundException e) {
     System.out.println("ClassNotFoundException : "+e.getMessage()); }
  catch (IllegalAccessException e) {
     System.out.println("IllegalAccessException : "+e.getMessage()); }
  catch (InstantiationException e) {
     System.out.println("InstantiationException : "+e.getMessage()); }
  catch (Exception e) {
     throw new MCRException(e.getMessage(),e); }
  }

/**
 * This methode get one MCRObjectId as string from the result list.
 *
 * @param index              the index of the Element
 * return a MCRObjectId of the index or null if no object for this index
 * was found.
 **/
public final String getElement(int index)
  {
  if ((index < 0) || (index > mcr_result.size())) { return null; }
  return (String) mcr_result.elementAt(index);
  }

/**
 * This methode print the MCRObjectId as string from the result list.
 **/
public final void debug()
  {
  System.out.println("Result list:");
  for (int i=0;i< mcr_result.size();i++) {
    System.out.println("  "+(String)mcr_result.elementAt(i)); }
  System.out.println();
  }
}

