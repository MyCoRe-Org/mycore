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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.GregorianCalendar;
import java.util.Vector;
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
  mcr_struct = new MCRObjectStructure();
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
 * <em>addChild</em> creates a (bidirectional) link to a child object.
 * The child inherits the heritable metadata part of this object
 * and its forefathers.
 * Note: In order to prevent from multiple inheritance, a child link cannot
 *       occur twice with the same href string !
 * 
 * @param child                 the child MCRObject
 * @param label                 the link's label
 * @param titleChild            the child link's title
 * @param titleParent           the parent's title
 * @return                      true, if operation successfully completed
 * @exception MCRException      thrown for multiple inheritance request
 */
public final boolean addChild (MCRObject child, String label,
                               String titleChild, String titleParent)
  throws MCRException
{
  boolean flag = mcr_struct.addChild(child.mcr_id.getId(), label, titleChild);
  Vector inh_metadata = new Vector();
  inh_metadata.addElement(mcr_metadata.getHeritableMetadata());
  Vector inh_fore = mcr_struct.getInheritedMetadata();
  if (inh_fore != null)
  {
    for (int i = 0; i < inh_fore.size(); ++i)
      inh_metadata.addElement((MCRObjectMetadata) inh_fore.elementAt(i));
  }
  return flag &&
    child.mcr_struct.setParent(mcr_id.getId(), label, titleParent, inh_metadata);
}

/** <em>removeChild</em> removes a child link. If the link was
 * found, a "true" will be returned, otherwise "false".
 *
 * @param dest                  the link's destination MCRObject
 * @return                      true, if operation successfully completed
 */
public final boolean removeChild (MCRObject dest)
{
  return mcr_struct.removeChild(dest.mcr_id.getId())
    && dest.mcr_struct.removeParent(mcr_id.getId());
}

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
     org.jdom.Namespace.getNamespace("xsi",XSI_URL)).getValue().trim();
  int i=0;
  int j=0;
  while (j!=-1) {
    j = mcr_schema.indexOf(SLASH,i+1); if (j!=-1) { i = j; } }
  mcr_schema = mcr_schema.substring(i+1,mcr_schema.length());
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
    org.jdom.input.DOMBuilder bulli = new org.jdom.input.DOMBuilder(false);
    jdom_document = bulli.build(MCRXMLHelper.parseURI(uri));
    }
  catch (Exception e) {
    throw new MCRException(e.getMessage()); }
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
    org.jdom.input.DOMBuilder bulli = new org.jdom.input.DOMBuilder(false);
    jdom_document = bulli.build(new ByteArrayInputStream(xml));
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
    debug();
    throw new MCRException("The content is not valid."); }
  org.jdom.Element elm = new org.jdom.Element("mycoreobject");
  org.jdom.Document doc = new org.jdom.Document(elm);
  elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",XSI_URL));
  elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
    XLINK_URL));
  elm.setAttribute("noNamespaceSchemaLocation",mcr_schema_path+SLASH+mcr_schema,
    org.jdom.Namespace.getNamespace("xsi",XSI_URL));
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
    debug();
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(tc.TYPE_MASTERTAG,"mycoreobject");
  tc.addStringElement(tc.TYPE_ATTRIBUTE,"ID",mcr_id.getId());
  tc.addStringElement(tc.TYPE_ATTRIBUTE,"label",mcr_label);
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
  mcr_service.setDate("createdate");
  mcr_service.setDate("modifydate");
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.create(mcr_tc,xml,mcr_ts);
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
  byte [] xmlarray = mcr_persist.receive(mcr_id);
  setFromXML(xmlarray,false);
  mcr_service.setDate("modifydate");
  getStructure().addDerivate(link);
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.update(mcr_tc,xml,mcr_ts);
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
  byte [] xmlarray = mcr_persist.receive(mcr_id);
  setFromXML(xmlarray,false);
  mcr_service.setDate("modifydate");
  int j = getStructure().searchForDerivate(link);
  if (j != -1) {
    getStructure().removeDerivate(j);
    org.jdom.Document xml = createXML();
    MCRTypedContent mcr_tc = createTypedContent();
    String mcr_ts = createTextSearch();
    mcr_persist.update(mcr_tc,xml,mcr_ts);
    }
  else {
    throw new MCRPersistenceException("The derivate link "+link.getXLinkHref()+
      " was not found."); 
    }
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
  // get the Item
  byte [] xml = mcr_persist.receive(mcr_id);
  setFromXML(xml,false);
  // set the derivate data in structure
  MCRDerivate der;
  for (int i=0;i<getStructure().getDerivateSize();i++) {
    der = new MCRDerivate();
    try {
      der.deleteFromDatastore(getStructure().getDerivate(i).getXLinkHref()); }
    catch (MCRException e) {
      System.out.println(e.getMessage()); }
    }
  mcr_persist.delete(mcr_id);
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
  { return mcr_persist.exist(new MCRObjectID(id)); }

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
  byte [] xml = mcr_persist.receive(mcr_id);
  setFromXML(xml,false);
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
  return mcr_persist.receive(mcr_id);
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
  old.receiveFromDatastore(mcr_id.getId());
  // set the derivate data in structure
  for (int i=0;i<old.getStructure().getDerivateSize();i++) {
    getStructure().addDerivate(old.getStructure().getDerivate(i));
    }
  // set the date
  mcr_service.setDate("createdate",old.getService().getDate("createdate"));
  mcr_service.setDate("modifydate");
  // update all
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.update(mcr_tc,xml,mcr_ts);
  }

}
