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

package org.mycore.backend.xmldb;

import java.io.*;
import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.jdom.Document;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;


import org.xmldb.api.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This class is the persistence layer for XML:DB databases.
 *
 * @author Harald Richter
 * @author Jens Kupferschmidt
 *
 * @version $Revision$ $Date$
 **/
public final class MCRXMLDBPersistence 
    implements MCRObjectPersistenceInterface {
    static Logger logger = Logger.getLogger( MCRXMLDBPersistence.class.getName() );
    static private MCRConfiguration config = MCRConfiguration.instance();  
    static private String def_enc = config.getString( "MCR.metadata_default_encoding" );
    static private String store_enc = config.getString( "MCR.persistence_xmldb_encoding" );
    static private SAXBuilder builder = new SAXBuilder();

    /**
     * Creates a new MCRXMLDBPersistence.
     **/
    public MCRXMLDBPersistence() throws MCRPersistenceException {
        MCRXMLDBConnectionPool.instance();
    }
    /**
     * This method creates and stores the data from MCRTypedContent and
     * XML data in the XMLDB datastore.
     *
     * @param mcr_tc the typed contend of all searchable data
     * @param doc    the XML document as DOM tree
     * @param mct_ts the string for the text search
     * @exception MCRPersistenceException if an error was occured
     **/
    public final void create( MCRTypedContent mcr_tc, Document doc, String mcr_ts ) 
	throws MCRPersistenceException {
        Collection collection = null;
	try {
            // get the MCRObjectID and the label from the typed content
            logger.debug("MCRXMLDBPersistence create: Document       : " + doc);
	    MCRObjectID mcr_id = null;
	    String mcr_label = null;
	    for( int i = 0; i < mcr_tc.getSize(); i++ ) {
		if( mcr_tc.getNameElement( i ).equals( "ID" ) ) {
		    mcr_id = new MCRObjectID( (String)mcr_tc.getValueElement( i ) ); 
		    mcr_label = (String)mcr_tc.getValueElement( i+1 ); }
	    }
            logger.debug("MCRXMLDBPersistence create: MCRObjectID    : " + 
              mcr_id.getId());
            logger.debug("MCRXMLDBPersistence create: MCRLabel       : " +
              mcr_label);
            // open the collection
	    collection = MCRXMLDBConnectionPool.instance().getConnection( mcr_id.getTypeId() );
            // check that the item not exist
            XMLResource res = (XMLResource)collection.getResource( mcr_id.getNumberAsString() );
            if (res != null) {
              throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+" exists."); }
            // create a new item
            res = (XMLResource)collection.createResource( mcr_id.getNumberAsString(),
                XMLResource.RESOURCE_TYPE );
            org.jdom.output.DOMOutputter outputter = new org.jdom.output.DOMOutputter();
            org.w3c.dom.Document dom = outputter.output( doc );
            res.setContentAsDOM( dom );
            collection.storeResource( res );
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e ); }
        finally {
            MCRXMLDBConnectionPool.instance().releaseConnection( collection ); }
    }

    /**
     * Creates an empty database from the given configuration
     * file. Currently not used in this persistence layer.
     *
     * @param mcr_type
     * @param mcr_conf
     *
     * @throws MCRConfigurationException 
     * @throws MCRPersistenceException
     **/
    public void createDataBase( String mcr_type, org.jdom.Document mcr_conf )
	throws MCRConfigurationException, 
	       MCRPersistenceException {
	try {
/*            
	    CollectionManagementService cms = 
		(CollectionManagementService)
		rootCollection.getService( "CollectionManagementService",
					   "1.0" );
	    cms.createCollection( mcr_type.toLowerCase() );
 */
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}
    }

    /**
     * Updates the content in the database. Currently the same as
     * delete and then a new create. Should be made with XUpdate in the future.
     *
     * @param mcr_tc the typed contend of all searchable data
     * @param doc    the XML document as DOM tree
     * @param mct_ts the string for the text search
     * @exception MCRPersistenceException if an error was occured
     **/    
    public void update( MCRTypedContent mcr_tc, Document doc, String mcr_ts ) 
	throws MCRPersistenceException {
        Collection collection = null;
	try {
            // get the MCRObjectID and the label from the typed content
            logger.debug("MCRXMLDBPersistence update: Document       : " + doc);
	    MCRObjectID mcr_id = null;
	    String mcr_label = null;
	    for( int i = 0; i < mcr_tc.getSize(); i++ ) {
		if( mcr_tc.getNameElement( i ).equals( "ID" ) ) {
		    mcr_id = new MCRObjectID( (String)mcr_tc.getValueElement( i ) ); 
		    mcr_label = (String)mcr_tc.getValueElement( i+1 ); }
	    }
            logger.debug("MCRXMLDBPersistence update: MCRObjectID    : " + 
              mcr_id.getId());
            logger.debug("MCRXMLDBPersistence update: MCRLabel       : " +
              mcr_label);
            // open the collection
	    collection = MCRXMLDBConnectionPool.instance().getConnection( mcr_id.getTypeId() );
            // check that the item exist
            XMLResource res = (XMLResource)collection.getResource( mcr_id.getNumberAsString() );
            if (res == null) {
              throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+" does not exist."); }
            // delete the old item
            delete(mcr_id);
            // create the new item
            res = (XMLResource)collection.createResource( mcr_id.getNumberAsString(),
                XMLResource.RESOURCE_TYPE );
            org.jdom.output.DOMOutputter outputter = new org.jdom.output.DOMOutputter();
            org.w3c.dom.Document dom = outputter.output( doc );
            res.setContentAsDOM( dom );
            collection.storeResource( res );
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e ); }
        finally {
            MCRXMLDBConnectionPool.instance().releaseConnection( collection ); }
    }

