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

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * This class implements all methode for handling one metadata object.
 * Methodes of this class can read the XML metadata by using a XML parser,
 * manipulate the data in the abstract persistence data store and return
 * the XML stream to the user application.
 * Additionally, this class provides the public user interface for the
 * linking of MCRObjects against other MCRObjects with metadata inheritance.
 *
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @version $Revision$ $Date$
 **/
final public class MCRObject extends MCRBase
{

// the object content
private MCRObjectStructure mcr_struct = null;
private MCRObjectMetadata mcr_metadata = null;

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
  super();
  // Metadata class
  mcr_metadata = new MCRObjectMetadata();
  // Structure class
  mcr_struct = new MCRObjectStructure(logger);
  }

/**
 * This methode return the object metadata element selected by tag.
 * If this was not found, null was returned.
 *
 * @return the metadata tag part as a object that extend MCRMetaElement
 **/
public final MCRMetaElement getMetadataElement(String tag)
  { return mcr_metadata.getMetadataElement(tag); }

/**
 * This method returns the instance of the MCRObjectMetadata class.
 * If there was no MCRObjectMetadata found, null will be returned.
 *
 * @return the instance of the MCRObjectMetadata class
 **/
public final MCRObjectMetadata getMetadata()
  { return mcr_metadata; }

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
  if (jdom_document == null) {
    throw new MCRException("The JDOM document is null or empty."); }
  // get object ID from DOM
  org.jdom.Element jdom_element_root = jdom_document.getRootElement();
  mcr_id = new MCRObjectID((String)jdom_element_root.getAttribute("ID")
    .getValue());
  mcr_label = (String)jdom_element_root.getAttribute("label").getValue().trim();
  if (mcr_label.length()>MAX_LABEL_LENGTH) {
    mcr_label = mcr_label.substring(0,MAX_LABEL_LENGTH); }
  mcr_schema = (String)jdom_element_root
    .getAttribute("noNamespaceSchemaLocation",
     org.jdom.Namespace.getNamespace("xsi",MCRDefaults.XSI_URL)).getValue()
     .trim();
  logger.debug("MCRObject set schemafile: " + mcr_schema );
  // get the structure data of the object
  org.jdom.Element jdom_element = jdom_element_root.getChild("structure");
  mcr_struct.setFromDOM(jdom_element);
  // get the metadata of the object
  jdom_element = jdom_element_root.getChild("metadata");
  mcr_metadata.setFromDOM(jdom_element);
  // get the service data of the object
  jdom_element = jdom_element_root.getChild("service");
  mcr_service.setFromDOM(jdom_element);
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
    jdom_document = MCRXMLHelper.parseURI(uri);
    }
  catch (Exception e) {
    throw new MCRException(e.getMessage(),e); }
  set();
  }

/**
 * This methode read the XML input stream from a byte array into JDOM 
 * and check it with XSchema file.
 *
 * @param xml                   a XML string
 * @exception MCRException      general Exception of MyCoRe
 **/
public final void setFromXML(byte [] xml, boolean valid) throws MCRException
  {
  try {
    jdom_document = MCRXMLHelper.parseXML(xml,false);
    }
  catch (Exception e) {
    throw new MCRException(e.getMessage()); }
  set();
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
 * @return a JDOM Document with the XML data of the object as byte array
 **/
public final org.jdom.Document createXML() throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("The content is not valid."); }
  org.jdom.Element elm = new org.jdom.Element("mycoreobject");
  org.jdom.Document doc = new org.jdom.Document(elm);
  elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
    MCRDefaults.XSI_URL));
  elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
  elm.setAttribute("noNamespaceSchemaLocation",mcr_schema,
    org.jdom.Namespace.getNamespace("xsi",MCRDefaults.XSI_URL));
  elm.setAttribute("ID",mcr_id.getId());
  elm.setAttribute("label",mcr_label);
  elm.addContent(mcr_struct.createXML());
  elm.addContent(mcr_metadata.createXML());
  elm.addContent(mcr_service.createXML());
  return doc;
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
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(MCRTypedContent.TYPE_MASTERTAG,"mycoreobject");
  tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE,"ID",mcr_id.getId());
  tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE,"label",mcr_label);
  tc.addMCRTypedContent(mcr_struct.createTypedContent());
  tc.addMCRTypedContent(mcr_metadata.createTypedContent());
  tc.addMCRTypedContent(mcr_service.createTypedContent());
  return tc;
  }

/**
 * This methode create a String for all text searchable data in this instance.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a String with the text values from the metadata object
 **/
public final String createTextSearch()
  throws MCRException
  { return mcr_metadata.createTextSearch(); }

