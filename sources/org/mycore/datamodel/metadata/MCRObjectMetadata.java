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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import mycore.common.MCRConfiguration;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRException;
import mycore.datamodel.MCRMetaInterface;

/**
 * This class implements all methode for handling one object metadata part.
 * This class uses only metadata type classes of the general 
 * datamodel code of MyCoRe.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRObjectMetadata
{
private String NL;
private String default_lang = null;
private String meta_package_name = "mycore.datamodel.";
private ArrayList meta_list = null;

/**
 * This is the constructor of the MCRObjectMetadata class. It set the default
 * language for all metadata to the value from the configuration properties.
 *
 * @exception MCRConfigurationException
 *                              a special exception for configuartion data
 */
public MCRObjectMetadata() throws MCRConfigurationException
  {
  NL = new String((System.getProperties()).getProperty("line.separator"));
  default_lang = MCRConfiguration.instance()
    .getString("MCR.metadata_default_lang");
  if (default_lang == null) {
    throw new MCRConfigurationException("MCR.metadata_default_lang"); }
  meta_list = new ArrayList();
  }

/**
 * This methode return the object metadata element selected by tag.
 * If this was not found, null was returned.
 *
 * @return the metadata tag part as a object that extend MCRMetaElement
 **/
public final Object getMetadataElement(String tag)
  {
  if ((tag == null) || ((tag = tag.trim()).length() ==0)) { return null; }
  Object meta_obj = new Object();
  int len = meta_list.size();
  for (int i = 0; i < len; i++) {
    meta_obj = meta_list.get(i);
    if (((MCRMetaInterface)meta_obj).getTag().equals(tag)) { 
      return meta_obj; }
    }
  return  null;
  }

/**
 * This methode set the given object metadata element.
 **/
public final Object setMetadataElement()
  { return null; }

/**
 * This methode read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param dom_element_list       a list of relevant DOM elements for
 *                               the metadata
 **/
public final void setFromDOM(NodeList dom_element_list)
  {
  String meta_class_name;
  Element metadata_root = (Element)dom_element_list.item(0);
  default_lang = metadata_root.getAttribute("xml:lang");
  NodeList metadata_elements = metadata_root.getChildNodes();
  int len = metadata_elements.getLength();
  for (int i=0;i<len;i++) {
    Node metadata_item = metadata_elements.item(i);
    if (metadata_item.getNodeType() != Node.ELEMENT_NODE) { continue; }
    System.out.println("Nodename = "+metadata_item.getNodeName()+" "+
      metadata_item.getNodeType());
    meta_class_name = meta_package_name +
      ((Element)metadata_item).getAttribute("type");
    System.out.println("Classname = "+meta_class_name);
    Object meta_obj = new Object();
    try {
      meta_obj = Class.forName(meta_class_name).newInstance();
      ((MCRMetaInterface)meta_obj).setDefaultLang(default_lang);
      ((MCRMetaInterface)meta_obj).setFromDOM(metadata_item);
      }
    catch (ClassNotFoundException e) {
       System.out.println("ClassNotFoundException : "+e.getMessage()); }
    catch (IllegalAccessException e) {
       System.out.println("IllegalAccessException : "+e.getMessage()); }
    catch (InstantiationException e) {
       System.out.println("InstantiationException : "+e.getMessage()); }
    meta_list.add(meta_obj);
    }
  }

/**
 * This methode create a XML stream for all metadata.
 *
 * @return a XML string with the XML data of the metadata part
 **/
public final String createXML()
  {
  StringBuffer sb = new StringBuffer(2048);
  sb.append("<metadata xml:lang=\"").append(default_lang).append("\">")
    .append(NL);
  Object meta_obj;
  int len = meta_list.size();
  for (int i = 0; i < len; i++) {
    meta_obj = meta_list.get(i);
    sb.append(((MCRMetaInterface)meta_obj).createXML());
    }
  sb.append("</metadata>").append(NL);
  return sb.toString();
  }

/**
 * This methode create a Text Search stream for all metadata.
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
    sb.append("<metadata xml:lang=\"").append(default_lang).append("\">")
      .append(NL);
    Object meta_obj;
    int len = meta_list.size();
    for (int i = 0; i < len; i++) {
      meta_obj = meta_list.get(i);
      sb.append(((MCRMetaInterface)meta_obj).createTS(type));
      }
    sb.append("</metadata>").append(NL);
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
  System.out.println("The document matadata content :");
  System.out.println("default language               = "+default_lang);
  System.out.println();
  Object meta_obj;
  int len = meta_list.size();
  for (int i = 0; i < len; i++) {
    meta_obj = meta_list.get(i);
    ((MCRMetaInterface)meta_obj).debug();
    }
  }
}