/**
 * Deletes the object with the given object id in the datastore.
 *
 * @param mcr_id id of the object to delete
 *
 * @throws MCRPersistenceException something goes wrong during delete
 **/     
public void delete( MCRObjectID mcr_id ) 
  throws MCRPersistenceException
  {
  Collection collection = null;
  logger.debug("MCRXMLDBPersistence delete: MCRObjectID    : " + mcr_id.getId());
  try {
    collection = MCRXMLDBConnectionPool.instance().getConnection( mcr_id.getTypeId() );
    Resource document = collection.getResource( mcr_id.getNumberAsString() ); 
    if ( null != document ) {
      collection.removeResource(document); }
    else {
      throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+" does not exist."); }
    }
  catch( Exception e ) {
    throw new MCRPersistenceException( e.getMessage(), e ); }
  finally {
    MCRXMLDBConnectionPool.instance().releaseConnection( collection ); }
  }

/**
 * Retrieves the object with the given ID from the datastore.
 *
 * @param mcr_id object id whose data shall be received
 *
 * @return the content as a JDOM document
 * @throws MCRPersistenceException
 **/
public final byte[] receive( MCRObjectID mcr_id )
  throws MCRConfigurationException 
  {
  logger.debug("MCRXMLDBPersistence receive: "+mcr_id.getId());    
  Collection collection = null;
  org.jdom.Document doc;
  try {
    collection = MCRXMLDBConnectionPool.instance().getConnection( mcr_id.getTypeId() );
    XMLResource res = (XMLResource)collection.getResource( mcr_id.getNumberAsString() );
    if (res == null) {
      throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+
        " does not exist."); }
    doc = convertResToDoc( res );
    }
  catch( Exception e ) {
    throw new MCRPersistenceException( e.getMessage(), e ); }
  finally {
    MCRXMLDBConnectionPool.instance().releaseConnection( collection ); }
  return MCRUtils.getByteArray( doc );
  }

/**
 * A private method to convert the result in a dom tree.
 *
 * @param res the result
 * @exception MCRPersistenceException if an error was occured
 * @return the DOM tree
 **/
public static org.jdom.Document convertResToDoc( XMLResource res )
  {
  try {
    String xml = (String)res.getContent();
    xml = new String( xml.getBytes( store_enc ), def_enc );
    return  builder.build( new StringReader( xml ) );
    }
  catch( Exception e ) {
    throw new MCRPersistenceException( e.getMessage(), e ); }
  }
    
/**
 * This method returns the next free ID number for a given 
 * MCRObjectID base. This method ensures that any invocation
 * returns a new, exclusive ID by remembering the highest ID
 * ever returned and comparing it with the highest ID stored
 * in the related index class.
 * 
 * @param project_ID   the project ID part of the MCRObjectID base
 * @param type_ID      the type ID part of the MCRObjectID base
 *
 * @exception MCRPersistenceException if a persistence problem is occured
 *
 * @return the next free ID number as a String
 **/
public synchronized String getNextFreeId( String project_ID, String type_ID )
  throws MCRPersistenceException { 
  logger.debug("MCRXMLDBPersistence: getNextFreeId");    
  return "";
  }

/**
 * Checks whether an object with the given object id exists in the
 * datastore.
 *
 * @param mcr_id id of the object to check exist
 * @return true if the item with the MCRObjectID exist, else false 
 **/
public boolean exist( MCRObjectID mcr_id )
  {
  logger.debug("MCRXMLDBPersistence exist: "+mcr_id.getTypeId().toLowerCase()+
    " "+mcr_id.getId() );    
  Collection collection = null;
  try {
    collection = MCRXMLDBConnectionPool.instance().getConnection( 
      mcr_id.getTypeId() );
    Resource document = collection.getResource( mcr_id.getNumberAsString() ); 
    MCRXMLDBConnectionPool.instance().releaseConnection( collection );
    if ( null != document ) { return true; }
    }
  catch( Exception e ) { return false; }
  return false;
  }

}
