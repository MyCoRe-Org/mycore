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

package mycore.cm7;

import java.util.*;
import mycore.common.MCRException;
import mycore.datamodel.MCRQueryInterface;

/**
 * This is the tranformer implementation for CM 7 from  Miless Query language
 * to the CM Text Search Engine.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRCM7QueryMiless implements MCRQueryInterface
{
// common data
protected static String NL =
  new String((System.getProperties()).getProperty("line.separator"));

/**
 * The constructor.
 **/
public MCRCM7QueryMiless()
  {}

/**
 * This method parse the  MilessQuery string and return a vector of 
 * MCRObjectID's.
 *
 * @param query	                the Miless Query string
 * @param maxresult             the maximum of results
 * @param type                  the MCRObject type
 * @exception MCRException      general Exception of MyCoRe
 * @return			list of MCRObjectID's as a vector
 **/
public final Vector getResultList(String query, String type, int maxresult) 
  throws MCRException
  {
  System.out.println("================================");
  System.out.println("MCRCM7QueryMiless : "+query);
  System.out.println("================================");
  return null;
  }

/**
 * The methode returns the search string for a XML text field for
 * the IBM Content Manager 7 persistence system.
 *
 * @param subtag             the tagname of an element from the list in a tag
 * @param value              the text value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringText(String subtag, String value)
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return ""; }
  if ((value == null) || ((value = value.trim()).length() ==0)) {
    return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("XXX").append(subtag.toUpperCase()).append("XXX ");
  sb.append(value.toUpperCase()).append(NL);
  return sb.toString();
  }

/**
 * The methode returns the search string for a XML attribute field for
 * the IBM Content Manager 7 persistence system.
 *
 * @param subtag             the tagname of an element from the list in a tag
 * @param attrib             the attribute name of this attribute
 * @param value              the text value of this attribute
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringAttrib(String subtag, String attrib, 
  String value)
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return ""; }
  if ((attrib == null) || ((attrib = attrib.trim()).length() ==0)) {
    return ""; }
  if ((value == null) || ((value = value.trim()).length() ==0)) {
    return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("XXX").append(subtag.toUpperCase()).append("XXX").
     append(attrib.toUpperCase()).append("XXX ");
  sb.append(value.toUpperCase()).append(NL);
  return sb.toString();
  }

}

