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
import org.xmldb.api.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.w3c.dom.*;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

/**
 * This class implements a storage item for xml:db databases. 
 * 
 * @author marc schluepmann
 * @version $Revision$ $Date$
 **/
public final class MCRXMLDBItem {
    private Collection collection;
    private MCRObjectID objectID;
    private org.jdom.Document doc;
    private MCRConfiguration config = MCRConfiguration.instance();
    private String def_enc = config.getString( "MCR.metadata_default_encoding" );
    private String store_enc = config.getString( "MCR.persistence_xmldb_encoding" );
    private SAXBuilder builder = new SAXBuilder();

    /**
     * Creates a MCRXMLDBItem that is stored in the given collection,
     * uses the given MCRObjectID, and contains the given xml data. 
     * 
     * @param collection the collection that holds the XMLResource
     * @param id the associated MCRObjectID object
     * @param doc the JDOM Document
     *
     * @throws XMLDBException a XMLDBException
     **/
    public MCRXMLDBItem( Collection collection, 
			 MCRObjectID id, 
			 org.jdom.Document doc ) 
	throws XMLDBException, JDOMException {
	if( doc != null ) {
	    this.doc = doc;
	}
	objectID = id;
	this.collection = collection;
    }

    /**
     * Creates a MCRXMLDBItem from a given XMLResource.
     *
     * @param resource a XMLResource source
     * 
     * @throws XMLDBException a XMLDBException
     * @throws UnsupportedEncodingException
     * @throws JDOMException
     **/
    public MCRXMLDBItem( XMLResource resource ) 
	throws XMLDBException, 
	       UnsupportedEncodingException,
	       JDOMException {
	collection = resource.getParentCollection();
	String xml = (String)resource.getContent();
	xml = new String( xml.getBytes( store_enc ), def_enc );
	//	System.out.println( xml );
	doc = builder.build( new StringReader( xml ) );
	objectID = new MCRObjectID( doc.getRootElement().getAttribute( "ID" ).getValue() );
    }

    /**
     * This method creates the item in the datastore.
     *
     * @throws XMLDBException a XMLDBException
     * @throws UnsupportedEncodingException thrown if an uncorrect 
     * encoding is to be used. Should never occur.
     **/
    public final void create() 
	throws XMLDBException, 
	       UnsupportedEncodingException {
	XMLResource res =
	    (XMLResource)collection.createResource( objectID.getNumberAsString(),
						    XMLResource.RESOURCE_TYPE );
	XMLOutputter xmlouter = new XMLOutputter();
	xmlouter.setEncoding( store_enc );
	res.setContent( xmlouter.outputString( doc ) );
	collection.storeResource( res );
    }
    
    /**
     * This methode retrievs the item from the datastore.
     *
     * @throws XMLDBException Exceptions of XML:DB API
     **/
    public void retrieve() 
	throws XMLDBException, 
	       UnsupportedEncodingException, 
	       JDOMException {

// Commented things are old and lame!!
// 	String[] resIDs = collection.listResources();
// 	for( int i = 0; i < resIDs.length; i++ ) {
// 	    XMLResource res = (XMLResource)collection.getResource( resIDs[i] );

	XMLResource res = (XMLResource)collection.getResource( objectID.getNumberAsString() );
	String xml = (String)res.getContent();
	xml = new String( xml.getBytes( store_enc ), def_enc );
	doc = builder.build( new StringReader( xml ) );

// 	    if( objectID.getId().equals( doc.getRootElement().getAttribute( "ID" ).getValue() ) ) {
// 		break;
// 	    }
// 	}
    }

    /**
     * This methode updates the item in the datastore.
     *
     * @throws XMLDBException a XMLDBException
     * @throws UnsupportedEncodingException thrown if an uncorrect 
     * encoding is to be used. Should never occur.
     **/
    public final void update()
	throws XMLDBException, 
	       UnsupportedEncodingException, 
	       JDOMException {
	XMLResource res = (XMLResource)collection.getResource( objectID.getNumberAsString() );
	XMLOutputter xmlouter = new XMLOutputter();
	xmlouter.setEncoding( store_enc );
	res.setContent( xmlouter.outputString( doc ) );
	collection.storeResource( res );
    }

    /**
     * This methode updates the item in the datastore with an option.
     *
     * @param option The update option
     *
     * @throws XMLDBException  a XMLDBException
     * @throws UnsupportedEncodingException thrown if an uncorrect 
     * encoding is to be used. Should never occur.
     **/
    public final void update( short option ) 
  	throws XMLDBException, UnsupportedEncodingException, JDOMException { 
	delete();
	create();
    }
    
    /**
     * This methode deletes the item from the datastore.
     *
     * @throws XMLDBException a XMLDBException
     **/
    public final void delete() 
	throws XMLDBException,
	       UnsupportedEncodingException,
	       JDOMException {
	CollectionManagementService cms;

	// Get all resources in the collection
	String[] resIDs = collection.listResources();
	// Looking for the correct XMLResource
	for( int i = 0; i < resIDs.length; i++ ) {
	    XMLResource res = (XMLResource)collection.getResource( resIDs[i] );
	    String xml = (String)res.getContent();
	    xml = new String( xml.getBytes( store_enc ), def_enc );
	    doc = builder.build( new StringReader( xml ) );
	    if( objectID.getId().equals( doc.getRootElement().getAttribute( "ID" ).getValue() ) ) {
		collection.removeResource( res );
		break;
	    }
	}
    }

    /**
     * This method returns the content as a JDOM tree.
     *
     * @return a JDOM tree
     **/
    public org.jdom.Document getContent() {
	return doc;
    }

    /**
     * This method returns the MCRObjectID of this item.
     *
     * @return the MCRObjectID
     **/
    public MCRObjectID getId() {
	return objectID;
    }
}
