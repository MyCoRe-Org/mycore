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

import org.jdom.transform.JDOMSource;
import org.mycore.common.*;
import org.mycore.services.query.MCRQueryCollector;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * This class implements the query command to start a query to a local
 * Library Server or to any remote Library Server in a configuration
 * list or to a dedicated named remote Library Server.
 * The result was presided as a text output.
 *
 * @author Jens Kupferschmidt
 * @author Frank L�tzenkirchen
 * @author Mathias Zarick
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 **/
final class MCRQueryCommands
{

// logger
private static final Logger logger=Logger.getLogger(MCRQueryCommands.class.getName());

private static MCRQueryCollector collector;

/** Executes a local query */
public static void queryLocal( String type, String query )
{ query( "local", type, query ); }

/** Executes a remote query */
public static void queryRemote( String type, String query )
{ query( "remote", type, query ); }

/**
 * The query command
 *
 * @param host  either "local", "remote" or hostname
 * @param type  the document type, "class" or "document" or ...
 * @param query the query expression
 **/
  public static void query( String host, String type, String query )
  {
    MCRConfiguration config = MCRConfiguration.instance();
    PropertyConfigurator.configure( config.getLoggingProperties() );
	if (collector==null){
		int cThreads=config.getInt("MCR.Collector_Thread_num",2);
		int aThreads=config.getInt("MCR.Agent_Thread_num",6);
		collector=new MCRQueryCollector(cThreads,aThreads);
	}

    // input parameters
    if( host  == null ) host = "local";
    if( type  == null ) return; else type = type.toLowerCase();
    if( query == null ) query = "";

    logger.info("Query Host = " + host  );
    logger.info("Query Type = " + type  );
    logger.info("Query      = " + query );

    MCRXMLContainer resarray = new MCRXMLContainer();

    if( type.equals( "class" ) ) // classifications
    {
      String squence = config.getString(
        "MCR.classifications_search_sequence", "remote-local" );
      synchronized(resarray){
		if( squence.equalsIgnoreCase( "local-remote" ) ) 
		{
		  collector.collectQueryResults( "local", type, query, resarray );
		  	try {
				resarray.wait();
		  	} catch (InterruptedException ignored) {}
		  if( resarray.size() == 0 ) 
		  {
			collector.collectQueryResults( host, type, query, resarray );
		  	try {
				resarray.wait();
		  	} catch (InterruptedException ignored) {}
		  }
		}
		else 
		{
			collector.collectQueryResults( host, type, query, resarray );
			try{
				resarray.wait();
			} catch (InterruptedException ignored) {}
		  if( resarray.size() == 0 ) 
		  {
			collector.collectQueryResults( "local", type, query, resarray );
			try{
				resarray.wait();
			} catch (InterruptedException ignored) {}
		  }
		}
      }

      if( resarray.size() == 0 )
        logger.error( "No classification or category exists" ); 
    }
    else // other types
    {
    	synchronized(resarray){
			collector.collectQueryResults( "local", type, query, resarray );
			try{
				resarray.wait();
			} catch (InterruptedException ignored) {}
    	}
    }

    String xslfile = "mcr_results-PlainText-" + type + ".xsl";
    InputStream in = MCRQueryCommands.class.getResourceAsStream( "/" + xslfile ); 
    if( in == null ) throw new MCRConfigurationException( "Can't read stylesheet file " + xslfile ); 

    try
    {
      StreamSource source = new StreamSource( in );
      TransformerFactory transfakt = TransformerFactory.newInstance();
      Transformer trans = transfakt.newTransformer( source );
      StreamResult sr = new StreamResult( (OutputStream)System.out );
      trans.transform(new JDOMSource(resarray.exportAllToDocument()),sr);
    }
    catch( Exception ex )
    {
      logger.error( "Error while tranforming query result XML using XSLT" );
      logger.debug( ex.getMessage() );
      logger.info( "Stop." );
      logger.info( "" );
      return;
    }

    logger.info( "" );
    logger.info( "Ready." );
    logger.info( "" );
  }
}

