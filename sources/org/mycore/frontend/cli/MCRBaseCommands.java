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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

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
  private static Logger logger =
    Logger.getLogger(MCRDerivateCommands.class.getName());
  private static MCRConfiguration config = null;

 /**
  * Initialize common data.
  **/
  private static void init()
    {
    config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
    }

 /**
  * Create a new data base file for the MCRObjectId type.
  *
  * @param mcr_type the MCRObjectId type
  * @return true if all is okay, else false
  **/
  public static boolean createDataBase ( String mcr_type )
    {
    init();
    // Application pathes
    String mycore_appl = "";
    try {
      mycore_appl = config.getString("MCR.appl_path"); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for MCR.appl_path." ); }
    String SLASH = System.getProperty("file.separator");
    // Read config file
    String conf_home = mycore_appl+SLASH+"config";
    File conf_dir = new File(conf_home);
    if(! conf_dir.isDirectory()) {
      throw new MCRException("Can't find the configuration directory "+
        conf_home ); }
    String conf_filename = "";
    try {
      conf_filename = config.getString("MCR.persistence_config_"+mcr_type); }
    catch ( Exception e ) {
      throw new MCRException("Can't find configuration for "+mcr_type ); }
    if(!conf_filename.endsWith(".xml")) {
      throw new MCRException("Configuration "+mcr_type+" ends not with .xml"); }
    File conf_file = new File(conf_home,conf_filename);
    if(! conf_file.isFile()) {
      throw new MCRException("Can't read configuration for "+mcr_type ); }
    logger.info( "Reading file " + conf_filename + " ...\n" );
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
}