/**
 * The methode create a new datastore based of given data. It create
 * a new data table for storing MCRObjects with the same MCRObjectID type.
 **/
public final void createDataBase(String mcr_type, org.jdom.Document confdoc)
  {
  setId(new MCRObjectID("Template_"+mcr_type+"_1"));
  mcr_persist.createDataBase(mcr_type, confdoc);
  }

/**
 * The methode create the object in the data store.
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void createInDatastore() throws MCRPersistenceException
  {
  // exist the object?
  if (existInDatastore(mcr_id.getId())) {
    throw new MCRPersistenceException("The object "+mcr_id.getId()+
      " allready exists, nothing done."); }
  // create this object in datastore
  mcr_service.setDate("createdate");
  mcr_service.setDate("modifydate");
  // prepare this object with parent metadata
  MCRObjectID parent_id = mcr_struct.getParentID();
  if (parent_id != null) {
    logger.debug("Parent ID = "+parent_id.getId());
    try {
      MCRObject parent = new MCRObject();
      parent.receiveFromDatastore(parent_id);
      mcr_metadata.appendMetadata(parent.getMetadata()
        .getHeritableMetadata());
      }
    catch (Exception e) {
      logger.error(MCRException.getStackTraceAsString(e));
      logger.error("Error while merging metadata in this object.");
      return;
      }
    }
  // build this object
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.create(mcr_tc,xml,mcr_ts);
  mcr_xmltable.create(mcr_id.getTypeId(),mcr_id,xml);
  deleteLinksFromTable();
  addLinksToTable(mcr_tc);
  // add the MCRObjectID to the child list in the parent object
  if (parent_id != null) {
    try {
      MCRObject parent = new MCRObject();
      parent.receiveFromDatastore(parent_id);
      parent.getStructure().addChild(mcr_id,mcr_struct.getParent()
        .getXLinkLabel(),mcr_label);
      parent.updateThisInDatastore();
      }
    catch (Exception e) {
      logger.debug(MCRException.getStackTraceAsString(e));
      logger.error("Error while store child ID in parent object.");
      deleteFromDatastore();
      logger.error("Child object was removed.");
      return;
      }
    }
  }

/**
 * The methode add a derivate MCRMetaLinkID to the structure part and
 * update the object with the ID in the data store.
 *
 * @param id the object ID
 * @param derivate a link to a derivate as MCRMetaLinkID
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void addDerivateInDatastore(String id, MCRMetaLinkID link)
  throws MCRPersistenceException
  {
  mcr_id = new MCRObjectID(id);
  byte [] xmlarray = mcr_xmltable.retrieve(mcr_id.getTypeId(),mcr_id);
  setFromXML(xmlarray,false);
  mcr_service.setDate("modifydate");
  getStructure().addDerivate(link);
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.update(mcr_tc,xml,mcr_ts);
  mcr_xmltable.update(mcr_id.getTypeId(),mcr_id,xml);
  }

/**
 * The methode remove a derivate MCRMetaLinkID from the structure part and
 * update the object with the ID in the data store.
 *
 * @param id the object ID
 * @param derivate a link to a derivate as MCRMetaLinkID
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void removeDerivateInDatastore(String id, MCRMetaLinkID link)
  throws MCRPersistenceException
  {
  mcr_id = new MCRObjectID(id);
  byte [] xmlarray = mcr_xmltable.retrieve(mcr_id.getTypeId(),mcr_id);
  setFromXML(xmlarray,false);
  mcr_service.setDate("modifydate");
  int j = getStructure().searchForDerivate(link);
  if (j != -1) {
    getStructure().removeDerivate(j);
    org.jdom.Document xml = createXML();
    MCRTypedContent mcr_tc = createTypedContent();
    String mcr_ts = createTextSearch();
    mcr_persist.update(mcr_tc,xml,mcr_ts);
    mcr_xmltable.update(mcr_id.getTypeId(),mcr_id,xml);
    }
  else {
    throw new MCRPersistenceException("The derivate link "+link.getXLinkHref()+
      " was not found."); 
    }
  }

/**
 * The methode delete the object for the given ID from the data store.
 *
 * @param id   the object ID
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void deleteFromDatastore(String id) throws MCRPersistenceException
  {
  mcr_id = new MCRObjectID(id);
  deleteFromDatastore();
  }

/**
 * The methode delete the object from the data store.
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
private final void deleteFromDatastore() throws MCRPersistenceException
  {
  if (mcr_id == null) {
    throw new MCRPersistenceException("The MCRObjectID is null."); }
  // get the Item
  byte [] xmlarray = mcr_xmltable.retrieve(mcr_id.getTypeId(),mcr_id);
  if (xmlarray == null) {
    logger.info("The MCRObjectID "+mcr_id.getId()+" does not exist.");
    return;
    }
  setFromXML(xmlarray,false);
  // set the derivate data in structure
  MCRDerivate der = null;
  for (int i=0;i<mcr_struct.getDerivateSize();i++) {
    der = new MCRDerivate();
    try {
      der.deleteFromDatastore(getStructure().getDerivate(i).getXLinkHref()); }
    catch (MCRException e) {
      logger.debug(MCRException.getStackTraceAsString(e));
      logger.error(e.getMessage());
      logger.error("Error while deleting derivate.");
      }
    }
  // remove all children
  MCRObject child = null;
  for (int i=0;i<mcr_struct.getChildSize();i++) {
    child = new MCRObject();
    try {
      child.deleteFromDatastore(getStructure().getChild(i).getXLinkHref()); }
    catch (MCRException e) {
      logger.debug(MCRException.getStackTraceAsString(e));
      logger.error(e.getMessage());
      logger.error("Error while deleting child.");
      }
    }
  //
  MCRObjectID parent_id = mcr_struct.getParentID();
  if (parent_id != null) {
    logger.debug("Parent ID = "+parent_id.getId());
    try {
      xmlarray = mcr_xmltable.retrieve(mcr_id.getTypeId(),parent_id);
      MCRObject parent = new MCRObject();
      parent.setFromXML(xmlarray,false);
      parent.mcr_struct.removeChild(mcr_id);
      parent.updateThisInDatastore();
      }
    catch (Exception e) {
      logger.debug(MCRException.getStackTraceAsString(e));
      logger.error("Error while delete child ID in parent object.");
      logger.warn("Attention, the parent "+parent_id+"is now inconsist.");
      }
    }
  // remove him self
  mcr_persist.delete(mcr_id);
  deleteLinksFromTable();
  mcr_xmltable.delete(mcr_id.getTypeId(),mcr_id);
  }

/**
 * The methode return true if the object is in the data store, else return 
 * false.
 *
 * @param id   the object ID
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final static boolean existInDatastore(String id) 
  throws MCRPersistenceException
  { 
  MCRObjectID mcr_id = new MCRObjectID(id);
  return mcr_xmltable.exist(mcr_id.getTypeId(),mcr_id);
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
  { receiveFromDatastore(new MCRObjectID(id)); }

/**
 * The methode receive the object for the given MCRObjectID and stored
 * it in this MCRObject.
 *
 * @param id   the object ID
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void receiveFromDatastore(MCRObjectID id) 
  throws MCRPersistenceException
  {
  mcr_id = id;
  byte [] xml = mcr_xmltable.retrieve(mcr_id.getTypeId(),mcr_id);
  if (xml != null) {
    setFromXML(xml,false); }
  else {
    logger.warn("The XML file for ID "+mcr_id.getId()+" was not retrieved.");
    }
  }

/**
 * The methode receive the object for the given MCRObjectID and returned
 * it as XML stream.
 *
 * @param id   the object ID
 * @return the XML stream of the object as string
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final byte [] receiveXMLFromDatastore(String id) 
  throws MCRPersistenceException
  {
  mcr_id = new MCRObjectID(id);
  byte [] xml = mcr_xmltable.retrieve(mcr_id.getTypeId(),mcr_id);
  if (xml == null) {
    logger.warn("The XML file for ID "+mcr_id.getId()+" was not retrieved.");
    }
  return xml;
  }

/**
 * The methode update the object in the data store.
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void updateInDatastore() throws MCRPersistenceException
  {
  // get the old Item
  MCRObject old = new MCRObject();
  try {
    old.receiveFromDatastore(mcr_id);
    }
  catch (MCRPersistenceException pe) {
    createInDatastore(); return; }
  // clean the structure
  mcr_struct.clear();
  // set the derivate data in structure
  for (int i=0;i<old.mcr_struct.getDerivateSize();i++) {
    mcr_struct.addDerivate(old.mcr_struct.getDerivate(i));
    }
  // set the parent from the original
  if (old.mcr_struct.getParent() != null) {
    mcr_struct.setParent(old.mcr_struct.getParent()); }
  // set the children from the original
  for (int i=0;i<old.mcr_struct.getChildSize();i++) {
    mcr_struct.addChild(old.mcr_struct.getChild(i));
    }
  // import all herited matadata from the parent
  MCRObjectID parent_id = mcr_struct.getParentID();
  if (parent_id != null) {
    logger.debug("Parent ID = "+parent_id.getId());
    try {
      MCRObject parent = new MCRObject();
      parent.receiveFromDatastore(parent_id);
      mcr_metadata.appendMetadata(parent.getMetadata()
        .getHeritableMetadata());
      }
    catch (Exception e) {
      logger.error(MCRException.getStackTraceAsString(e));
      logger.error("Error while merging metadata in this object.");
      }
    }
  // set the date
  mcr_service.setDate("createdate",old.getService().getDate("createdate"));
  // update this dataset
  updateThisInDatastore();
  // update all children
  for (int i=0;i<mcr_struct.getChildSize();i++) {
    MCRObject child = new MCRObject();
    child.updateMetadataInDatastore(mcr_struct.getChild(i).getXLinkHrefID());
    }
  }

/**
 * The method updates this object in the persistence layer.
 **/
