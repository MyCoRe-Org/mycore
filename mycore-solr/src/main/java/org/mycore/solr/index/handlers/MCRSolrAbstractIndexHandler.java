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

package org.mycore.solr.index.handlers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.index.MCRSolrIndexHandler;

public abstract class MCRSolrAbstractIndexHandler implements MCRSolrIndexHandler {

    protected SolrClient solrClient;

    protected int commitWithin;

    public MCRSolrAbstractIndexHandler() {
        this(null);
    }

    public MCRSolrAbstractIndexHandler(SolrClient solrClient) {
        this.solrClient = solrClient != null ? solrClient : MCRSolrClientFactory.getSolrClient();
        this.commitWithin = -1;
    }

    public SolrClient getSolrClient() {
        return this.solrClient;
    }

    public abstract void index() throws IOException, SolrServerException;

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return Collections.emptyList();
    }

    /**
     * Time in milliseconds solr should index the stream. -1 by default,
     * says that solr decide when to commit.
     */
    public void setCommitWithin(int commitWithin) {
        this.commitWithin = commitWithin;
    }

    public int getCommitWithin() {
        return commitWithin;
    }

    @Override
    public void setSolrServer(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    @Override
    public int getDocuments() {
        return 1;
    }

    protected UpdateRequest getUpdateRequest(String path) {
        UpdateRequest req = path != null ? new UpdateRequest(path) : new UpdateRequest();
        req.setCommitWithin(getCommitWithin());
        return req;
    }

}
