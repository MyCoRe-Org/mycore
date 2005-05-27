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

package org.mycore.datamodel.ifs;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.jdom.*;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * This class manages instances of MCRContentIndexer and provides methods to get
 * these for a given Indexer ID or MCRFile instance. The class is responsible
 * for looking up, loading, instantiating and remembering the implementations of
 * MCRContentIndexer that are used in the system.
 * 
 * @author Harald Richter
 * @version $Revision$ $Date$
 */
public class MCRContentIndexerFactory {
    /** Hashtable IndexerID to MCRContentIndexer instance */
    protected static Hashtable indexers = new Hashtable();

    /** Hashtable IndexerID to attributes of handler */
    protected static Hashtable handlers = new Hashtable();

    static final private Logger logger = Logger
            .getLogger(MCRContentIndexerFactory.class.getName());

    static {
        MCRConfiguration config = MCRConfiguration.instance();

        String file = config.getString(
                "MCR.IFS.FileContentHandler.DefinitionFile",
                "FileContentHandler.xml");

        Element xml = MCRURIResolver.instance().resolve("resource:" + file);

        List types = xml.getChildren("fcttype");
        for (int i = 0; i < types.size(); i++) // handle all fcttypes
        {
            Element xType = (Element) (types.get(i));
            String fcttype = xType.getAttributeValue("type");

            List handler = xType.getChildren("handler");
            for (int j = 0; j < handler.size(); j++) // handle all handlers
            {
                org.jdom.Element xAttribute = (org.jdom.Element) handler.get(j);
                List attribute = xAttribute.getChildren("attribute");
                Hashtable attr = new Hashtable();
                for (int k = 0; k < attribute.size(); k++) // handle all
                                                           // attributes of
                                                           // handler
                {
                    org.jdom.Element xValue = (org.jdom.Element) attribute
                            .get(k);
                    logger.info("FCTTYPE: " + fcttype + " "
                            + xAttribute.getAttributeValue("ID") + " "
                            + xAttribute.getAttributeValue("type") + " "
                            + xValue.getAttributeValue("type") + " "
                            + xValue.getTextTrim());
                    attr.put(xValue.getAttributeValue("type"), xValue
                            .getTextTrim());
                }
                handlers.put(fcttype + "/" + xAttribute.getAttributeValue("ID")
                        + "/", attr);
            }
        }
    }

    /**
     * Returns the MCRContentIndexer instance that is configured for this
     * IndexerID.
     * 
     * @param indexerID
     *            the non-null ID of the MCRContentIndexer implementation
     * @return the MCRContentIndexer instance that uses this indexerID
     */
    public static MCRContentIndexer getIndexer(String indexerID) {
        MCRArgumentChecker.ensureNotEmpty(indexerID, "Indexer ID");
        if (!indexers.containsKey(indexerID)) {
            try {
                Hashtable attribute = (Hashtable) handlers.get(indexerID);
                Class cl = Class.forName((String) attribute.get("class"));
                Object obj = cl.newInstance();
                MCRContentIndexer s = (MCRContentIndexer) (obj);
                s.init(indexerID, attribute);
                indexers.put(indexerID, s);
            } catch (Exception ex) {
                String msg = "Could not load MCRContentIndexer with indexer ID = "
                        + indexerID;
                throw new MCRConfigurationException(msg, ex);
            }
        }
        return (MCRContentIndexer) (indexers.get(indexerID));
    }

    /**
     * Returns the MCRContentIndexer instance that should be used to index the
     * content of the given file.
     * 
     * @see MCRContentIndexer
     */
    public static MCRContentIndexer getIndexerFromFCT(String fct) {
        String indexerID = null;
        for (Enumeration e = handlers.keys(); e.hasMoreElements();) {
            String h = (String) e.nextElement();
            if (h.startsWith(fct))
                indexerID = h;
        }
        if (null != indexerID) {
            logger.info("++++ Indexer found: " + indexerID);
            return getIndexer(indexerID);
        } else
            return null;
    }

}