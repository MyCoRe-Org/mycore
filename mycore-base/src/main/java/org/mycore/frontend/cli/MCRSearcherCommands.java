/**
 * $RCSfile: MCRLuceneCommands.java,v $ $Revision: 1.0 $ $Date: 22.10.2008
 * 06:48:51 $ This file is part of ** M y C o R e ** Visit our homepage at
 * http://www.mycore.de/ for details. This program is free software; you can use
 * it, redistribute it and / or modify it under the terms of the GNU General
 * Public License (GPL) as published by the Free Software Foundation; either
 * version 2 of the License or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program, normally in the file license.txt. If not, write to the Free Software
 * Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
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
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRFSNODES;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.common.xml.MCRXMLResource;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
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

    private static Logger LOGGER = Logger.getLogger(MCRSearcherCommands.class);

    private static final String SEARCHER_PROPERTY_START = "MCR.Searcher.";

    private static final String SEARCHER_CLASS_SUFFIX = ".Class";

    private static final String SEARCHER_INDEX_SUFFIX = ".Index";

    public MCRSearcherCommands() {
        super();
        command.add(new MCRCommand("rebuild metadata index", "org.mycore.frontend.cli.MCRSearcherCommands.repairMetaIndex", "Repairs metadata index"));
        command.add(new MCRCommand("rebuild content index", "org.mycore.frontend.cli.MCRSearcherCommands.repairContentIndex", "Repairs metadata index"));
    }

    static class RepairIndex {
        private RepairMechanism mechanism;

        public RepairIndex(RepairMechanism mechanism) {
            this.mechanism = mechanism;
        }

        public void repair() {
            try {
                List<MCRSearcher> searcherList = clearAndInitSearchers();
                createNewIndexFor(searcherList);
                closeSearchers(searcherList);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JDOMException e) {
                e.printStackTrace();
            }
        }

        private void closeSearchers(List<MCRSearcher> searcherList) {
            for (MCRSearcher searcher : searcherList) {
                searcher.notifySearcher("finish");
                LOGGER.info("Done building index " + searcher.getID());
            }
        }

        private List<MCRSearcher> clearAndInitSearchers() throws IOException, JDOMException {
            List<MCRSearcher> searcherList = new ArrayList<MCRSearcher>();
            for (String index : getIndexes()) {
                if (isIndexType(index, mechanism.getIndexType())) {
                    MCRSearcher searcher = MCRSearcherFactory.getSearcher(index);
                    LOGGER.info("clearing index " + index);
                    if (searcher.isIndexer()) {
                        searcher.clearIndex();
                        searcherList.add(searcher);
                    }
                }
            }
            return searcherList;
        }

        private void createNewIndexFor(List<MCRSearcher> searcherList) {
            mechanism.repair(searcherList);
        }

        private List<String> getIndexes() {
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

        private boolean isIndexType(String index, String type) throws IOException, JDOMException {
            Document searchFields = MCRXMLResource.instance().getResource("searchfields.xml");
            final String indexKey = MCRConfiguration.instance().getString(SEARCHER_PROPERTY_START + index + SEARCHER_INDEX_SUFFIX);
            for (Object indexElement : searchFields.getRootElement().getChildren("index", MCRConstants.MCR_NAMESPACE)) {
                final Element indexE = (Element) indexElement;
                if (indexE.getAttributeValue("id").equals(indexKey))
                    for (Object fieldElement : indexE.getChildren("field", MCRConstants.MCR_NAMESPACE)) {
                        String source = ((Element) fieldElement).getAttributeValue("source");
                        if (source != null && source.startsWith(type)) {
                            return true;
                        }
                    }
            }
            return false;
        }
    }

    interface RepairMechanism {
        public void repair(List<MCRSearcher> searcherList);

        public String getIndexType();
    }

    static class MetaIndexRepairMechanism implements RepairMechanism {

        @Override
        public void repair(List<MCRSearcher> searcherList) {
            MCRXMLMetadataManager mcrxmlTableManager = MCRXMLMetadataManager.instance();
            for (String id : mcrxmlTableManager.listIDs()) {
                MCRObjectID mcrid = MCRObjectID.getInstance(id);
                addMetaToIndex(mcrid, mcrxmlTableManager.retrieveBLOB(mcrid), false, searcherList);
            }
        }

        private void addMetaToIndex(MCRObjectID id, byte[] xml, boolean update, List<MCRSearcher> searcherList) {
            for (MCRSearcher searcher : searcherList) {
                List<MCRFieldValue> fields = MCRData2Fields.buildFields(xml, searcher.getIndex(), MCRFieldDef.OBJECT_METADATA + MCRFieldDef.OBJECT_CATEGORY,
                    id.getTypeId());
                List<MCRFieldValue> fieldsDerivate = MCRData2Fields.buildFields(xml, searcher.getIndex(), MCRFieldDef.DERIVATE_METADATA, id.getTypeId());
                fields.addAll(fieldsDerivate);
                if (update) {
                    searcher.removeFromIndex(id.toString());
                }
                searcher.addToIndex(id.toString(), id.toString(), fields);
            }
        }

        @Override
        public String getIndexType() {
            return "object";
        }
    }

    static class ContentIndexRepairMechanism implements RepairMechanism {

        @Override
        public void repair(List<MCRSearcher> searcherList) {
            Session session = MCRHIBConnection.instance().getSession();
            Criteria fileCriteria = session.createCriteria(MCRFSNODES.class);
            fileCriteria.add(Restrictions.eq("type", "F"));
            fileCriteria.setCacheMode(CacheMode.IGNORE);
            ScrollableResults results = fileCriteria.scroll(ScrollMode.FORWARD_ONLY);
            try {
                while (results.next()) {
                    MCRFSNODES node = (MCRFSNODES) results.get(0);
                    GregorianCalendar greg = new GregorianCalendar();
                    greg.setTime(node.getDate());
                    MCRFile file = (MCRFile) MCRFileMetadataManager.instance().buildNode(node.getType(), node.getId(), node.getPid(), node.getOwner(),
                        node.getName(), node.getLabel(), node.getSize(), greg, node.getStoreid(), node.getStorageid(), node.getFctid(), node.getMd5(),
                        node.getNumchdd(), node.getNumchdf(), node.getNumchtd(), node.getNumchtf());
                    addFileToIndex(file, false, searcherList);
                    session.evict(node);
                }
            } finally {
                results.close();
            }
        }

        private void addFileToIndex(MCRFile file, boolean update, List<MCRSearcher> searcherList) {
            for (MCRSearcher searcher : searcherList) {
                List<MCRFieldValue> fields = MCRData2Fields.buildFields(file, searcher.getIndex());
                String entryID = file.getID();
                String returnID = searcher.getReturnID(file);
                if (update)
                    searcher.removeFromIndex(entryID);
                searcher.addToIndex(entryID, returnID, fields);
            }
        }

        @Override
        public String getIndexType() {
            return "file";
        }
    }

    /**
     * repairs all metadata indexes
     * 
     * @throws IOException
     * @throws JDOMException
     */
    public static void repairMetaIndex() throws IOException, JDOMException {
        new RepairIndex(new MetaIndexRepairMechanism()).repair();
    }

    /**
     * repairs all content indexes
     * 
     * @throws IOException
     * @throws JDOMException
     */
    public static void repairContentIndex() throws IOException, JDOMException {
        new RepairIndex(new ContentIndexRepairMechanism()).repair();
    }

}
