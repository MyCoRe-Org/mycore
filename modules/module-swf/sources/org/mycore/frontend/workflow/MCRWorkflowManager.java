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

// package
package org.mycore.frontend.workflow;

// Imported java classes
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.frontend.cli.MCRDerivateCommands;
import org.mycore.frontend.cli.MCRObjectCommands;

/**
 * This class holds methods to manage the workflow file system of MyCoRe.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/

public class MCRWorkflowManager
{
/** New line */
static String NL = System.getProperty("file.separator");

/** The link table manager singleton */
protected static MCRWorkflowManager singleton;

// Configuration
private static MCRConfiguration config = null;

// logger
static Logger logger=Logger.getLogger(MCRWorkflowManager.class.getName());
 
// The mail sender address
static String sender = "";

// table of workflow directories mail addresses
private Hashtable ht = null;
private Hashtable mt = null;

// The file slash
private static String SLASH = System.getProperty("file.separator");;

/**
 * Returns the workflow manager singleton.
 **/
public static synchronized MCRWorkflowManager instance()
  {
  if( singleton == null ) singleton = new MCRWorkflowManager();
  return singleton;
  }

/**
 * The constructor of this class.
 **/
protected MCRWorkflowManager()
  {
  config = MCRConfiguration.instance();
  // read mail sender address
  sender = config.getString("MCR.editor_mail_sender","mcradmin@localhost");
  // int tables
  ht = new Hashtable();
  mt = new Hashtable();
  }

/**
 * The method return the workflow directory path for a given MCRObjectID type.
 *
 * @param type the MCRObjectID type
 * @return the string of the workflow directory path
 **/
public String getDirectoryPath(String type)
  {
  if (ht.containsKey(type)) { return (String)ht.get(type); }
  String dirname = config.getString("MCR.editor_"+type+"_directory",null);
  if (dirname == null) {
    ht.put(type,".");
    logger.warn("No workflow directory path of "+type+" is in the configuration.");
    return ".";
    }
  ht.put(type,dirname);
  return dirname;
  }

/**
 * The method return the information mail address for a given MCRObjectID type.
 *
 * @param type the MCRObjectID type
 * @param todo the todo action String from the workflow.
 * @return the List of the information mail addresses
 **/
public List getMailAddress(String type, String todo)
  {
  if ((type == null) || ((type = type.trim()).length() ==0)) { 
    return new ArrayList(); }
  if ((todo == null) || ((todo = todo.trim()).length() ==0)) { 
    return new ArrayList(); }
  if (mt.containsKey(type+"_"+todo)) { return (List)mt.get(type+"_"+todo); }
  String mailaddr = config.getString("MCR.editor_"+type+"_"+todo+"_mail","");
  ArrayList li = new ArrayList();
  if ((mailaddr == null) || ((mailaddr = mailaddr.trim()).length() ==0)) {
    mt.put(type,li);
    logger.warn("No mail address for "+type+"_"+todo+" is in the configuration.");
    return li;
    }
  StringTokenizer st = new StringTokenizer(mailaddr,",");
  while (st.hasMoreTokens()) { li.add(st.nextToken()); }
  mt.put(type,li);
  return li;
  }

/**
 * The method return the mail sender adress form the configuration.
 *
 * @return the mail sender adress
 **/
public String getMailSender()
  { return sender; }

/**
 * The method return a ArrayList of file names from objects they are under .../workflow/<em>type/...type...</em>.
 *
 * @param type the MCRObjectID type attribute
 * @return an ArrayList of file names
 **/
public final ArrayList getAllObjectFileNames(String type)
  {
  String dirname = getDirectoryPath(type);
  ArrayList workfiles = new ArrayList();
  if (!dirname.equals(".")) {
    File dir = new File(dirname);
    String [] dirl = null;
    if (dir.isDirectory()) { dirl = dir.list(); }
    if (dirl != null) {
      for (int i=0; i< dirl.length;i++) {
        if ((dirl[i].indexOf(type) != -1) && (dirl[i].endsWith(".xml"))) {
          workfiles.add(dirl[i]); }
        }
      }
    java.util.Collections.sort(workfiles);
    for (int i=0; i<workfiles.size();i++) {
      logger.debug("After the sort workflow "+(String)workfiles.get(i)); }
    }
  return workfiles;
  }

/**
 * The method return a ArrayList of file names form derivates they are under .../workflow/<em>type/...derivate...</em>.
 *
 * @param type the MCRObjectID type attribute
 * @return an ArrayList of file names
 **/
public final ArrayList getAllDerivateFileNames(String type)
  {
  String dirname = getDirectoryPath(type);
  ArrayList workfiles = new ArrayList();
  if (!dirname.equals(".")) {
    File dir = new File(dirname);
    String [] dirl = null;
    if (dir.isDirectory()) { dirl = dir.list(); }
    if (dirl != null) {
      for (int i=0; i< dirl.length;i++) {
        if ((dirl[i].indexOf("_derivate_") != -1) && (dirl[i].endsWith(".xml"))) {
          workfiles.add(dirl[i]); }
        }
      }
    java.util.Collections.sort(workfiles);
    for (int i=0; i<workfiles.size();i++) {
      logger.debug("After the sort workflow "+(String)workfiles.get(i)); }
    }
  return workfiles;
  }

