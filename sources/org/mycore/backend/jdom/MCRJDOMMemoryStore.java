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

package org.mycore.backend.jdom;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class implements the memory store based on JDOM trees.
 *
 * @author Jens Kupferschmidt
 *
 * @version $Revision$ $Date$
 **/
public class MCRJDOMMemoryStore
  {
  /** The connection pool singleton */
  protected static MCRJDOMMemoryStore singleton;

  /** The logger */
  private static Logger logger=Logger.getLogger("org.mycore.backend.jdom");

  /** A hashtable of the JDOM trees */
  private Hashtable trees = null;

  /** The search XSL file */
  private InputStream searchxsl = null;
  private org.jdom.Document xslorig = null;

  /** The XSL namespace */
  private org.jdom.Namespace ns = null;

  /** Timestamp of the last SQL read and the default reload time in seconds */
  private Date ts = null;
  private long tsdiff = 0;
  private static final int tsdiffdefault = 3600;

/**
 * Returns the link table manager singleton.
 **/
public static synchronized MCRJDOMMemoryStore instance()
  {
  if( singleton == null ) singleton = new MCRJDOMMemoryStore();
  return singleton;
  }

/**
 * The constructor of this class.
 **/
protected MCRJDOMMemoryStore()
  {
  MCRConfiguration config = MCRConfiguration.instance();
  // set the logger property
  PropertyConfigurator.configure(config.getLoggingProperties());
  // initalize the table
  trees = new Hashtable();
  // XSL Namespace
  ns = org.jdom.Namespace.getNamespace("xsl",MCRDefaults.XSL_URL);
  // Read stylesheet
  searchxsl = MCRJDOMMemoryStore.class.getResourceAsStream( "/MCRJDOMSearch.xsl"); 
  if( searchxsl == null ) throw new MCRConfigurationException( "Can't find stylesheet file MCRJDOMSearch.xsl" ); 
  try {
    xslorig = (new org.jdom.input.SAXBuilder()).build(searchxsl); }
  catch (Exception e) {
    throw new MCRException("Error while read XML file MCRJDOMSearch.xsl."); }
  // set the start time and the diff from the config
  ts = new Date();
  tsdiff = (config.getInt("MCR.persistence_jdom_reload",tsdiffdefault))*1000;
  }

/**
 * The method return a org.jdom.document from the MCRJDOMSearch.xsl file.
 *
 * @param query the XSLT String to select in the for-each loop
 * @return a org.jdom.Document of the stylesheet
 **/
public final org.jdom.Document getStylesheet(String query)
  { 
  org.jdom.Document xslfile = (org.jdom.Document)xslorig.clone();
  try {
    org.jdom.Element root = xslfile.getRootElement();
    org.jdom.Element template = root.getChild("template",ns);
    org.jdom.Element result = template.getChild("mcr_search_results");
    org.jdom.Element foreach = result.getChild("for-each",ns);
    foreach.removeAttribute("select");
    foreach.setAttribute("select",query);
    // debug
    org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter(org.jdom.output.Format.getPrettyFormat());
    outputter.output(xslfile, System.out);
    }
  catch (Exception e) {
    throw new MCRException("Error while show XML to file."); }
  return xslfile;
  }

/**
 * The method check the type.
 *
 * @param type the table type
 * @exception if the store for the given type could not find or loaded.
 **/
protected final org.jdom.Element retrieveType(String type)
  {
  // return the JDOM tree if it is in the store
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    throw new MCRPersistenceException("The type is null or empty."); }
  // check the reload
  org.jdom.Element store = null;
  if ((new Date()).getTime() <= (ts.getTime()+tsdiff)) {
    store = (org.jdom.Element)trees.get(type); }
  if (store != null) { return store; }
  ts = new Date();
  trees.remove(type);
  // fill the store form the SQL store of the type
  org.jdom.Element root = new org.jdom.Element("root");
  // read the SQL data
  Date startdate = new Date();
  MCRXMLTableManager mcr_xml = MCRXMLTableManager.instance();
  ArrayList ar = mcr_xml.retrieveAllIDs(type);
  String stid = null;
  for (int i=0;i<ar.size();i++) {
    stid = (String)ar.get(i);
    MCRObjectID mid = new MCRObjectID(stid);
    byte [] xml = mcr_xml.retrieve(type,mid);
    try {
      org.jdom.Document jdom_document = MCRXMLHelper.parseXML(xml,true); 
      org.jdom.Element jdom_rootelm = jdom_document.getRootElement();
      jdom_rootelm.detach();
      root.addContent(jdom_rootelm);
      }
    catch (Exception e) {
      logger.warn("Can't add "+(String)ar.get(i)+" to JDOM tree!"); }
    logger.debug("Load to JDOM tree "+(String)ar.get(i));
    }
  Date stopdate = new Date();
  float diff = (stopdate.getTime()-startdate.getTime())/1000;
  logger.debug("Read "+Integer.toString(ar.size())+" SQL data sets for type "+type+" in "+Float.toString(diff)+" s");
  trees.put(type,root);
  
  //debug(root);
  return root;
  }

/**
 * Add a new org.jdom.Element to a tree of a type.
 *
 * @param type the MCRObjectID type
 * @param em the root of the org.jdom.Document as  org.jdom.Element
 **/
 
protected final void addElementOfType(String type, org.jdom.Element elm) 
  {
  // get root for type
  org.jdom.Element root = retrieveType(type);
  // add
  root.addContent(elm);
  }

/** 
 * Remove a org.jdom.Element object from the tree of a type.
 *
 * @param type the MCRObjectID type
 * @param id the ID they should be removed
 **/
protected final void removeElementOfType(String type, MCRObjectID mcr_id)
  {
  String id = mcr_id.getId();
  // get root for type
  org.jdom.Element root = retrieveType(type);
  // find child and remove them
  List list = root.getChildren();
  for (int i=0; i<list.size(); i++) {
    org.jdom.Element obj = (org.jdom.Element) list.get(i);
    if (obj.getAttributeValue("ID").equals(id)) {
      root.removeContent(i);
      break;
      }
    }
  }

/**
 * The method debug the content of root.
 **/
protected final void debug(org.jdom.Element root)
  {
  logger.debug("ROOT   : "+root.getName());
  List listone = root.getChildren();
  for (int i=0; i<listone.size(); i++) {
    org.jdom.Element elmone = (org.jdom.Element)listone.get(i);
    String id = elmone.getAttributeValue("ID");
    logger.debug("DEEP 1 : "+elmone.getName()+" with ID "+id);
    }
  }
}

