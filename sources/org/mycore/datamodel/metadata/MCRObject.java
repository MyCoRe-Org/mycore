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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import mycore.common.MCRConfiguration;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRException;
import mycore.common.MCRPersistenceException;
import mycore.datamodel.MCRObjectID;
import mycore.datamodel.MCRObjectStructure;
import mycore.datamodel.MCRObjectService;

/**
 * This class implements all methode for handling one metadata object.
 * Methodes of this class can read the XML metadata by using a XML parser,
 * manipulate the data in the abstract persistence data store and return
 * the XML stream to the user application.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRObject
{

/**
 * constant value for the object id length
 **/
public final int MAX_LABEL_LENGTH = 256;

// from configuration
private static String parser_name;
private static String persist_name;
private static String persist_type;

// interface classes
private static MCRParserInterface mcr_parser;
private static MCRObjectPersistenceInterface mcr_persist;

// the DOM document
private Document mcr_document = null;

// the object content
private MCRObjectID mcr_id = null;
private String mcr_label = null;
private MCRObjectStructure mcr_struct = null;
private MCRObjectService mcr_service = null;
private MCRObjectMetadata mcr_metadata = null;

// other
private String NL;

/**
 * This is the constructor of the MCRObject class. It make an
 * instance of the parser class and the metadata class.<br>
 * The constructor reads the following informations from the property file:
 * <ul>
 * <li>MCR.parser_class_name</li>
 * <li>MCR.persistence_class_name</li>
 * <li>MCR.persistence_type</li>
 * </ul>
 *
 * @exception MCRException      general Exception of MyCoRe
 * @exception MCRConfigurationException
 *                              a special exception for configuartion data
 */
public MCRObject() throws MCRException, MCRConfigurationException
  {
  NL = new String((System.getProperties()).getProperty("line.separator"));
  mcr_id = new MCRObjectID();
  mcr_label = new String("");
  try {
  // Parser class
    parser_name = MCRConfiguration.instance()
      .getString("MCR.parser_class_name");
    if (parser_name == null) {
      throw new MCRConfigurationException("MCR.parser_class_name"); }
    mcr_parser = (MCRParserInterface)Class.forName(parser_name).newInstance();
  // Metadata class
    mcr_metadata = new MCRObjectMetadata();
  // Structure class
    mcr_struct = new MCRObjectStructure();
  // Service class
    mcr_service = new MCRObjectService();
  // Persistence class and type
    persist_name = MCRConfiguration.instance()
      .getString("MCR.persistence_class_name");
    if (persist_name == null) {
      throw new MCRConfigurationException("MCR.persistence_class_name"); }
    mcr_persist = (MCRObjectPersistenceInterface)Class.forName(persist_name)
      .newInstance(); 
    persist_type = MCRConfiguration.instance()
      .getString("MCR.persistence_type");
    if (persist_type == null) {
      throw new MCRConfigurationException("MCR.persistence_type"); }
    }
  catch (Exception e) {
     throw new MCRException(e.getMessage()); }
  }

/**
 * This methode return the object id. If this is not set, null was returned.
 *
 * @return the id as MCRObjectID
 **/
public final MCRObjectID getId()
  { return mcr_id; }

/**
 * This methode return the object label. If this is not set, null was returned.
 *
 * @return the lable as a string
 **/
public final String getLabel()
  { return mcr_label; }

/**
 * This methode return the object metadata element selected by tag.
 * If this was not found, null was returned.
 *
 * @return the metadata tag part as a object that extend MCRMetaElement
 **/
public final Object getMetadataElement(String tag)
  { return mcr_metadata.getMetadataElement(tag); }

/**
 * This methode return the instance of the MCRObjectService class.
 * If this was not found, null was returned.
 *
 * @return the instance of the MCRObjectService class
 **/
public final MCRObjectService getService()
  { return mcr_service; }

/**
 * This methode return the instance of the MCRObjectStructure class.
 * If this was not found, null was returned.
 *
 * @return the instance of the MCRObjectStructure class
 **/
public final MCRObjectStructure getStructure()
  { return mcr_struct; }

/**
 * The given DOM was convert into an internal view of metadata. This are 
 * the object ID and the object label, also the blocks structure, flags and 
 * metadata.
 *
 * @exception MCRException      general Exception of MyCoRe
 **/
private final void set() throws MCRException
  {
  if (mcr_document == null) {
    throw new MCRException("The DOM is null or empty."); }
  // get object ID from DOM
  NodeList dom_element_list = mcr_document.getElementsByTagName("mycoreobject");
  Element dom_element = (Element)dom_element_list.item(0);
  mcr_id = new MCRObjectID(dom_element.getAttribute("ID"));
  mcr_label = dom_element.getAttribute("label").trim();
  if (mcr_label.length()>MAX_LABEL_LENGTH) {
   mcr_label = mcr_label.substring(0,MAX_LABEL_LENGTH); }
  // get the structure data of the object
  dom_element_list = mcr_document.getElementsByTagName("structure");
  mcr_struct.setFromDOM(dom_element_list);
  // get the metadata of the object
  dom_element_list = mcr_document.getElementsByTagName("metadata");
  mcr_metadata.setFromDOM(dom_element_list);
  // get the service data of the object
  dom_element_list = mcr_document.getElementsByTagName("service");
  mcr_service.setFromDOM(dom_element_list);
  }

