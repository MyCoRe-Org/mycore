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
import java.io.*;
import java.text.*;
import java.util.*;

// Imported log4j classes
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// Importes MyCoRe classes
import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.frontend.cli.*;

/**
 * This class holds methods to manage the workflow file system of MyCoRe.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/

public class MCRWorkflowManager
{

/** The link table manager singleton */
protected static MCRWorkflowManager singleton;

// Configuration
private static MCRConfiguration config = null;

// logger
static Logger logger=Logger.getLogger(MCRWorkflowManager.class.getName());
 
// table of workflow directories
private Hashtable ht = null;

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
  // set the logger property
  PropertyConfigurator.configure(config.getLoggingProperties());
  ht = new Hashtable();
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
      logger.debug("Delete the derivate "+dername);
      fn = getDirectoryPath(type)+SLASH+dername;
      try {
        File fi = new File(fn);
        if (fi.isFile() && fi.canWrite()) {
          fi.delete(); logger.debug("File "+fn+" removed."); }
        else {
          logger.error("Can't remove file "+fn); }
        }
      catch (Exception ex) {
        logger.error("Can't remove file "+fn); }
      try {
        File fi = new File(fn.substring(0,fn.length()-4));
        if (fi.isDirectory() && fi.canWrite()) {
          File [] fl = fi.listFiles();
          for (int j=0;j<fl.length;j++) {
            String na = fl[j].getName();
            fl[j].delete(); logger.debug("File "+na+" removed.");
            }
          fi.delete();
          logger.debug("Directory "+fn.substring(0,fn.length()-4)+" removed.");
          }
        else {
          logger.error("Can't remove directory "+fn.substring(0,fn.length()-4)); }
        }
      catch (Exception ex) {
        logger.error("Can't remove directory "+fn.substring(0,fn.length()-4)); }
      }
    }
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

}

