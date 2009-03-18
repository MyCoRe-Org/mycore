/**
 * $RCSfile: MCRLuceneCommands.java,v $
 * $Revision: 1.0 $ $Date: 22.10.2008 06:48:51 $
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRFSNODES;
import org.mycore.backend.hibernate.tables.MCRXMLTABLE;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.common.xml.MCRXMLResource;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileMetadataManager;
import org.mycore.services.fieldquery.MCRData2Fields;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSearcherFactory;

/**
 * provides static methods to manipulate MCRSearcher indexes.
 * 
 * @author Thomas Scheffler (yagee)
 */

public class MCRSearcherCommands extends MCRAbstractCommands {
    private static final String INDEX_TYPE_CONTENT = "file";

    private static final String INDEX_TYPE_METADATA = "object";

    private static Logger LOGGER = Logger.getLogger(MCRSearcherCommands.class);

    private static final String SEARCHER_PROPERTY_START = "MCR.Searcher.";

    private static final String SEARCHER_CLASS_SUFFIX = ".Class";

    private static final String SEARCHER_INDEX_SUFFIX = ".Index";

    public MCRSearcherCommands() {
        super();
        command.add(new MCRCommand("rebuild metadata index", "org.mycore.frontend.cli.MCRSearcherCommands.repairMetaIndex", "Repairs metadata index"));
        command.add(new MCRCommand("rebuild content index", "org.mycore.frontend.cli.MCRSearcherCommands.repairContentIndex", "Repairs metadata index"));
    }

    /**
     * repairs all metadata indexes
     * 
     * @throws IOException
     * @throws JDOMException
     */
    public static void repairMetaIndex() throws IOException, JDOMException {
        List<String> indexes = getIndexes();
        List<String> metaSearcher = new ArrayList<String>(1);
        for (String index : indexes) {
            if (isIndexType(index, INDEX_TYPE_METADATA))
                metaSearcher.add(index);
        }
        for (String searcherID : metaSearcher) {
            MCRSearcher searcher = MCRSearcherFactory.getSearcher(searcherID);
            LOGGER.info("clearing index " + searcherID);
            searcher.clearIndex();
            searcher.notifySearcher("insert");
            Session session = MCRHIBConnection.instance().getSession();
            Criteria xmlCriteria = session.createCriteria(MCRXMLTABLE.class);
            xmlCriteria.setCacheMode(CacheMode.IGNORE);
            ScrollableResults results = xmlCriteria.scroll(ScrollMode.FORWARD_ONLY);
            while (results.next()) {
                MCRXMLTABLE xmlEntry = (MCRXMLTABLE) results.get(0);
                if (xmlEntry.getType().equals("derivate"))
                    continue;
                addMetaToIndex(xmlEntry, false, searcher);
                session.evict(xmlEntry);
            }
            searcher.notifySearcher("finish");
            LOGGER.info("Done building index " + searcherID);
        }
    }

    /**
     * repairs all content indexes
     * 
     * @throws IOException
     * @throws JDOMException
     */
    public static void repairContentIndex() throws IOException, JDOMException {
        List<String> indexes = getIndexes();
        List<String> contentSearcher = new ArrayList<String>(1);
        for (String index : indexes) {
            if (isIndexType(index, INDEX_TYPE_CONTENT))
                contentSearcher.add(index);
        }
        for (String searcherID : contentSearcher) {
            MCRSearcher searcher = MCRSearcherFactory.getSearcher(searcherID);
            LOGGER.info("clearing index " + searcherID);
            searcher.clearIndex();
            searcher.notifySearcher("insert");
            Session session = MCRHIBConnection.instance().getSession();
            Criteria fileCriteria = session.createCriteria(MCRFSNODES.class);
            fileCriteria.add(Restrictions.eq("type", "F"));
            fileCriteria.setCacheMode(CacheMode.IGNORE);
            ScrollableResults results = fileCriteria.scroll(ScrollMode.FORWARD_ONLY);
            while (results.next()) {
                MCRFSNODES node = (MCRFSNODES) results.get(0);
                GregorianCalendar greg = new GregorianCalendar();
                greg.setTime(node.getDate());
                MCRFile file = (MCRFile) MCRFileMetadataManager.instance().buildNode(node.getType(), node.getId(), node.getPid(), node.getOwner(),
                        node.getName(), node.getLabel(), node.getSize(), greg, node.getStoreid(), node.getStorageid(), node.getFctid(), node.getMd5(),
                        node.getNumchdd(), node.getNumchdf(), node.getNumchtd(), node.getNumchtf());
                addFileToIndex(file, false, searcher);
                session.evict(node);
            }
            searcher.notifySearcher("finish");
            LOGGER.info("Done building index " + searcherID);
        }
    }

    private static List<String> getIndexes() {
        Properties searcherProps = MCRConfiguration.instance().getProperties(SEARCHER_PROPERTY_START);
        List<String> luceneIndexes = new ArrayList<String>(2);
        for (Entry<Object, Object> property : searcherProps.entrySet()) {
            if (property.getKey().toString().endsWith(SEARCHER_CLASS_SUFFIX)) {
                luceneIndexes.add(property.getKey().toString().split("\\.")[2]);
            }
        }
        LOGGER.info("Found MCRSearcher indexes: " + luceneIndexes);
        return luceneIndexes;
    }

    private static boolean isIndexType(String index, String type) throws IOException, JDOMException {
        Document searchFields = MCRXMLResource.instance().getResource("searchfields.xml");
        final String indexKey = MCRConfiguration.instance().getString(SEARCHER_PROPERTY_START + index + SEARCHER_INDEX_SUFFIX);
        for (Object indexElement : searchFields.getRootElement().getChildren("index", MCRConstants.MCR_NAMESPACE)) {
            final Element indexE = (Element) indexElement;
            if (indexE.getAttributeValue("id").equals(indexKey))
                for (Object fieldElement : indexE.getChildren("field", MCRConstants.MCR_NAMESPACE)) {
                    String source = ((Element) fieldElement).getAttributeValue("source");
                    if (source.startsWith(type)) {
                        return true;
                    }
                }
        }
        return false;
    }

    private static void addMetaToIndex(MCRXMLTABLE xmlEntry, boolean update, MCRSearcher searcher) {
        List<MCRFieldValue> fields = MCRData2Fields.buildFields(xmlEntry.getXmlByteArray(), searcher.getIndex(), MCRFieldDef.OBJECT_METADATA
                + MCRFieldDef.OBJECT_CATEGORY, xmlEntry.getType());
        if (update)
            searcher.removeFromIndex(xmlEntry.getId());
        searcher.addToIndex(xmlEntry.getId(), xmlEntry.getId(), fields);
    }

    private static void addFileToIndex(MCRFile file, boolean update, MCRSearcher searcher) {
        List<MCRFieldValue> fields = MCRData2Fields.buildFields(file, searcher.getIndex());
        String entryID = file.getID();
        String returnID = searcher.getReturnID(file);
        if (update)
            searcher.removeFromIndex(entryID);
        searcher.addToIndex(entryID, returnID, fields);
    }
}
