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

import java.text.*;
import java.util.*;
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;
import mycore.datamodel.MCRMetaLink;
import mycore.datamodel.MCRTypedContent;

/**
 * This class implements all methode for handling one derivate data.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRObjectDerivate
{
// common data
private String NL;

// derivate data
private ArrayList linkmetas = null;
private ArrayList externals = null;

/**
 * This is the constructor of the MCRObjectDerivate class. All data
 * are set to null.
 */
public MCRObjectDerivate()
  {
  NL = new String((System.getProperties()).getProperty("line.separator"));
  linkmetas = new ArrayList();
  externals = new ArrayList();
  }

/**
 * This methode read the XML input stream part from a DOM part for the
 * structure data of the document.
 *
 * @param derivate_element       a list of relevant DOM elements for
 *                               the derivate
 **/
public final void setFromDOM(org.jdom.Element derivate_element)
  {
  // Link to Metadata part
  org.jdom.Element linkmetas_element = derivate_element.getChild("linkmetas");
  if (linkmetas_element!=null) {
    List linkmeta_element_list = linkmetas_element.getChildren();
    int linkmeta_len = linkmeta_element_list.size();
    for (int i=0;i<linkmeta_len;i++) {  
      org.jdom.Element linkmeta_element = (org.jdom.Element)
        linkmeta_element_list.get(i);
      MCRMetaLink link = new MCRMetaLink();
      link.setDataPart("linkmeta");
      link.setFromDOM(linkmeta_element);
      linkmetas.add(link); 
      }
    }
  // External part
  org.jdom.Element externals_element = derivate_element.getChild("externals");
  if (externals_element!=null) {
    List external_element_list = externals_element.getChildren();
    int external_len = external_element_list.size();
    for (int i=0;i<external_len;i++) {  
      org.jdom.Element external_element = (org.jdom.Element)
        external_element_list.get(i);
      MCRMetaLink link = new MCRMetaLink();
      link.setDataPart("external");
      link.setFromDOM(external_element);
      externals.add(link); 
      }
    }
  }

/**
 * This method return the size of the linkmeta array.
 **/
public final int getSizeLinkMeta()
  { return linkmetas.size(); }

/**
 * This method get a single link from the linkmeta list as a MCRMetaLink.
 *
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 * @return a metadata link as MCRMetaLink
 **/
public final MCRMetaLink getLinkMeta(int index) 
  throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>linkmetas.size())) {
    throw new IndexOutOfBoundsException("Index error in getLinkMeta."); }
  return (MCRMetaLink)linkmetas.get(index);
  }

/**
 * This method return the size of the external array.
 **/
public final int getSizeExternal()
  { return externals.size(); }

/**
 * This method get a single link from the external list as a MCRMetaLink.
 *
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 * @return a external link as MCRMetaLink
 **/
public final MCRMetaLink getExternal(int index) 
  throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>externals.size())) {
    throw new IndexOutOfBoundsException("Index error in getExternal."); }
  return (MCRMetaLink)externals.get(index);
  }

/**
 * This methode create a XML stream for all derivate data.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML data of the structure data part
 **/
public final org.jdom.Element createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  org.jdom.Element elm = new org.jdom.Element("derivate");
  if (linkmetas.size()!=0) {
    org.jdom.Element elmm = new org.jdom.Element("linkmetas");
    elmm.setAttribute("class","MCRMetaLink");
    elmm.setAttribute("heritable","false");
    elmm.setAttribute("parasearch","true");
    elmm.setAttribute("textsearch","false");
    for (int i=0;i<linkmetas.size();i++) {
      elmm.addContent(((MCRMetaLink)linkmetas.get(i)).createXML()); }
    elm.addContent(elmm); 
    }
  if (externals.size()!=0) {
    org.jdom.Element elmm = new org.jdom.Element("externals");
    elmm.setAttribute("class","MCRMetaLink");
    elmm.setAttribute("heritable","false");
    elmm.setAttribute("parasearch","false");
    elmm.setAttribute("textsearch","false");
    for (int i=0;i<externals.size();i++) {
      elmm.addContent(((MCRMetaLink)externals.get(i)).createXML()); }
    elm.addContent(elmm); 
    }
  return elm;
  }

/**
 * This methode create a typed content list for all derivate data.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the metadata part
 **/
public final MCRTypedContent createTypedContent() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(tc.TYPE_MASTERTAG,"derivate");
  tc.addTagElement(tc.TYPE_TAG,"linkmetas");
  for (int i=0;i<linkmetas.size();i++) {
    tc.addMCRTypedContent(((MCRMetaLink)linkmetas.get(i))
      .createTypedContent(true));
    }
  return tc;
  }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if<br>
 * <ul>
 * <li>the number of linkmeta is 0</li>
 * <li>the XLink type of linkmeta is not "arc"</li>
 * </ul>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if (linkmetas.size() == 0) { return false; }
  for (int i=0;i<linkmetas.size();i++) {
    if (!((MCRMetaLink)linkmetas.get(i)).getXLinkType().equals("arc")) {
      return false; }
    }
  return true;
  }

/**
 * This metode print all data content from the internal data of this class.
 **/
public final void debug()
  {
  System.out.println("MCRObjectDerivate debug start");
  for (int i=0;i<linkmetas.size();i++) {
    ((MCRMetaLink)linkmetas.get(i)).debug(); }
  for (int i=0;i<externals.size();i++) {
    ((MCRMetaLink)externals.get(i)).debug(); }
  System.out.println("MCRObjectDerivate debug end"+NL);
  }

}