private final void updateThisInDatastore()
  throws MCRPersistenceException
  {
  mcr_service.setDate("modifydate");
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.update(mcr_tc,xml,mcr_ts);
  mcr_xmltable.update(mcr_id.getTypeId(),mcr_id,xml);
  deleteLinksFromTable();
  addLinksToTable(mcr_tc);
  }

/**
 * The method update the metadata of the stored dataset and replace the
 * inherited data from the parent.
 *
 * @param child_id  the MCRObjectID of the parent as string
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
private final void updateMetadataInDatastore(MCRObjectID child_id)
  throws MCRPersistenceException
  {
  logger.debug("Update metadata from Child "+child_id.getId());
  // get the XML Stream for the child_id
  receiveFromDatastore(child_id);
  // delete the old inherited data from all metadata elements
  for (int i= 0; i<mcr_metadata.size();i++) {
    mcr_metadata.getMetadataElement(i).removeInheritedObject();
    if (mcr_metadata.getMetadataElement(i).size() == 0) {
      mcr_metadata.removeMetadataElement(i); }
    }
  // import all herited matadata from the parent
  MCRObjectID parent_id = mcr_struct.getParentID();
  if (parent_id != null) {
    logger.debug("Parent ID = "+parent_id.getId());
    try {
      MCRObject parent = new MCRObject();
      parent.receiveFromDatastore(parent_id);
      mcr_metadata.appendMetadata(parent.getMetadata()
        .getHeritableMetadata());
      }
    catch (Exception e) {
      logger.error(MCRException.getStackTraceAsString(e));
      logger.error("Error while merging metadata in this object.");
      }
    }
  // update this dataset
  updateThisInDatastore();
  // update all children
  for (int i=0;i<mcr_struct.getChildSize();i++) {
    MCRObject child = new MCRObject();
    child.updateMetadataInDatastore(mcr_struct.getChild(i).getXLinkHrefID());
    }
  }

/**
 * The method add all class and reference links of this instance to 
 * the link table.
 * 
 * @param mcr_tc the typed content list
 **/