/**
 * The method read a derivate file with name <em>filename</em> in the
 * workflow directory of <em>type</em> and check that this derivate 
 * reference the given <em>ID</em>.
 *
 * @param type the MCRObjectID type
 * @param filename the file name of the derivate
 * @param ID the MCRObjectID of the metadata object
 * @return true if the derivate refernce the metadata object, else return false
 **/
public final boolean isDerivateOfObject(String type, String filename, 
  String ID)
  {
  String dirname = getDirectoryPath(type);
  String fname = dirname+SLASH+filename;
  org.jdom.Document workflow_in = null;
  try {
    workflow_in = MCRXMLHelper.parseURI(fname);
    logger.debug("Readed from workflow "+fname);
    }
  catch( Exception ex ) {
    logger.error( "Error while reading XML workflow file "+filename );
    logger.error( ex.getMessage() );
    return false;
    }
  org.jdom.Element root = workflow_in.getRootElement();
  org.jdom.Element derivate = root.getChild("derivate");
  if (derivate == null) return false;
  org.jdom.Element linkmetas = derivate.getChild("linkmetas");
  if (linkmetas == null) return false;
  org.jdom.Element linkmeta = linkmetas.getChild("linkmeta");
  if (linkmeta == null) return false;
  String DID = linkmeta.getAttributeValue("href",
    org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL));
  logger.debug("The linked object ID of derivate is "+DID);
  if (!ID.equals(DID)) return false;
  return true;
  }

/**
 * The method removes a metadata object with all referenced derivate objects 
 * from the workflow.
 *
 * @param type the MCRObjectID type of the metadata object
 * @param ID the ID of the metadata object
 **/
public final void deleteMetadataObject(String type, String ID)
  {
  // remove metadate
  String fn = getDirectoryPath(type)+SLASH+ID+".xml";
  try {
    File fi = new File(fn);
    if (fi.isFile() && fi.canWrite()) {
      fi.delete(); logger.debug("File "+fn+" removed."); }
    else {
      logger.error("Can't remove file "+fn); }
    }
  catch (Exception ex) {
    logger.error("Can't remove file "+fn); }
  // remove derivate
  ArrayList derifiles = getAllDerivateFileNames(type);
  for (int i=0;i<derifiles.size();i++) {
    String dername = (String)derifiles.get(i);
    logger.debug("Check the derivate file "+dername);
    if (isDerivateOfObject(type,dername,ID)) {
      deleteDerivateObject(type,dername.substring(0,dername.length()-4));
      }
    }
  }

/**
 * The method removes a derivate object from the workflow.
 *
 * @param type the MCRObjectID type of the metadata object
 * @param ID the MCRObjectID of the derivate object as String
 **/
public final void deleteDerivateObject(String type, String DD)
  {
  logger.debug("Delete the derivate "+DD);
  // remove the XML file
  String fn = getDirectoryPath(type)+SLASH+DD;
  try {
    File fi = new File(fn+".xml");
    if (fi.isFile() && fi.canWrite()) {
      fi.delete(); logger.debug("File "+fn+".xml removed."); }
    else {
      logger.error("Can't remove file "+fn+".xml"); }
    }
  catch (Exception ex) {
    logger.error("Can't remove file "+fn+".xml"); }
  // remove all derivate objects
  try {
    File fi = new File(fn);
    if (fi.isDirectory() && fi.canWrite()) {
      // delete files
      ArrayList dellist = MCRUtils.getAllFileNames(fi);
      for (int j=0;j<dellist.size();j++) {
        String na = (String)dellist.get(j);
        File fl = new File(fn+SLASH+na);
        if (fl.delete()) {
          logger.debug("File "+na+" removed."); }
        else {
          logger.error("Can't remove file "+na); }
        }
      // delete subirectories
      dellist = MCRUtils.getAllDirectoryNames(fi);
      for (int j=dellist.size()-1;j>-1;j--) {
        String na = (String)dellist.get(j);
        File fl = new File(fn+SLASH+na);
        if (fl.delete()) {
          logger.debug("Directory "+na+" removed."); }
        else {
          logger.error("Can't remove directory "+na); }
        }
      if (fi.delete()) {
        logger.debug("Directory "+fn+" removed."); }
      else {
        logger.error("Can't remove directory "+fn); }
      }
    else {
      logger.error("Can't remove directory "+fn); }
    }
  catch (Exception ex) {
    logger.error("Can't remove directory "+fn.substring(0,fn.length()-4)); }
  }

/**
 * The method commit a metadata object with all referenced derivate objects 
 * from the workflow to the data store.
 *
 * @param type the MCRObjectID type of the metadata object
 * @param ID the ID of the metadata object
 **/
