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
 *
 * @version $Revision$ $Date$
 **/
public class MCRBaseCommands
{
  private static Logger logger = Logger.getLogger( MCRBaseCommands.class );
  private static MCRConfiguration config;

  /**
   * Initialize common data.
   **/
  static
  {
    config = MCRConfiguration.instance();
    PropertyConfigurator.configure( config.getLoggingProperties() );
  }

 /**
  * Create a new data base file for the MCRObjectId type.
  *
  * @param mcr_type the MCRObjectId type
  * @return true if all is okay, else false
  **/
  public static boolean createDataBase( String mcr_type )
  {
    // Read config file
    String conf_filename = config.getString( "MCR.persistence_config_" + mcr_type );
    if( ! conf_filename.endsWith( ".xml" ) ) 
      throw new MCRException( "Configuration " + mcr_type + " does not end with .xml" ); 

    logger.info( "Reading file " + conf_filename + " ..." );
    InputStream conf_file = MCRBaseCommands.class.getResourceAsStream( "/" + conf_filename );
    if( conf_file == null )
      throw new MCRException( "Can't read configuration file " + conf_filename );

    org.jdom.Document confdoc = null;
    try 
    { confdoc = new SAXBuilder().build( conf_file ); }
    catch( Exception ex ) 
    { throw new MCRException( "Can't parse configuration file " + conf_file ); }
    // create the database

    if( mcr_type.equals( "derivate" ) ) 
    {
      MCRDerivate der = new MCRDerivate();
      der.createDataBase( mcr_type, confdoc ); 
    }
    else 
    {
      MCRObject obj = new MCRObject();
      obj.createDataBase( mcr_type, confdoc ); 
    }
    return true;
  }
}

