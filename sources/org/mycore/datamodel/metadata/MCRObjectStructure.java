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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class implements all methode for handling one document structur data.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRObjectStructure
{
private String NL;

/**
 * This is the constructor of the MCRObjectStructure class.
 */
public MCRObjectStructure()
  {
  NL = new String((System.getProperties()).getProperty("line.separator"));
  }

/**
 * This methode read the XML input stream part from a DOM part for the
 * structure data of the document.
 *
 * @param dom_element_list       a list of relevant DOM elements for
 *                               the metadata
 **/
public final void setFromDOM(NodeList dom_element_list)
  {
  Element structure_element = (Element)dom_element_list.item(0);
  }

/**
 * This methode create a XML stream for all structure data.
 *
 * @return a XML string with the XML data of the structure data part
 **/
public final String createXML()
  {
  StringBuffer sb = new StringBuffer(2048);
  sb.append("<structure>").append(NL);
  sb.append("</structure>").append(NL);
  return sb.toString();
  }

/**
 * This methode create a Text Search stream for all structure data.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param type   the type of the persistece system
 * @return a Text Search string with the data of the metadata part
 **/
public final String createTS(String type)
  {
  if (type.equals("CM7")) {
    StringBuffer sb = new StringBuffer(2048);
    sb.append("<structure>").append(NL);
    sb.append("</structure>").append(NL);
    return sb.toString();
    }
  return "";
  }

/**
 * This metode print all data content from the internal data of the
 * metadata class.
 **/
public final void debug()
  {
  System.out.println("The document structure data content :");
  System.out.println();
  }
}

