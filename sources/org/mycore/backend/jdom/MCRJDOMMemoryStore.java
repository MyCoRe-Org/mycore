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

package org.mycore.backend.jdom;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRNormalizeText;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;

/**
 * This class implements the memory store based on JDOM documents.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRJDOMMemoryStore {
    /** The connection pool singleton */
    private static MCRJDOMMemoryStore singleton;

    /** The logger */
    private static Logger logger = Logger.getLogger("org.mycore.backend.jdom");

    /** A hashtable of the JDOM trees */
    private Hashtable trees = new Hashtable();

    private org.jdom.Document xslorig = null;

    /** The XSL namespace */
    private org.jdom.Namespace ns = null;

    /** Timestamp of the last SQL read and the default reload time in seconds */
    private long tslast = 0;

    private long tsdiff = 0;

    private static final long tsdiffdefault = 3600; // 1

    // hour

    /**
     * Returns the singleton.
     */
    public static synchronized MCRJDOMMemoryStore instance() {
        if (singleton == null) {
            singleton = new MCRJDOMMemoryStore();
        }

        return singleton;
    }

    /**
     * Creates a new JDOM memory store
     */
    private MCRJDOMMemoryStore() {
        // XSL Namespace
        ns = org.jdom.Namespace.getNamespace("xsl", MCRDefaults.XSL_URL);

        // Read stylesheet
        InputStream searchxsl = MCRJDOMMemoryStore.class.getResourceAsStream("/MCRJDOMSearch.xsl");

        if (searchxsl == null) {
            throw new MCRConfigurationException("Can't find stylesheet file MCRJDOMSearch.xsl");
        }

        try {
            xslorig = (new org.jdom.input.SAXBuilder()).build(searchxsl);
        } catch (Exception e) {
            throw new MCRException("Error while parsing file MCRJDOMSearch.xsl.");
        }

        // set the start time and the diff from the config
        MCRConfiguration config = MCRConfiguration.instance();
        tslast = System.currentTimeMillis();
        tsdiff = (config.getLong("MCR.persistence_jdom_reload", tsdiffdefault)) * 1000;
    }

    /**
     * Returns a stylesheet that will execute a query on the JDOM store.
     * 
     * @param query
     *            the XSLT String that represents the search condition
     * @return a org.jdom.Document of the stylesheet
     */
    org.jdom.Document getStylesheet(String query) {
        org.jdom.Document xslfile = (org.jdom.Document) (xslorig.clone());
        xslfile.getRootElement().getChild("template", ns).getChild("result").getChild("choose", ns).getChild("when", ns).setAttribute("test", query);

        // debug
        // org.jdom.output.XMLOutputter outputter = new
        // org.jdom.output.XMLOutputter(org.jdom.output.Format.getPrettyFormat());
        // outputter.output(xslfile, System.out);
        return xslfile;
    }

    /**
     * Returns a list of all object metadata for a given object type
     */
    Hashtable getObjects(String type) {
        // return the JDOM tree if it is in the store
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            throw new MCRPersistenceException("The type is null or empty.");
        }

        // check the reload
        Hashtable store = null;

        if (System.currentTimeMillis() <= (tslast + tsdiff)) {
            store = (Hashtable) trees.get(type);
        }

        if (store != null) {
            return store;
        }

        tslast = System.currentTimeMillis();

        // fill the store form the SQL store of the type
        store = readObjectsFromPersistentStore(type);
        trees.put(type, store);

        return store;
    }

    /**
     * Reads all objects metadata from the persistent store into memory
     */
    private Hashtable readObjectsFromPersistentStore(String type) {
        long startdate = System.currentTimeMillis();
        MCRXMLTableManager mcr_xml = MCRXMLTableManager.instance();
        ArrayList ar = mcr_xml.retrieveAllIDs(type);
        Hashtable objects = new Hashtable();
        int size = ar.size();
        String stid;
        MCRObjectID mid;
        org.jdom.Document jdom_document;

        for (int i = 0; i < size; i++) {
            stid = (String) ar.get(i);
            mid = new MCRObjectID(stid);
            jdom_document = (org.jdom.Document) mcr_xml.readDocument(mid).clone();
            MCRNormalizeText.normalizeJDOM(jdom_document);
            objects.put(mid, jdom_document.detachRootElement());
            logger.debug("Load to JDOM tree " + stid);
        }

        long stopdate = System.currentTimeMillis();
        double diff = (double) (stopdate - startdate) / 1000.0;
        logger.debug("Read " + Integer.toString(ar.size()) + " SQL data sets for type " + type + " in " + diff + " seconds");

        return objects;
    }

    /**
     * Adds an objects xml metadata to the memory store.
     */
    void addElement(MCRObjectID id, org.jdom.Element elm) {
        getObjects(id.getTypeId()).put(id, elm);
        logger.debug("MRJDOMMemoryStore addElement " + id.getTypeId());
        debug(id.getTypeId());
    }

    /**
     * Removes an object from the memory store.
     */
    void removeElement(MCRObjectID id) {
        getObjects(id.getTypeId()).remove(id);
        logger.debug("MRJDOMMemoryStore removeElement " + id.getTypeId());
        debug(id.getTypeId());
    }

    /**
     * Debug the content of the Hashtable.
     */
    void debug(String type) {
        Hashtable h = getObjects(type);

        for (Enumeration e = h.keys(); e.hasMoreElements();) {
            logger.info((String) (e.nextElement().toString()));
        }
    }
}
