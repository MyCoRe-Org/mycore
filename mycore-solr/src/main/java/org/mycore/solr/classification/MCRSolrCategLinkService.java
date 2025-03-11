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

package org.mycore.solr.classification;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

/**
 * Solr extension of the category link service. Updates the solr index on set and delete
 * operations.
 *
 * @see MCRSolrCategoryLink
 * @author Matthias Eichner
 */
public class MCRSolrCategLinkService extends MCRCategLinkServiceImpl {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void setLinks(MCRCategLinkReference objectReference, Collection<MCRCategoryID> categories) {
        super.setLinks(objectReference, categories);
        // solr
        List<MCRSolrCore> cores = MCRSolrCoreManager.getCoresForType(MCRSolrCoreType.CLASSIFICATION);
        List<SolrInputDocument> solrDocumentList = MCRSolrClassificationUtil
            .toSolrDocument(objectReference, categories);

        MCRSolrClassificationUtil.bulkIndex(cores, solrDocumentList);
    }

    @Override
    public void deleteLink(MCRCategLinkReference reference) {
        super.deleteLink(reference);
        // solr
        try {
            SolrClient solrClient = MCRSolrClassificationUtil.getCore().getClient();
            delete(solrClient, reference);
        } catch (Exception exc) {
            LOGGER.error(() -> "Unable to delete links of object " + reference.getObjectID(), exc);
        }
    }

    @Override
    public void deleteLinks(Collection<MCRCategLinkReference> references) {
        super.deleteLinks(references);
        // solr
        SolrClient solrClient = MCRSolrClassificationUtil.getCore().getClient();
        for (MCRCategLinkReference reference : references) {
            try {
                delete(solrClient, reference);
            } catch (Exception exc) {
                LOGGER.error(() -> "Unable to delete links of object " + reference.getObjectID(), exc);
            }
        }
    }

    /**
     * Delete the given reference in solr.
     */
    protected void delete(SolrClient solrClient, MCRCategLinkReference reference) throws SolrServerException,
        IOException {
        UpdateRequest req = new UpdateRequest();
        req.deleteByQuery("+type:link +object:" + reference.getObjectID());
        req.setCommitWithin(500);
        MCRSolrAuthenticationManager.getInstance().applyAuthentication(req, MCRSolrAuthenticationLevel.INDEX);
        req.process(solrClient);
    }

}
