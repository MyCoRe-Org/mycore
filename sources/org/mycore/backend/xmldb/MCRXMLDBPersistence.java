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

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.jdom.Document;

import org.jdom.input.*;
import org.jdom.output.*;


import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;

import org.apache.log4j.Logger;

/**
 * This class is the persistence layer for XML:DB databases.
 *
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision$ $Date$
 **/
public final class MCRXMLDBPersistence 
    implements MCRObjectPersistenceInterface {
    static final private Logger logger = Logger.getLogger( MCRXMLDBPersistence.class.getName() );
    static final private MCRConfiguration config = MCRConfiguration.instance();  

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
            SAXOutputter outputter = new SAXOutputter(res.setContentAsSAX());
            outputter.output(doc);
            collection.storeResource( res );
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e ); }
        finally {
            MCRXMLDBConnectionPool.instance().releaseConnection( collection ); }
    }

/**
 * The methode create a new datastore based of given configuration. It create
 * a new data table for storing MCRObjects with the same MCRObjectID type.
 *
 * @param mcr_type    the MCRObjectID type as string
 * @param mcr_conf    the configuration XML stream as JDOM tree
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void createDataBase(String mcr_type, org.jdom.Document mcr_conf)
  throws MCRConfigurationException, MCRPersistenceException
  {
  logger.info("This feature exist not for this store.");
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
     		SAXOutputter outputter = new SAXOutputter(res.setContentAsSAX());
	    	outputter.output(doc);
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
      logger.warn("A object with ID "+mcr_id.getId()+" does not exist."); }
    }
  catch( Exception e ) {
    throw new MCRPersistenceException( e.getMessage(), e ); }
  finally {
    MCRXMLDBConnectionPool.instance().releaseConnection( collection ); }
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
  	SAXHandler handler=new SAXHandler();
  	res.getContentAsSAX(handler);
  	return handler.getDocument();
    }
  catch( Exception e ) {
    throw new MCRPersistenceException( e.getMessage(), e ); }
  }

}
