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
final public class MCRDerivate
{

/**
 * constant value for the object id length
 **/
public final static int MAX_LABEL_LENGTH = 256;

// from configuration
private static MCRConfiguration mcr_conf = null;
private static String mcr_encoding = null;
private static String mcr_schema_path = null;
private static String persist_name;
private static String persist_type;

// interface classes
private static MCRObjectPersistenceInterface mcr_persist;

// the DOM document
private org.jdom.Document jdom_document = null;

// the object content
private MCRObjectID mcr_id = null;
private String mcr_label = null;
private String mcr_schema = null;
private MCRObjectDerivate mcr_derivate = null;
private MCRObjectService mcr_service = null;

// other
private static String NL;
private static String SLASH;
public final static String XLINK_URL = "http://www.w3.org/1999/xlink"; 
public final static String XSI_URL = "http://www.w3.org/2001/XMLSchema-instance";

/**
 * Load static data for all MCRDerivates
 **/
static
  {
  NL = System.getProperty("line.separator");
  SLASH = System.getProperty("file.separator");
  try {
    // Load the configuration
    mcr_conf = MCRConfiguration.instance();
    // Default Encoding
    mcr_encoding = mcr_conf.getString("MCR.metadata_default_encoding",
      "ISO_8859-1");
    // Path of XML schema
    mcr_schema_path = mcr_conf.getString("MCR.appl_path")+SLASH+"schema";
    // Set persistence layer
    persist_type = mcr_conf.getString("MCR.persistence_type","cm7");
    String proppers = "MCR.persistence_"+persist_type.toLowerCase()+
      "_class_name";
    persist_name = mcr_conf.getString(proppers);
    mcr_persist = (MCRObjectPersistenceInterface)Class.forName(persist_name)
      .newInstance(); 
    }
  catch (Exception e) {
     throw new MCRException(e.getMessage(),e); }
  }

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
  mcr_id = new MCRObjectID();
  mcr_label = new String("");
  mcr_schema = new String("");
  // Derivate class
  mcr_derivate = new MCRObjectDerivate();
  // Service class
  mcr_service = new MCRObjectService();
  }

/**
 * This methode return the derivate id. If this is not set, null was returned.
 *
 * @return the id as MCRObjectID
 **/
public final MCRObjectID getId()
  { return mcr_id; }

/**
 * This methode return the derivate label. If this is not set, null was returned.
 *
 * @return the lable as a string
 **/
public final String getLabel()
  { return mcr_label; }

/**
 * This methode return the instance of the MCRObjectDerivate class.
 * If this was not found, null was returned.
 *
 * @return the instance of the MCRObjectDerivate class
 **/
public final MCRObjectDerivate getDerivate()
  { return mcr_derivate; }

/**
 * This methode return the instance of the MCRObjectService class.
 * If this was not found, null was returned.
 *
 * @return the instance of the MCRObjectService class
 **/
public final MCRObjectService getService()
  { return mcr_service; }

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
 * This methode set the object MCRObjectDerivate.
 *
 * @param service   the object MCRObjectDerivate part
 **/
public final void setDerivate(MCRObjectDerivate derivate)
  { if (derivate != null) { mcr_derivate = derivate; } }

/**
 * This methode set the object MCRObjectService.
 *
 * @param service   the object MCRObjectService part
 **/
public final void setService(MCRObjectService service)
  { if (service != null) { mcr_service = service; } }

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
public void createDataBase(org.jdom.Document confdoc)
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
  mcr_service.setDate("createdate");
  mcr_service.setDate("modifydate");
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.create(mcr_tc,xml,mcr_ts);
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
  mcr_service.setDate("createdate",
    mcr_persist.receiveServiceDate(mcr_id,"createdate"));
  mcr_service.setDate("modifydate");
  org.jdom.Document xml = createXML();
  MCRTypedContent mcr_tc = createTypedContent();
  String mcr_ts = createTextSearch();
  mcr_persist.update(mcr_tc,xml,mcr_ts);
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
