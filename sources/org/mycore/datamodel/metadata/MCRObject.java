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
import mycore.xml.MCRXMLHelper;

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
/**
 * the header of all XML Files
 **/
public final static String XML_HEADER = 
  "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>";

// from configuration
private String parser_name;
private String persist_name;
private String persist_type;

// interface classes
private MCRObjectPersistenceInterface mcr_persist;

// the DOM document
private Document mcr_document = null;

// the object content
private MCRObjectID mcr_id = null;
private String mcr_label = null;
private String mcr_schema = null;
private String mcr_schema_path = null;
private MCRObjectStructure mcr_struct = null;
private MCRObjectService mcr_service = null;
private MCRObjectMetadata mcr_metadata = null;

// other
private String NL;
private String SLASH;

/**
 * This is the constructor of the MCRObject class. It make an
 * instance of the parser class and the metadata class.<br>
 * The constructor reads the following informations from the property file:
 * <ul>
 * <li>MCR.parser_class_name</li>
 * </ul>
 *
 * @exception MCRException      general Exception of MyCoRe
 * @exception MCRConfigurationException
 *                              a special exception for configuartion data
 */
public MCRObject() throws MCRException, MCRConfigurationException
  {
  NL = System.getProperty("line.separator");
  SLASH = System.getProperty("file.separator");
  mcr_id = new MCRObjectID();
  mcr_label = new String("");
  mcr_schema = new String("");
  mcr_schema_path = new String("");
  persist_type = new String("");
  mcr_persist = null;
  try {
  // Path of XML schema
    mcr_schema_path = MCRConfiguration.instance()
      .getString("MCR.parser_schema_path");
  // Metadata class
    mcr_metadata = new MCRObjectMetadata();
  // Structure class
    mcr_struct = new MCRObjectStructure();
  // Service class
    mcr_service = new MCRObjectService();
    }
  catch (Exception e) {
     throw new MCRException(e.getMessage(),e); }
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
public final MCRMetaElement getMetadataElement(String tag)
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
  mcr_schema = dom_element.getAttribute("xsi:noNamespaceSchemaLocation").trim();
  int i=0;
  int j=0;
  while (j!=-1) {
    j = mcr_schema.indexOf(SLASH,i+1); if (j!=-1) { i = j; } }
  mcr_schema = mcr_schema.substring(i+1,mcr_schema.length());
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
 * This methode set the persistence depended of the ObjectId type part.
 * It search the <em>MCR.persistence_type_...</em> information of
 * the property file. The it will load the coresponding persistence class.
 *
 * @exception MCRException was throw if the ObjectId is null or empty or 
 * the class was not found
 **/
private final void setPersistence() throws MCRException
  {
  if (!mcr_id.isValid()) { 
    throw new MCRException("The ObjectId is not valid."); }
  String proptype = "MCR.persistence_type_"+mcr_id.getTypeId().toLowerCase();
  try {
    persist_type = MCRConfiguration.instance().getString(proptype);
    String proppers = "MCR.persistence_"+persist_type.toLowerCase()+
      "_class_name";
    persist_name = MCRConfiguration.instance().getString(proppers);
    mcr_persist = (MCRObjectPersistenceInterface)Class.forName(persist_name)
      .newInstance(); 
    }
  catch (Exception e) {
     throw new MCRException(e.getMessage(),e); }
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
    mcr_document = MCRXMLHelper.parseURI(uri);
    set();
    }
  catch (Exception e) {
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
    mcr_document = MCRXMLHelper.parseXML(xml);
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
public final boolean setMetadataElement(MCRMetaElement obj, String tag)
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
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML data of the object
 **/
public final String createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  StringBuffer sb = new StringBuffer(4096);
  sb.append(XML_HEADER).append(NL);
  sb.append("<mycoreobject ").append(NL);
  sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
    .append(NL);
  sb.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
    .append(NL);
  sb.append("xsi:noNamespaceSchemaLocation=\"").append(mcr_schema_path)
    .append(SLASH).append(mcr_schema).append("\"").append(NL);
  sb.append("ID=\"").append(mcr_id.getId()).append("\" ").append(NL);
  sb.append("label=\"").append(mcr_label).append("\">").append(NL);
  sb.append(mcr_struct.createXML());
  sb.append(mcr_metadata.createXML());
  sb.append(mcr_service.createXML());
  sb.append("</mycoreobject>").append(NL);
  return sb.toString();
  }

/**
 * This methode create a typed content list for all MCRObject data.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public final MCRTypedContent createTypedContent() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(tc.TYPE_MASTERTAG,"mycoreobject");
  tc.addStringElement(tc.TYPE_ATTRIBUTE,"ID",mcr_id.getId(),true,true);
  tc.addStringElement(tc.TYPE_ATTRIBUTE,"label",mcr_label,true,true);
  tc.addMCRTypedContent(mcr_struct.createTypedContent());
  tc.addMCRTypedContent(mcr_metadata.createTypedContent());
  tc.addMCRTypedContent(mcr_service.createTypedContent());
  return tc;
  }

/**
 * The methode create the object in the data store.
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void createInDatastore() throws MCRPersistenceException
  {
  if (mcr_persist==null) { setPersistence(); }
  mcr_service.setDate("createdate");
  mcr_service.setDate("modifydate");
  String xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  mcr_persist.create(mcr_tc,xml);
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
  if (mcr_persist==null) { setPersistence(); }
  mcr_persist.delete(mcr_id);
  }

/**
 * The methode receive the object for the given MCRObjectID and stored
 * it in this MCRObject.
 *
 * @param id   the object ID
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void receiveFromDatastore(String id) 
  throws MCRPersistenceException
  {
  mcr_id = new MCRObjectID(id);
  if (mcr_persist==null) { setPersistence(); }
  String xml = mcr_persist.receive(mcr_id);
  setFromXML(xml);
  }

/**
 * The methode receive the object for the given MCRObjectID and returned
 * it as XML stream.
 *
 * @param id   the object ID
 * @return the XML stream of the object as string
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final String receiveXMLFromDatastore(String id) 
  throws MCRPersistenceException
  {
  mcr_id = new MCRObjectID(id);
  if (mcr_persist==null) { setPersistence(); }
  return mcr_persist.receive(mcr_id);
  }

/**
 * The methode update the object in the data store.
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void updateInDatastore() throws MCRPersistenceException
  {
  if (mcr_persist==null) { setPersistence(); }
  mcr_service.setDate("createdate",mcr_persist.receiveCreateDate(mcr_id));
  mcr_service.setDate("modifydate");
  String xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  mcr_persist.update(mcr_tc,xml);
  }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if
 * <ul>
 * <li> the mcr_id value is valid
 * <li> the label value is not null or empty
 * </ul>
 * otherwise the method return <em>false</em>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if (!mcr_id.isValid()) { return false; }
  if ((mcr_label == null) || ((mcr_label = mcr_label.trim()).length() ==0)) {
    return false; }
  if ((mcr_schema == null) || ((mcr_schema = mcr_schema.trim()).length() ==0)) {
    return false; }
  return true;
  }

/**
 * This metode print the data content from this class.
 **/
public final void debug()
  {
  System.out.println();
  System.out.println("The object content :");
  System.out.println("  ID     = "+mcr_id.getId());
  System.out.println("  label  = "+mcr_label);
  System.out.println("  schema = "+mcr_schema);
  System.out.println();
  }
}
