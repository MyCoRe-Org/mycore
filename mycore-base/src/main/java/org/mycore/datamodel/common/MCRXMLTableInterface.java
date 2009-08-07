/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.common;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.mycore.common.MCRPersistenceException;

/**
 * This interface is designed to choose the Persistence for the XML tables.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public interface MCRXMLTableInterface {
    /**
     * The initializer for the class MCRSQLXMLStore. It reads the configuration
     * and checks the table names and create the table if they does'n exist..
     * 
     * @param type
     *            the type String of the MCRObjectID
     * @exception MCRPersistenceException
     *                throws if the type is not correct
     */
    public void init(String type) throws MCRPersistenceException;

    /**
     * The method create a new item in the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @param xml
     *            a byte array with the XML file
     * @exception MCRPersistenceException
     *                if the method arguments are not correct
     */
    public void create(String mcrid, byte[] xml, Date lastModified) throws MCRPersistenceException;

    /**
     * The method remove a item for the MCRObjectID from the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @exception MCRPersistenceException
     *                if the method argument is not correct
     */
    public void delete(String mcrid) throws MCRPersistenceException;

    /**
     * The method update an item in the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @param xml
     *            a byte array with the XML file
     * @exception MCRPersistenceException
     *                if the method arguments are not correct
     */
    public void update(String mcrid, byte[] xml, Date lastModified) throws MCRPersistenceException;

    /**
     * The method retrieve a dataset for the given MCRObjectID and returns the
     * corresponding XML file as InputStream.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @exception MCRPersistenceException
     *                if the method arguments are not correct
     */
    public InputStream retrieve(String mcrid) throws MCRPersistenceException;

    /**
     * This method returns the highest stored ID number, 
     * or 0 if no object is stored for this type and project.
     * 
     * @param project
     *            the project ID part of the MCRObjectID base
     * @param type
     *            the type ID part of the MCRObjectID base
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * 
     * @return the highest stored ID number as a String
     */
    public abstract int getHighestStoredID(String project, String type) throws MCRPersistenceException;

    /**
     * This method check that the MCRObjectID exist in this store.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @return true if the MCRObjectID exist, else return false
     */
    public boolean exists(String mcrid);

    /**
     * The method return a Array list with all stored MCRObjectID's of the XML
     * table.
     * 
     * @return a ArrayList of MCRObjectID's
     */
    public List<String> retrieveAllIDs();
    
    /**
     * lists objects of the specified <code>type</code> and their last modified date. 
     * @param type type of object
     * @return
     */
    public List<MCRObjectIDDate> listObjectDates(String type);
}
