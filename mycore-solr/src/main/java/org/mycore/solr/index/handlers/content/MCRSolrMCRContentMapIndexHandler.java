/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.solr.index.handlers.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.document.MCRSolrInputDocumentFactory;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;
import org.mycore.solr.index.handlers.document.MCRSolrInputDocumentHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrMCRContentMapIndexHandler extends MCRSolrAbstractIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrMCRContentMapIndexHandler.class);

    private List<MCRSolrIndexHandler> subhandlers;

    private Map<MCRObjectID, MCRContent> contentMap;

    public MCRSolrMCRContentMapIndexHandler(Map<MCRObjectID, MCRContent> contentMap, MCRSolrCoreType type) {
        super();
        this.contentMap = contentMap;
        this.subhandlers = new ArrayList<>(contentMap.size());
        this.setCoreType(type);
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.DOCUMENTS;
    }

    @Override
    public void index() {
        int totalCount = contentMap.size();
        LOGGER.info("Handling {} documents", totalCount);
        //multithread processing will result in too many http request
        UpdateResponse updateResponse = null;
        try {
            Iterator<SolrInputDocument> documents = MCRSolrInputDocumentFactory.getInstance().getDocuments(contentMap);

            for (SolrClient client : getClients()) {
                if (client instanceof ConcurrentUpdateHttp2SolrClient) {
                    //split up to speed up processing
                    makeConcurrent(documents);
                    return;
                }
            }

            if (LOGGER.isDebugEnabled()) {
                ArrayList<SolrInputDocument> debugList = new ArrayList<>();
                while (documents.hasNext()) {
                    debugList.add(documents.next());
                }
                LOGGER.debug("Sending these documents: {}", debugList);
                //recreate documents interator;
                documents = debugList.iterator();
            }
            UpdateRequest req = new UpdateRequest();
            getSolrAuthenticationFactory().applyAuthentication(req, MCRSolrAuthenticationLevel.INDEX);

            ArrayList<SolrInputDocument> docs = new ArrayList<>(totalCount);
            while (documents.hasNext()) {
                docs.add(documents.next());
            }
            req.add(docs);

            for (MCRSolrCore destinationCore : getDestinationCores()) {
                try {
                    updateResponse = req.process(destinationCore.getClient());
                    if (updateResponse != null && updateResponse.getStatus() != 0) {
                        LOGGER.error("Error while indexing document collection. Split and retry: {}",
                            updateResponse.getResponse());
                        splitup(List.of(destinationCore));
                    } else {
                        LOGGER.info("Sending {} documents was successful in {} ms.", totalCount,
                            updateResponse.getElapsedTime());
                    }
                } catch (SolrServerException | IOException e) {
                    LOGGER.warn("Error while indexing document collection. Split and retry.", e);
                    splitup(List.of(destinationCore));
                    return;
                }
            }
        } catch (SAXException | IOException e) {
            splitup(getDestinationCores());
        } finally {
            contentMap.clear();
        }
    }

    private void makeConcurrent(Iterator<SolrInputDocument> documents) {
        while (documents.hasNext()) {
            SolrInputDocument nextDocument = documents.next();
            MCRSolrInputDocumentHandler subhandler = new MCRSolrInputDocumentHandler(() -> nextDocument,
                nextDocument.get("id").toString(), getCoreType());
            subhandler.setCommitWithin(getCommitWithin());
            subhandlers.add(subhandler);
        }
        contentMap.clear();
    }

    private void splitup(List<MCRSolrCore> cores) {
        for (Map.Entry<MCRObjectID, MCRContent> entry : contentMap.entrySet()) {
            MCRSolrMCRContentIndexHandler subHandler = new MCRSolrMCRContentIndexHandler(entry.getKey(),
                entry.getValue(), getCoreType());
            subHandler.setCommitWithin(getCommitWithin());
            subHandler.setDestinationCores(cores);
            subhandlers.add(subHandler);
        }
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return subhandlers;
    }

    @Override
    public int getDocuments() {
        return contentMap.size();
    }

    @Override
    public String toString() {
        return "bulk index " + contentMap.size() + " documents";
    }

}
