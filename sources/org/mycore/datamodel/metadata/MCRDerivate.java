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
import mycore.xml.MCRXMLHelper;

/**
 * This class implements all methode for handling one derivate object.
 * Methodes of this class can read the XML metadata by using a XML parser,
 * manipulate the data in the abstract persistence data store and return
 * the XML stream to the user application.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRDerivate extends MCRBase
{

/**
 * constant value for the object id length
 **/

// the object content
private MCRObjectDerivate mcr_derivate = null;

/**
 * This is the constructor of the MCRDerivate class. It make an
 * instance of the parser class and the metadata class.
 *
 * @exception MCRException      general Exception of MyCoRe
 * @exception MCRConfigurationException
 *                              a special exception for configuartion data
 */
public MCRDerivate() throws MCRException, MCRConfigurationException
  {
  super();
  // Derivate class
  mcr_derivate = new MCRObjectDerivate();
  }

/**
 * This methode return the instance of the MCRObjectDerivate class.
 * If this was not found, null was returned.
 *
 * @return the instance of the MCRObjectDerivate class
 **/
public final MCRObjectDerivate getDerivate()
  { return mcr_derivate; }

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
  org.jdom.Element jdom_element;
  // get the derivate data of the object
  jdom_element = jdom_element_root.getChild("derivate");
  mcr_derivate.setFromDOM(jdom_element);
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
 * This methode set the object MCRObjectDerivate.
 *
 * @param service   the object MCRObjectDerivate part
 **/
public final void setDerivate(MCRObjectDerivate derivate)
  { if (derivate != null) { mcr_derivate = derivate; } }

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
  org.jdom.Element elm = new org.jdom.Element("mycorederivate");
  org.jdom.Document doc = new org.jdom.Document(elm);
  elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",XSI_URL));
  elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
    XLINK_URL));
  elm.setAttribute("noNamespaceSchemaLocation",mcr_schema_path+SLASH+mcr_schema,
    org.jdom.Namespace.getNamespace("xsi",XSI_URL));
  elm.setAttribute("ID",mcr_id.getId());
  elm.setAttribute("label",mcr_label);
  elm.addContent(mcr_derivate.createXML());
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
  tc.addMCRTypedContent(mcr_derivate.createTypedContent());
  tc.addMCRTypedContent(mcr_service.createTypedContent());
  return tc;
  }

/**
 * This methode create an empty String. They exist only for the interface.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a String with the text values from the metadata object
 **/
public final String createTextSearch()
  throws MCRException
  { return ""; }

/**
 * The methode create a new datastore based of given data. It create
 * a new data table for storing MCRObjects with the same MCRObjectID type.
 *
 * @param confdoc the configuration XML document
 **/
public final void createDataBase(String mcr_type, org.jdom.Document confdoc)
  {
  setId(new MCRObjectID("Template_derivate_1"));
  mcr_persist.createDataBase("derivate", confdoc);
  }

/**
 * The methode create the object in the data store.
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void createInDatastore() throws MCRPersistenceException
  {
  // create the derivate
  mcr_service.setDate("createdate");
  mcr_service.setDate("modifydate");
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.create(mcr_tc,xml,mcr_ts);
  // add the link to metadata
  MCRObject obj;
  for (int i=0;i<getDerivate().getLinkMetaSize();i++) {
    MCRMetaLinkID meta = getDerivate().getLinkMeta(i);
    MCRMetaLinkID der = new MCRMetaLinkID();
    der.setReference(mcr_id.getId(),mcr_label,"");
    der.setSubTag("derivate");
    try {
      obj = new MCRObject();
      obj.addDerivateInDatastore(meta.getXLinkHref(),der); }
    catch (MCRException e) {
      throw new MCRPersistenceException("The MCRObject "+meta.getXLinkHref()+
        " was not found."); }
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
  // get the derivate
  mcr_id = new MCRObjectID(id);
  byte [] xml = mcr_persist.receive(mcr_id);
  setFromXML(xml,false);
  // remove the link to metadata
  MCRObject obj;
  for (int i=0;i<getDerivate().getLinkMetaSize();i++) {
    MCRMetaLinkID meta = getDerivate().getLinkMeta(i);
    MCRMetaLinkID der = new MCRMetaLinkID();
    der.setReference(mcr_id.getId(),mcr_label,"");
    der.setSubTag("derivate");
    try {
      obj = new MCRObject();
      obj.removeDerivateInDatastore(meta.getXLinkHref(),der); }
    catch (MCRException e) {
      System.out.println(e.getMessage()); }
    }
  // delete derivate
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
  MCRDerivate old = new MCRDerivate();
  old.receiveFromDatastore(mcr_id.getId());
  // remove the old link to metadata
  MCRObject obj;
  for (int i=0;i<old.getDerivate().getLinkMetaSize();i++) {
    MCRMetaLinkID meta = old.getDerivate().getLinkMeta(i);
    MCRMetaLinkID der = new MCRMetaLinkID();
    der.setReference(mcr_id.getId(),mcr_label,"");
    der.setSubTag("derivate");
    try {
      obj = new MCRObject();
      obj.removeDerivateInDatastore(meta.getXLinkHref(),der); }
    catch (MCRException e) {
      System.out.println(e.getMessage()); }
    }
  // update the derivate
  mcr_service.setDate("createdate",old.getService().getDate("createdate"));
  mcr_service.setDate("modifydate");
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.update(mcr_tc,xml,mcr_ts);
  // add the link to metadata
  for (int i=0;i<getDerivate().getLinkMetaSize();i++) {
    MCRMetaLinkID meta = getDerivate().getLinkMeta(i);
    MCRMetaLinkID der = new MCRMetaLinkID();
    der.setReference(mcr_id.getId(),mcr_label,"");
    der.setSubTag("derivate");
    try {
      obj = new MCRObject();
      obj.addDerivateInDatastore(meta.getXLinkHref(),der); }
    catch (MCRException e) {
      throw new MCRPersistenceException("The MCRObject "+meta.getXLinkHref()+
        " was not found."); }
    }
  }

}