/**
 * This methode read the XML input stream from an URI into a temporary DOM 
 * and check it with XSchema file.
 *
 * @param uri                   an URI
 * @exception MCRException      general Exception of MyCoRe
 **/
public final void setFromURI(String uri) throws MCRException
  {
  try {
    mcr_document = mcr_parser.parseURI(uri);
    set();
    }
  catch (Exception e) {
    System.out.println(e.getMessage());
    throw new MCRException(e.getMessage()); }
  }

/**
 * This methode read the XML input stream from a string into a temporary DOM 
 * and check it with XSchema file.
 *
 * @param xml                   a XML string
 * @exception MCRException      general Exception of MyCoRe
 **/
public final void setFromXML(String xml) throws MCRException
  {
  try {
    mcr_document = mcr_parser.parseXML(xml);
    set();
    }
  catch (Exception e) {
    System.out.println(e.getMessage());
    throw new MCRException(e.getMessage()); }
  }

/**
 * This methode set the object ID.
 *
 * @param id   the object ID
 **/
public final void setId(MCRObjectID id)
  { if (id.isValid()) { mcr_id = id; } }

/**
 * This methode set the object label.
 *
 * @param label   the object label
 **/
public final void setLabel(String label)
  { 
  mcr_label = label.trim();
  if (mcr_label.length()>MAX_LABEL_LENGTH) {
   mcr_label = mcr_label.substring(0,MAX_LABEL_LENGTH); }
  }

/**
 * This methode set the object metadata part named by a tag.
 *
 * @param obj      the class object of a metadata part
 * @param tag      the tag of a metadata part
 * @return true if set was succesful, otherwise false
 **/
public final boolean setMetadataElement(Object obj, String tag)
  { 
  if (obj == null) { return false; }
  if ((tag == null) || ((tag = tag.trim()).length() ==0)) { return false; }
  return mcr_metadata.setMetadataElement(obj, tag);
  }

/**
 * This methode set the object MCRObjectService.
 *
 * @param service   the object MCRObjectService part
 **/
public final void setService(MCRObjectService service)
  { if (service != null) { mcr_service = service; } }

/**
 * This methode set the object MCRObjectStructure.
 *
 * @param structure   the object MCRObjectStructure part
 **/
public final void setStructure(MCRObjectStructure structure)
  { if (structure != null) { mcr_struct = structure; } }

/**
 * This methode create a XML stream for all object data.
 *
 * @return a XML string with the XML data of the object
 **/
public final String createXML()
  {
  StringBuffer sb = new StringBuffer(4096);
  sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(NL);
  sb.append("<mycoreobject ")
    .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
    .append(NL);
  sb.append("xsi:noNamespaceSchemaLocation=\"../schema/")
    .append(mcr_id.getSchema()).append(".xsd\"").append(NL);
  sb.append("ID=\"").append(mcr_id.getId()).append("\" ").append(NL);
  sb.append("label=\"").append(mcr_label).append("\">").append(NL);
  sb.append(mcr_struct.createXML());
  sb.append(mcr_metadata.createXML());
  sb.append(mcr_service.createXML());
  sb.append("</mycoreobject>").append(NL);
  return sb.toString();
  }

/**
 * This methode create a Text Search stream for all object data.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the 
 * <em>MCR.persistence_type</em> configuration.
 *
 * @return a Text Search string with the Text Search data of the object
 **/
public final String createTS()
  {
  if (persist_type.equals("CM7")) {
    StringBuffer sb = new StringBuffer(4096);
    sb.append("<mycoreobject ")
      .append("ID=\"").append(mcr_id.getId()).append("\" ")
      .append("label=\"").append(mcr_label).append("\">").append(NL);
    sb.append(mcr_struct.createTS(persist_type));
    sb.append(mcr_metadata.createTS(persist_type));
    sb.append(mcr_service.createTS(persist_type));
    sb.append("</mycoreobject>").append(NL);
    return sb.toString(); }
  return "";
  }

/**
 * The methode create the object in the data store.
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void createInDatastore() throws MCRPersistenceException
  {
  mcr_service.setCreateDate();
  mcr_service.setSubmitDate("");
  mcr_service.setAcceptDate("");
  mcr_service.setModifyDate("");
  String xml = createXML();
  String ts = createTS();
  mcr_persist.create(mcr_id,mcr_label,mcr_service,xml,ts);
  }

/**
 * The methode delete the object in the data store.
 *
 * @param id   the object ID
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void deleteFromDatastore(String id) throws MCRPersistenceException
  {
  mcr_id = new MCRObjectID(id);
  mcr_persist.delete(mcr_id);
  }

/**
 * The methode receive the object in the data store.
 *
 * @param id   the object ID
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void receiveFromDatastore(String id) throws MCRPersistenceException
  {
  mcr_id = new MCRObjectID(id);
  String xml = mcr_persist.receive(mcr_id);
  setFromXML(xml);
  }

/**
 * The methode update the object in the data store.
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void updateInDatastore() throws MCRPersistenceException
  {
  mcr_service.setModifyDate();
  String xml = createXML();
  String ts = createTS();
  mcr_persist.update(mcr_id,mcr_label,mcr_service,xml,ts); 
  }

/**
 * This metode print the data content from this class.
 **/
public final void debug()
  {
  System.out.println();
  System.out.println("The object content :");
  System.out.println("  ID    = "+mcr_id.getId());
  System.out.println("  label = "+mcr_label);
  System.out.println();
  }
}