public final boolean commitMetadataObject(String type, String ID)
  {
  // commit metadata
  String fn = getDirectoryPath(type)+SLASH+ID+".xml";
  MCRObjectCommands.updateFromFile(fn);
  logger.info("The metadata objekt was "+fn+" loaded.");
  // commit derivates
  if (!MCRObject.existInDatastore(ID)) { return false; }
  ArrayList derifiles = getAllDerivateFileNames(type);
  for (int i=0;i<derifiles.size();i++) {
    String dername = (String)derifiles.get(i);
    logger.debug("Check the derivate file "+dername);
    if (isDerivateOfObject(type,dername,ID)) {
      String fd = getDirectoryPath(type)+SLASH+dername;
      MCRDerivateCommands.updateFromFile(fd);
      if (!MCRDerivate.existInDatastore(dername.substring(0,dername.length()-4))) { return false; }
      logger.debug("Commit the derivate "+fd);
      }
    }
  return true;
  }

/**
 * The method commit a derivate object with update method
 * from the workflow to the data store.
 *
 * @param type the MCRObjectID type of the metadata object
 * @param DD the ID as String of the derivate object
 **/
public final boolean commitDerivateObject(String type, String ID)
  {
  String fn = getDirectoryPath(type)+SLASH+ID+".xml";
  MCRDerivateCommands.updateFromFile(fn);
  if (!MCRDerivate.existInDatastore(ID)) { return false; }
  logger.debug("Commit the derivate "+fn);
  return true;
  }

/**
 * The method create a new MCRDerivate and store them to the directory
 * of the workflow that correspons with the type of the given object
 * MCRObjectID with the name of itseslf. Also ti create a ne directory 
 * with the same new name. This new derivate ID was returned.
 *
 * @param objmcrid the MCRObjectID of the related object
 * @return the MCRObjectID of the derivate
 **/
public String createDerivate(String objmcrid)
  {
  // prepare the derivate MCRObjectID
  MCRObjectID ID = new MCRObjectID(objmcrid);
  String myproject = ID.getProjectId() + "_derivate";
  MCRObjectID dmcridnext = new MCRObjectID();
  dmcridnext.setNextFreeId(myproject);
  String workdir = getDirectoryPath(ID.getTypeId());
  File workf = new File(workdir);
  if (workf.isDirectory()) {
    String [] list = workf.list();
    for (int i=0;i<list.length;i++) {
      if (!list[i].startsWith(myproject)) continue;
      if (!list[i].endsWith(".xml")) continue;
      try {
        MCRObjectID dmcriddir = new MCRObjectID(list[i].substring(0,list[i].length()-4));
        if (dmcridnext.getNumberAsInteger() <= dmcriddir.getNumberAsInteger()) {
          dmcriddir.setNumber(dmcriddir.getNumberAsInteger()+1);
          dmcridnext = dmcriddir;
          }
        }
      catch (Exception e) { }
      }
    }
  MCRObjectID DD = dmcridnext;
  logger.debug("New derivate ID "+DD.getId());

  // create a new directory
  String dirname = workdir+NL+DD.getId();
  File dir = new File(dirname);
  dir.mkdir();
  logger.debug("Directory "+dirname+" created.");

  // build the derivate XML file
  MCRDerivate der = new MCRDerivate();
  der.setId(DD);
  der.setLabel("Dataobject from "+ID.getId());
  der.setSchema("datamodel-derivate.xsd");
  MCRMetaLinkID link = new MCRMetaLinkID("linkmetas","linkmeta","de",0);
  link.setReference(ID.getId(),"","");
  der.getDerivate().setLinkMeta(link);
  MCRMetaIFS internal = new MCRMetaIFS("internals","internal","de",
    DD.getId());
  internal.setMainDoc("#####");
  der.getDerivate().setInternals(internal);
  MCRObject obj = new MCRObject();
  try {
    obj.setFromURI(workdir+NL+ID.getId()+".xml");
    MCRObjectService serv = obj.getService();
    der.setService(serv);
    }
  catch (Exception e) {
    try {
      obj.receiveFromDatastore(ID);
      MCRObjectService serv = obj.getService();
      der.setService(serv);
      }
    catch (Exception e2) {
      logger.warn("Read error of "+workdir+NL+ID.getId()+".xml"); }
    }
  byte [] outxml = MCRUtils.getByteArray(der.createXML());

  // Save the prepared MCRDerivate to a file
  String fullname = workdir+NL+DD.getId()+".xml";
  try {
    FileOutputStream out = new FileOutputStream(fullname);
    out.write(outxml);
    out.flush();
    }
  catch (IOException ex) {
    logger.error( ex.getMessage() );
    logger.error( "Exception while store to file " + fullname );
    return "";
    }
  logger.info( "Derivate "+DD.getId()+" stored under "+fullname+"." );
  return DD.getId();
  }

}

