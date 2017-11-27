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

package org.mycore.sword.application;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrClientFactory;
import org.swordapp.server.SwordServerException;

/**
 * Supplies the Sword with results from solr. This is the Suggested method to use.
 */
public class MCRSwordSolrObjectIDSupplier extends MCRSwordObjectIDSupplier {

    private SolrQuery solrQuery;

    /**
     * @param solrQuery the query which will be used to ask solr. (start and rows will be overwritten for pagination purposes)
     */
    public MCRSwordSolrObjectIDSupplier(SolrQuery solrQuery) {
        this.solrQuery = solrQuery;
    }

    @Override
    public long getCount() throws SwordServerException {
        try {
            // make a copy to prevent multi threading issues
            final SolrQuery queryCopy = this.solrQuery.getCopy();

            // only need the numFound
            queryCopy.setStart(0);
            queryCopy.setRows(0);
            final QueryResponse queryResponse = MCRSolrClientFactory.getSolrClient().query(queryCopy);

            return queryResponse.getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            throw new SwordServerException(
                "Error while getting count with MCRSword2SolrObjectIDSupplier and Query: " + this.solrQuery,
                e);
        }
    }

    @Override
    public List<MCRObjectID> get(int from, int count) throws SwordServerException {
        final SolrQuery queryCopy = this.solrQuery.getCopy();
        queryCopy.setStart(from);
        queryCopy.setRows(count);
        try {
            final QueryResponse queryResponse = MCRSolrClientFactory.getSolrClient().query(queryCopy);
            return queryResponse.getResults().stream()
                .map(r -> (String) r.getFieldValue("id"))
                .map(MCRObjectID::getInstance)
                .collect(Collectors.toList());
        } catch (SolrServerException | IOException e) {
            throw new SwordServerException("Error while getting id list with MCRSword2SolrObjectIDSupplier and Query: "
                + this.solrQuery, e);
        }
    }
}
