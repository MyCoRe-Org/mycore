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
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrClientFactory;
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
public class MCRSolrMCRContentIndexHandler extends MCRSolrAbstractIndexHandler {

    MCRObjectID id;

    MCRContent content;

    private SolrInputDocument document;

    public MCRSolrMCRContentIndexHandler(MCRObjectID id, MCRContent content) {
        this(id, content, MCRSolrClientFactory.getSolrClient());
    }

    public MCRSolrMCRContentIndexHandler(MCRObjectID id, MCRContent content, SolrClient solrClient) {
        super(solrClient);
        this.id = id;
        this.content = content;
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.DOCUMENTS;
    }

    /* (non-Javadoc)
     * @see org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler#index()
     */
    @Override
    public void index() throws IOException, SolrServerException {
        try {
            this.document = MCRSolrInputDocumentFactory.getInstance().getDocument(id, content);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        MCRSolrIndexHandler mcrSolrIndexHandler = new MCRSolrInputDocumentHandler(document, getSolrClient());
        mcrSolrIndexHandler.setCommitWithin(getCommitWithin());
        return Collections.singletonList(mcrSolrIndexHandler);
    }

    @Override
    public int getDocuments() {
        return 0;
    }

    @Override
    public String toString() {
        return "index " + id;
    }

}