private void addLinksToTable(MCRTypedContent mcr_tc)
  {
  int i = 0;
  while ((i<mcr_tc.getSize())&&(!mcr_tc.getNameElement(i).equals("metadata"))) {
    i++; }
  // add metadata links
  i++;
  while ((i<mcr_tc.getSize())&&(!mcr_tc.getNameElement(i).equals("service"))) {
    if ((mcr_tc.getNameElement(i).equals("type")) && 
       (mcr_tc.getFormatElement(i) == MCRTypedContent.FORMAT_LINK) &&
       (mcr_tc.getValueElement(i).equals("locator"))) {
      try {
        MCRObjectID mcr_link = 
          new MCRObjectID((String)mcr_tc.getValueElement(i+1));
        mcr_linktable.addReferenceLink("href",mcr_id,mcr_link);
        }
      catch (MCRException e) { }
      i++;
      }
    if ((mcr_tc.getNameElement(i).equals("classid")) && 
       (mcr_tc.getFormatElement(i) == MCRTypedContent.FORMAT_CLASSID)) {
      try {
        MCRObjectID mcr_class = 
          new MCRObjectID((String)mcr_tc.getValueElement(i));
        mcr_linktable.addClassificationLink(mcr_id,mcr_class,
          (String)mcr_tc.getValueElement(i+1));
        }
      catch (MCRException e) { }
      i++;
      }
    i++;
    }
  }

/**
 * The method delete all class and reference links of this instance from
 * the link table.
 **/
private void deleteLinksFromTable()
  {
  mcr_linktable.deleteReferenceLink("href",mcr_id);
  mcr_linktable.deleteClassificationLink(mcr_id);
  }

/**
 * The method print all informations about this MCRObject.
 **/
public final void debug()
  {
  logger.debug("MCRObject ID : "+mcr_id.getId());
  logger.debug("MCRObject Label : "+mcr_label);
  logger.debug("MCRObject Schema : "+mcr_schema);
  logger.debug("");
  }
}
