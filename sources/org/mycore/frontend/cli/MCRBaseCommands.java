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

package org.mycore.frontend.cli;

import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;

/**
 * Provides static methods that implement commands for the
 * MyCoRe command line interface.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/

public class MCRBaseCommands
{
  private static String SLASH = System.getProperty( "file.separator" );

 /**
  * Create a new data base file for the MCRObjectId type.
  *
  * @param mcr_type the MCRObjectId type
  * @return true if all is okay, else false
  **/
  public static boolean createDataBase ( String mcr_type )
    {
    MCRConfiguration conf = MCRConfiguration.instance();
    // Application pathes
    String mycore_appl = "";
    try {
      mycore_appl = conf.getString("MCR.appl_path"); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for MCR.appl_path." ); }
    String SLASH = System.getProperty("file.separator");
    // Read config file
    String conf_home = mycore_appl+SLASH+"config";
    String conf_filename = "";
    try {
      conf_filename = conf.getString("MCR.persistence_config_"+mcr_type); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for "+mcr_type ); }
    if(!conf_filename.endsWith(".xml")) {
      throw new MCRException("Configuration "+mcr_type+" ends not with .xml"); }
    File conf_file = new File(conf_home,conf_filename);
    if(! conf_file.isFile()) {
      throw new MCRException("Can't read configuration for "+mcr_type ); }
    System.out.println( "Reading file " + conf_filename + " ...\n" );
    org.jdom.Document confdoc = null;
    try {
      SAXBuilder builder = new SAXBuilder();
      confdoc = builder.build(conf_file); }
    catch ( Exception e ) {
      throw new MCRException("Can't parse configuration for "+mcr_type ); }
    // create the database
    if (mcr_type.equals("derivate")) {
      MCRDerivate der = new MCRDerivate();
      der.createDataBase(mcr_type,confdoc); }
    else {
      MCRObject obj = new MCRObject();
      obj.createDataBase(mcr_type,confdoc); }
    return true;
    }

 /**
  * Create a new XML Schema file for the MCRObjectId type.
  *
  * @param mcr_type the MCRObjectId type
  * @return true if all is okay, else false
  **/
  public static boolean createXMLSchema ( String mcr_type )
    {
    MCRConfiguration conf = MCRConfiguration.instance();
    // Root pathes
    String mycore_root = "";
    try {
      mycore_root = conf.getString("MCR.root_path"); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for MCR.root_path." ); }
    String mycore_appl = "";
    try {
      mycore_appl = conf.getString("MCR.appl_path"); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for MCR.appl_path." ); }
    String SLASH = System.getProperty("file.separator");
    // Read config file
    String conf_home = mycore_appl+SLASH+"config";
    String conf_filename = "";
    try {
      conf_filename = conf.getString("MCR.persistence_config_"+mcr_type); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for "+mcr_type ); }
    if(!conf_filename.endsWith(".xml")) {
      throw new MCRException("Configuration "+mcr_type+" ends not with .xml"); }
    File conf_file = new File(conf_home,conf_filename);
    if(! conf_file.isFile()) {
      throw new MCRException("Can't read configuration for "+mcr_type ); }
    System.out.println( "Reading file " + conf_filename + " ...\n" );
    // Set schema file
    String schema_home = mycore_appl+SLASH+"schema";
    String schema_filename = conf_filename.substring(0,conf_filename.length()-4)
      +".xsd";
    // Read transformer file
    String xslt_home = mycore_root+SLASH+"stylesheets";
    String xslt_filename = "MCRObjectSchema.xsl";
    File xslt_file = new File(xslt_home,xslt_filename);
    if(! xslt_file.isFile()) {
      throw new MCRException("Can't read schema from MCRSchema.xsl"); }
    System.out.println( "Reading file " + xslt_filename + " ...\n" );
    // Transform
    try {
    TransformerFactory transfakt = TransformerFactory.newInstance();
    Transformer trans = transfakt.newTransformer(new StreamSource(xslt_file));
    trans.setParameter("mycore_home",mycore_root);
    trans.setParameter("mycore_appl",mycore_appl);
    trans.transform(new StreamSource(conf_file),new StreamResult(schema_home+
      System.getProperty("file.separator")+schema_filename));
    System.out.println( "Write file " + schema_filename + " ...\n" ); }
    catch ( Exception e ) {
      throw new MCRException(e.getMessage()); }
    return true;
    }

}
