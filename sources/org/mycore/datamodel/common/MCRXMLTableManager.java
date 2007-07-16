/*
 * $RCSfile$
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jdom.Document;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class manage all accesses to the XML table database. This database holds
 * all informations about the MCRObjectID and the corresponding XML file.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRXMLTableManager {
    /** The link table manager singleton */
    private static MCRXMLTableManager SINGLETON;

    // logger
    static Logger LOGGER = Logger.getLogger(MCRLinkTableManager.class);

    static MCRConfiguration CONFIG = MCRConfiguration.instance();

    static MCRCache jdomCache;

    private Hashtable<String, MCRXMLTableInterface> tablelist;

    private static int number_distance = 1;

    /**
     * Returns the link table manager singleton.
     */
    public static synchronized MCRXMLTableManager instance() {
        if (SINGLETON == null) {
            SINGLETON = new MCRXMLTableManager();
        }

        return SINGLETON;
    }

    /**
     * The constructor of this class.
     */
    protected MCRXMLTableManager() {
        tablelist = new Hashtable<String, MCRXMLTableInterface>();
        jdomCache = new MCRCache(CONFIG.getInt("MCR.Persistence.XML.Store.CacheSize", 100), "XMLTable JDOMs");
        number_distance = CONFIG.getInt("MCR.Metadata.ObjectID.NumberDistance", 1);
    }

    /**
     * returns a MCRXMLTableInterface handling MyCoRe object of type
     * <code>type</code>
     * 
     * @param type
     *            the table type
     */
    private MCRXMLTableInterface getXMLTable(String type) {
        if ((type == null) || (type.length() == 0)) {
            throw new MCRException("The type is null or empty.");
        } else if (tablelist.containsKey(type)) {
            return tablelist.get(type);
        }

        MCRXMLTableInterface inst = (MCRXMLTableInterface) CONFIG.getInstanceOf("MCR.Persistence.XML.Store.Class");
        inst.init(type);
        tablelist.put(type, inst);

        return inst;
    }

    /**
     * The method create a new item in the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @param xml
     *            a JDOM Document
     * 
     * @exception MCRException
     *                if the method arguments are not correct
     */
    public void create(MCRObjectID mcrid, org.jdom.Document xml) throws MCRException {
        getXMLTable(mcrid.getTypeId()).create(mcrid.getId(), MCRUtils.getByteArray(xml), 1);
        jdomCache.put(mcrid, xml);
        CONFIG.systemModified();
    }

    /**
     * The method create a new item in the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @param xml
     *            a byte array with the XML file
     * 
     * @exception MCRException
     *                if the method arguments are not correct
     */
    public void create(MCRObjectID mcrid, byte[] xml) throws MCRException {
        getXMLTable(mcrid.getTypeId()).create(mcrid.getId(), xml, 1);
        CONFIG.systemModified();
    }

    /**
     * The method remove a item for the MCRObjectID from the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * 
     * @exception MCRException
     *                if the method argument is not correct
     */
    public void delete(MCRObjectID mcrid) throws MCRException {
        getXMLTable(mcrid.getTypeId()).delete(mcrid.getId(), 1);
        jdomCache.remove(mcrid);
        CONFIG.systemModified();
    }

    /**
     * The method update an item in the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @param xml
     *            a byte array with the XML file
     * 
     * @exception MCRException
     *                if the method arguments are not correct
     */
    public void update(MCRObjectID mcrid, org.jdom.Document xml) throws MCRException {
        getXMLTable(mcrid.getTypeId()).update(mcrid.getId(), MCRUtils.getByteArray(xml), 1);
        jdomCache.put(mcrid, xml);
        CONFIG.systemModified();
    }

    /**
     * The method update an item in the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @param xml
     *            a byte array with the XML file
     * 
     * @exception MCRException
     *                if the method arguments are not correct
     */
    public void update(MCRObjectID mcrid, byte[] xml) throws MCRException {
        getXMLTable(mcrid.getTypeId()).update(mcrid.getId(), xml, 1);
        jdomCache.remove(mcrid);
        CONFIG.systemModified();
    }

    /**
     * The method retrieve a dataset for the given MCRObjectID and returns the
     * corresponding XML file as byte array.
     * 
     * @param mcrid
     *            a MCRObjectID
     * 
     * @return the byte array of data or NULL
     * @exception MCRException
     *                if the method arguments are not correct
     */
    public byte[] retrieveAsXML(MCRObjectID mcrid) throws MCRException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MCRUtils.copyStream(getXMLTable(mcrid.getTypeId()).retrieve(mcrid.getId(), 1), bos);
        return bos.toByteArray();
    }

    /**
     * The method retrieve a dataset for the given MCRObjectID and returns the
     * corresponding JDOM Document.
     * 
     * @param mcrid
     *            a MCRObjectID
     * 
     * @return the JDOM Document of data or NULL
     * @exception MCRException
     *                if the method arguments are not correct
     */
    public Document retrieveAsJDOM(MCRObjectID mcrid) throws MCRException {
        InputStream xml = getXMLTable(mcrid.getTypeId()).retrieve(mcrid.getId(), 1);
        return MCRXMLHelper.getParser().parseXML(xml, false);
    }

    /**
     * This method returns the next free ID number for a given MCRObjectID base.
     * This method ensures that any invocation returns a new, exclusive ID by
     * remembering the highest ID ever returned and comparing it with the
     * highest ID stored in the related index class.
     * 
     * @param idproject
     *            the project ID part of the MCRObjectID base
     * @param idtype
     *            the type ID part of the MCRObjectID base
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * 
     * @return the next free ID number as a String
     */
    public int getNextFreeIdInt(String idproject, String idtype) throws MCRPersistenceException {
        int i = getXMLTable(idtype).getNextFreeIdInt(idproject, idtype);

        while ((i % number_distance) != 0) {
            i += 1;
        }

        return i;
    }

    /**
     * This method check that the MCRObjectID exist in this store.
     * 
     * @param mcrid
     *            a MCRObjectID
     * 
     * @return true if the MCRObjectID exist, else return false
     */
    public boolean exist(MCRObjectID mcrid) {
        return getXMLTable(mcrid.getTypeId()).exist(mcrid.getId(), 1);
    }

    /**
     * The method return a Array list with all stored MCRObjectID's of the XML
     * table of a MCRObjectID type.
     * 
     * @param type
     *            a MCRObjectID type string
     * @return a ArrayList of MCRObjectID's
     */
    public List<String> retrieveAllIDs(String type) {
        return getXMLTable(type).retrieveAllIDs(type);
    }

    /**
     * The method return a Array list with all stored MCRObjectID's of the XML
     * table
     * 
     * @return a ArrayList of MCRObjectID's
     */
    public List<String> retrieveAllIDs() {
        ArrayList<String> a = new ArrayList<String>();
        for (String type : getAllAllowedMCRObjectIDTypes()) {
            a.addAll(retrieveAllIDs(type));
        }
        Collections.sort(a);
        return a;
    }

    /**
     * The method return a Array list with all MCRObjectID-Types, stored in the
     * XML table. reads the mycore.properties-configuration for datamodel-types
     * 
     * @return a ArrayList of MCRObjectID-Types for which
     *         MCR.Metadata.Type.{datamodel}=true
     */
    public List<String> getAllAllowedMCRObjectIDTypes() {
        ArrayList<String> listTypes = new ArrayList<String>();
        final String prefix = "MCR.Metadata.Type.";
        Properties prop = MCRConfiguration.instance().getProperties(prefix);
        Enumeration names = prop.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) (names.nextElement());
            if (MCRConfiguration.instance().getBoolean(name)) {
                listTypes.add(name.substring(prefix.length()));
            }
        }
        return listTypes;
    }

    /**
     * returns the JDOM-Document of the given MCRObjectID. This method uses
     * caches to save database connections. Use this if you want to get a JDOM
     * Document not just plain xml. Be aware that any changes done to the
     * Document will be applied to the copy in the cache. So if you made any
     * changes to the Document make a clone of the Document to avoid side
     * effects.
     * 
     * @param id
     *            ObjectID of MyCoRe Document
     * @return MyCoRe Document as JDOM or NULL
     */
    public Document readDocument(MCRObjectID id) {
        // use object if in cache
        Document jDoc = (Document) jdomCache.get(id);

        if (jDoc == null) {
            jDoc = retrieveAsJDOM(id);
            jdomCache.put(id, jDoc);
            LOGGER.debug(new StringBuffer(id.toString()).append(" is now in MCRCache..."));
        } else {
            LOGGER.debug(new StringBuffer("read ").append(id).append(" from MCRCache..."));
        }

        return jDoc;
    }
}
