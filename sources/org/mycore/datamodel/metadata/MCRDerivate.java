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

import java.io.File;
//import java.util.Vector;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFileImportExport;

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
     org.jdom.Namespace.getNamespace("xsi",MCRDefaults.XSI_URL)).getValue()
     .trim();
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
     jdom_document = MCRXMLHelper.parseURI(uri);
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
    jdom_document = MCRXMLHelper.parseXML(xml,false);
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
    throw new MCRException("The content is not valid."); }
  org.jdom.Element elm = new org.jdom.Element("mycorederivate");
  org.jdom.Document doc = new org.jdom.Document(elm);
  elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
    MCRDefaults.XSI_URL));
  elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
  elm.setAttribute("noNamespaceSchemaLocation",mcr_schema,
    org.jdom.Namespace.getNamespace("xsi",MCRDefaults.XSI_URL));
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
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(MCRTypedContent.TYPE_MASTERTAG,"mycoreobject");
  tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE,"ID",mcr_id.getId());
  tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE,"label",mcr_label);
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
  mcr_persist.createDataBase(mcr_type, confdoc);
  }

/**
 * The methode create the object in the data store.
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void createInDatastore() throws MCRPersistenceException
  {
  // exist the derivate?
  if (existInDatastore(mcr_id.getId())) {
    throw new MCRPersistenceException("The derivate "+mcr_id.getId()+
      " allready exists, nothing done."); }
  // prepare the derivate metadata and store under the XML table
  mcr_service.setDate("createdate");
  mcr_service.setDate("modifydate");
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_xmltable.create(mcr_id.getTypeId(),mcr_id,xml);
  // create data in IFS
  if (getDerivate().getInternals() != null) {
    File f = new File(getDerivate().getInternals().getSourcePath());
    if ((!f.isDirectory()) && (!f.isFile())) {
      mcr_xmltable.delete(mcr_id.getTypeId(),mcr_id);
      throw new MCRPersistenceException("The File or Directory on "+
        getDerivate().getInternals().getSourcePath()+" was not found."); }
    try {
      MCRDirectory difs = MCRFileImportExport.importFiles(f,mcr_id.getId());
      getDerivate().getInternals().setIFSID(difs.getID());
      }
    catch (Exception e) { 
      mcr_xmltable.delete(mcr_id.getTypeId(),mcr_id);
      throw new MCRPersistenceException("Error while creating "+
        getDerivate().getInternals().getSourcePath()+" in the IFS.",e); }
    }
  // create the derivate in the search store
  try {
    mcr_persist.create(mcr_tc,xml,mcr_ts); }
  catch (Exception e) { 
    // delete from IFS
    MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());
    difs.delete();
    // delete from the XML table
    mcr_xmltable.delete(mcr_id.getTypeId(),mcr_id);
    // throw final exception
    throw new MCRPersistenceException("Error while creating derivate in"+
      " datastore.",e);
    }
  // add the link to metadata
  MCRObject obj;
  for (int i=0;i<getDerivate().getLinkMetaSize();i++) {
    MCRMetaLinkID meta = getDerivate().getLinkMeta(i);
    MCRMetaLinkID der = new MCRMetaLinkID();
    der.setReference(mcr_id.getId(),mcr_label,"");
    der.setSubTag("derobject");
    try {
      obj = new MCRObject();
      obj.addDerivateInDatastore(meta.getXLinkHref(),der); }
    catch (Exception e) {
      // delete from IFS
      MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());
      difs.delete();
      // delete search data
      mcr_persist.delete(mcr_id);
      // delete from the XML table
      mcr_xmltable.delete(mcr_id.getTypeId(),mcr_id);
      // throw final exception
      throw new MCRPersistenceException("Error while creatlink to MCRObject "+
        meta.getXLinkHref()+".",e);
      }
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
  try { mcr_id = new MCRObjectID(id); }
  catch (MCRException e) {
    throw new MCRPersistenceException("ID error in deleteFromDatastore for "+
      id+".",e);
    }
  byte [] xml = mcr_xmltable.retrieve(mcr_id.getTypeId(),mcr_id);
  // remove the link from metadata object
  if (xml != null) {
    setFromXML(xml,false);
    MCRObject obj;
    for (int i=0;i<getDerivate().getLinkMetaSize();i++) {
      MCRMetaLinkID meta = getDerivate().getLinkMeta(i);
      MCRMetaLinkID der = new MCRMetaLinkID();
      der.setReference(mcr_id.getId(),mcr_label,"");
      der.setSubTag("derobject");
      try {
        obj = new MCRObject();
        obj.removeDerivateInDatastore(meta.getXLinkHref(),der); }
      catch (MCRException e) {
        logger.warn("Error while delete link from MCRObject "
          +meta.getXLinkHref()+".");
        }
      }
    }
  // delete data from IFS
  try {
    MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());
    difs.delete();
    }
  catch (Exception e) {
    if (xml != null) {
      if (getDerivate().getInternals() != null) {
        logger.warn("Error while delete from IFS for ID "+getDerivate()
          .getInternals().getIFSID());
        }
      }
    else {
      logger.warn("Error while clean IFS for ID "+id); }
    }
  // delete derivate from search store
  try { mcr_persist.delete(mcr_id); }
  catch (MCRException e) { logger.warn(e.getMessage()); }
  // delete derivate from XML table
  if (xml != null) {
    mcr_xmltable.delete(mcr_id.getTypeId(),mcr_id); }
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
  {
  mcr_id = new MCRObjectID(id);
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
 * The methode receive the multimedia object(s) for the given MCRObjectID 
 * and returned it as MCRDirectory.
 *
 * @param id   the object ID
 * @return the MCRDirectory of the multimedia object 
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final MCRDirectory receiveDirectoryFromIFS(String id) 
  throws MCRPersistenceException
  {
  // check the ID
  mcr_id = new MCRObjectID(id);
  // receive the IFS informations
  MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());
  if (difs == null) {
    throw new MCRPersistenceException("Error while receiving derivate with "+
      "ID "+mcr_id.getId()+" from IFS.");
    }
  return difs;
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
  // update to IFS
  if (getDerivate().getInternals() != null) {
    File f = new File(getDerivate().getInternals().getSourcePath());
    if ((!f.isDirectory()) && (!f.isFile())) {
      throw new MCRPersistenceException("The File or Directory on "+
      getDerivate().getInternals().getSourcePath()+" was not found."); }
    try {
      MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());
      MCRFileImportExport.importFiles(f,difs);
      getDerivate().getInternals().setIFSID(difs.getID());
      }
    catch (Exception e) { e.printStackTrace(); }
    }
  // update the derivate
  mcr_service.setDate("createdate",old.getService().getDate("createdate"));
  mcr_service.setDate("modifydate");
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_xmltable.update(mcr_id.getTypeId(),mcr_id,xml);
  mcr_persist.update(mcr_tc,xml,mcr_ts);
  // add the link to metadata
  for (int i=0;i<getDerivate().getLinkMetaSize();i++) {
    MCRMetaLinkID meta = getDerivate().getLinkMeta(i);
    MCRMetaLinkID der = new MCRMetaLinkID();
    der.setReference(mcr_id.getId(),mcr_label,"");
    der.setSubTag("derobject");
    try {
      obj = new MCRObject();
      obj.addDerivateInDatastore(meta.getXLinkHref(),der); }
    catch (MCRException e) {
      throw new MCRPersistenceException("The MCRObject "+meta.getXLinkHref()+
        " was not found."); }
    }
  }

}
