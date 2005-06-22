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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;

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
    protected static MCRXMLTableManager SINGLETON;

    // logger
    static Logger LOGGER = Logger.getLogger(MCRLinkTableManager.class);

    static MCRConfiguration CONFIG = MCRConfiguration.instance();

    static MCRCache jdomCache;

    // the list of link table
    private String persistclassname = null;

    private Hashtable tablelist;

    /**
     * Returns the link table manager singleton.
     */
    public static synchronized MCRXMLTableManager instance() {
        if (SINGLETON == null)
            SINGLETON = new MCRXMLTableManager();
        return SINGLETON;
    }

    /**
     * The constructor of this class.
     */
    protected MCRXMLTableManager() {
        tablelist = new Hashtable();
        jdomCache = new MCRCache(CONFIG.getInt(
                "MCR.xml.tablemanager.cache.size", 100));
    }

    /**
     * returns a MCRXMLTableInterface handling MyCoRe object of type
     * <code>type</code>
     * 
     * @param type
     *            the table type
     * @exception if
     *                the store for the given type could not find or loaded.
     */
    private MCRXMLTableInterface getXMLTable(String type) {
        if ((type == null) || (type.length() == 0)) {
            throw new MCRException("The type is null or empty.");
        } else if (tablelist.containsKey(type))
            return (MCRXMLTableInterface) tablelist.get(type);
        MCRXMLTableInterface inst = (MCRXMLTableInterface) CONFIG
                .getInstanceOf("MCR.xml_store_class");
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
     * @exception if
     *                the method arguments are not correct
     */
    public void create(MCRObjectID mcrid, org.jdom.Document xml)
            throws MCRException {
        getXMLTable(mcrid.getTypeId()).create(mcrid,
                MCRUtils.getByteArray(xml), 1);
        jdomCache.put(mcrid, xml);
    }

    /**
     * The method create a new item in the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @param xml
     *            a byte array with the XML file
     * 
     * @exception if
     *                the method arguments are not correct
     */
    public void create(MCRObjectID mcrid, byte[] xml) throws MCRException {
        getXMLTable(mcrid.getTypeId()).create(mcrid, xml, 1);
    }

    /**
     * The method remove a item for the MCRObjectID from the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * 
     * @exception if
     *                the method argument is not correct
     */
    public void delete(MCRObjectID mcrid) throws MCRException {
        getXMLTable(mcrid.getTypeId()).delete(mcrid, 1);
        jdomCache.remove(mcrid);
    }

    /**
     * The method update an item in the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @param xml
     *            a byte array with the XML file
     * 
     * @exception if
     *                the method arguments are not correct
     */
    public void update(MCRObjectID mcrid, org.jdom.Document xml)
            throws MCRException {
        getXMLTable(mcrid.getTypeId()).update(mcrid,
                MCRUtils.getByteArray(xml), 1);
        jdomCache.put(mcrid, xml);
    }

    /**
     * The method update an item in the datastore.
     * 
     * @param mcrid
     *            a MCRObjectID
     * @param xml
     *            a byte array with the XML file
     * 
     * @exception if
     *                the method arguments are not correct
     */
    public void update(MCRObjectID mcrid, byte[] xml) throws MCRException {
        getXMLTable(mcrid.getTypeId()).update(mcrid, xml, 1);
        jdomCache.remove(mcrid);
    }

    /**
     * The method retrieve a dataset for the given MCRObjectID and returns the
     * corresponding XML file as byte array.
     * 
     * @param mcrid
     *            a MCRObjectID
     * 
     * @return the byte array of data or NULL
     * @exception if
     *                the method arguments are not correct
     */
    public byte[] retrieve(MCRObjectID mcrid) throws MCRException {
        return getXMLTable(mcrid.getTypeId()).retrieve(mcrid, 1);
    }

    /**
     * This method returns the next free ID number for a given MCRObjectID base.
     * This method ensures that any invocation returns a new, exclusive ID by
     * remembering the highest ID ever returned and comparing it with the
     * highest ID stored in the related index class.
     * 
     * @param project_ID
     *            the project ID part of the MCRObjectID base
     * @param type_ID
     *            the type ID part of the MCRObjectID base
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * 
     * @return the next free ID number as a String
     */
    public int getNextFreeIdInt(String idproject, String idtype)
            throws MCRPersistenceException {
        return getXMLTable(idtype).getNextFreeIdInt(idproject, idtype);
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
        return getXMLTable(mcrid.getTypeId()).exist(mcrid, 1);
    }

    /**
     * The method return a Array list with all stored MCRObjectID's of the XML
     * table of a MCRObjectID type.
     * 
     * @param type
     *            a MCRObjectID type string
     * @return a ArrayList of MCRObjectID's
     */
    public ArrayList retrieveAllIDs(String type) {
        return getXMLTable(type).retrieveAllIDs(type);
    }

    /**
     * returns the JDOM-Document of the given MCRObjectID. This method uses
     * caches to save database connections. Use this if you want to get a JDOM
     * Document not just plain xml.
     * 
     * @param id
     *            ObjectID of MyCoRe Document
     * @return MyCoRe Document as JDOM or NULL
     */
    public Document readDocument(MCRObjectID id) {
        Document jDoc = (Document) jdomCache.get(id); // use object if in cache
        if (jDoc == null) {
            byte[] xml = retrieve(id);
            if (xml == null || xml.length == 0) {
                LOGGER
                        .error("Error while retrieving XML from MCRXMLTableInterface");
                return null;
            }
            jDoc = MCRXMLHelper.parseXML(xml, false); //read from MCRXMLTable
            jdomCache.put(id, jDoc);
            LOGGER.debug(new StringBuffer(id.toString())
                    .append(" is now in MCRCache..."));
        } else {
            LOGGER.debug(new StringBuffer("read ").append(id).append(
                    " from MCRCache..."));
        }
        return (Document)jDoc.clone();
    }

}

