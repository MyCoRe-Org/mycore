/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.index.MCRSolrIndexHandler;

public abstract class MCRSolrAbstractIndexHandler implements MCRSolrIndexHandler {

    protected final MCRSolrAuthenticationManager solrAuthenticationFactory;

    private List<MCRSolrCore> destinationCores;

    protected int commitWithin;

    private MCRSolrCoreType coreType;

    public MCRSolrAbstractIndexHandler() {
        this.commitWithin = MCRConfiguration2.getInt("MCR.Solr.commitWithIn").orElseThrow();
        this.solrAuthenticationFactory = MCRSolrAuthenticationManager.getInstance();
    }

    @Override
    public abstract void index() throws IOException, SolrServerException;

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return Collections.emptyList();
    }

    /**
     * Time in milliseconds solr should index the stream. -1 by default,
     * says that solr decide when to commit.
     */
    @Override
    public void setCommitWithin(int commitWithin) {
        this.commitWithin = commitWithin;
    }

    @Override
    public int getCommitWithin() {
        return commitWithin;
    }

    public List<MCRSolrCore> getDestinationCores() {
        if (destinationCores != null) {
            return destinationCores;
        } else {
            return MCRSolrCoreManager.getCoresForType(this.coreType);
        }
    }

    public void setDestinationCores(List<MCRSolrCore> destinationCores) {
        this.destinationCores = destinationCores;
    }

    /**
     * Returns all solr clients for the destination cores.
     * @return set of solr clients
     */
    protected List<SolrClient> getClients() {
        return getDestinationCores().stream().map(MCRSolrCore::getClient).collect(Collectors.toList());
    }

    @Override
    public int getDocuments() {
        return 1;
    }

    protected UpdateRequest getUpdateRequest(String path) {
        UpdateRequest req = path != null ? new UpdateRequest(path) : new UpdateRequest();
        MCRSolrAuthenticationManager.getInstance().applyAuthentication(req, MCRSolrAuthenticationLevel.INDEX);
        req.setCommitWithin(getCommitWithin());
        return req;
    }

    public MCRSolrAuthenticationManager getSolrAuthenticationFactory() {
        return solrAuthenticationFactory;
    }

    @Override
    public void setCoreType(MCRSolrCoreType coreType) {
        this.coreType = coreType;
    }

    public MCRSolrCoreType getCoreType() {
        return coreType;
    }
}
