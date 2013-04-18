/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 18, 2013 $
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

package org.mycore.solr.index.handlers.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.document.MCRSolrInputDocumentFactory;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrMCRContentMapIndexHandler extends MCRSolrAbstractIndexHandler {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrMCRContentMapIndexHandler.class);

    private List<MCRSolrIndexHandler> subhandlers;

    private Map<MCRObjectID, MCRContent> contentMap;

    public MCRSolrMCRContentMapIndexHandler(Map<MCRObjectID, MCRContent> contentMap) {
        this(contentMap, MCRSolrServerFactory.getSolrServer());
    }

    public MCRSolrMCRContentMapIndexHandler(Map<MCRObjectID, MCRContent> contentMap, SolrServer solrServer) {
        super();
        this.contentMap = contentMap;
        this.subhandlers = new ArrayList<>(contentMap.size());
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.documents;
    }

    @Override
    public void index() throws IOException, SolrServerException {
        int totalCount = contentMap.size();
        LOGGER.info("Handling " + totalCount + " documents");
        SolrServer server = getSolrServer();
        if (server instanceof ConcurrentUpdateSolrServer) {
            //split up to speed up processing
            splitup();
            return;
        }
        //multithread processing will result in too many http request
        UpdateResponse updateResponse;
        try {
            Iterator<SolrInputDocument> iter = getSolrInputDocumentIterator();
            if (server instanceof HttpSolrServer) {
                updateResponse = ((HttpSolrServer) server).add(iter);
            } else {
                ArrayList<SolrInputDocument> docs = new ArrayList<>(totalCount);
                while (iter.hasNext()) {
                    docs.add(iter.next());
                }
                updateResponse = server.add(docs);
            }
        } catch (Throwable e) {
            LOGGER.warn("Error while indexing document collection. Split and retry.");
            splitup();
            return;
        }
        if (updateResponse.getStatus() != 0) {
            LOGGER.error("Error while indexing document collection. Split and retry: " + updateResponse.getResponse());
            splitup();
        } else {
            LOGGER.info("Sending " + totalCount + " documents was successful in " + updateResponse.getElapsedTime() + " ms.");
        }

    }

    private Iterator<SolrInputDocument> getSolrInputDocumentIterator() {
        final Iterator<Map.Entry<MCRObjectID, MCRContent>> delegate = contentMap.entrySet().iterator();
        Iterator<SolrInputDocument> iter = new Iterator<SolrInputDocument>() {

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public SolrInputDocument next() {
                Entry<MCRObjectID, MCRContent> entry = delegate.next();
                try {
                    return MCRSolrInputDocumentFactory.getInstance().getDocument(entry.getKey(), entry.getValue());
                } catch (SAXException | IOException e) {
                    throw new MCRException(e);
                }
            }

            @Override
            public void remove() {
                delegate.remove();
            }
        };
        return iter;
    }

    private void splitup() {
        for (Map.Entry<MCRObjectID, MCRContent> entry : contentMap.entrySet()) {
            MCRSolrMCRContentIndexHandler subHandler = new MCRSolrMCRContentIndexHandler(entry.getKey(), entry.getValue(), getSolrServer());
            subHandler.setCommitWithin(getCommitWithin());
            subhandlers.add(subHandler);
        }
        contentMap.clear();
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return subhandlers;
    }

    @Override
    public int getDocuments() {
        return contentMap.size();
    }

}
