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

package org.mycore.solr.classification;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.solr.search.MCRSolrSearchUtils;

/**
 * Eventhandler that stores classification and their modifications in a Solr collection
 * 
 * The implementation is based on MCRSolrCategoryDAO and MCREventedCategoryDAO.
 * 
 * @author Robert Stephan
 */
public class MCRSolrClassificationEventHandler implements MCREventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void doHandleEvent(MCREvent evt) throws MCRException {
        if (evt.getObjectType() == MCREvent.ObjectType.CLASS) {
            MCRCategory categ = (MCRCategory) evt.get(MCREvent.CLASS_KEY);
            LOGGER.debug("{} handling {} {}", getClass().getName(), categ.getId(), evt.getEventType());
            MCRCategory categParent = (MCRCategory) evt.get("parent");
            switch (evt.getEventType()) {
                case CREATE:
                    MCRSolrClassificationUtil.reindex(categ, categParent);
                    break;
                case UPDATE:
                    switch ((String) evt.get("type")) {
                        case "move":
                            solrMove(categ.getId(), categParent.getId());
                            break;
                        case "replace":
                            @SuppressWarnings("unchecked")
                            Collection<MCRCategory> replaced = (Collection<MCRCategory>) evt.get("replaced");
                            solrDelete(categ.getId(), categ.getParent());
                            MCRSolrClassificationUtil.reindex(replaced.toArray(new MCRCategory[replaced.size()]));
                            break;
                        default:
                            MCRSolrClassificationUtil.reindex(categ, categParent);
                            break;
                    }
                    break;
                case DELETE:
                    solrDelete(categ.getId(), categ.getParent());
                    break;
                default:
                    LOGGER.error("No Method available for {}", evt.getEventType());
                    break;
            }
        }
    }

    @Override
    public void undoHandleEvent(MCREvent evt) throws MCRException {
        if (evt.getObjectType() == MCREvent.ObjectType.CLASS) {
            LOGGER.debug("{} handling undo of {} {}", getClass().getName(),
                ((MCRCategory) evt.get(MCREvent.CLASS_KEY)).getId(),
                evt.getEventType());
            LOGGER.info("Doing nothing for undo of {} {}", ((MCRCategory) evt.get(MCREvent.CLASS_KEY)).getId(),
                evt.getEventType());
        }
    }

    protected void solrDelete(MCRCategoryID id, MCRCategory parent) {
        try {
            // remove all descendants and itself
            HttpSolrClient solrClient = MCRSolrClassificationUtil.getCore().getClient();
            List<String> toDelete = MCRSolrSearchUtils.listIDs(solrClient,
                "ancestor:" + MCRSolrClassificationUtil.encodeCategoryId(id));
            toDelete.add(id.toString());
            solrClient.deleteById(toDelete);
            // reindex parent
            if (parent != null) {
                MCRSolrClassificationUtil.reindex(parent);
            }
        } catch (Exception exc) {
            LOGGER.error("Solr: unable to delete categories of parent {}", id);
        }
    }

    protected void solrMove(MCRCategoryID id, MCRCategoryID newParentID) {
        try {
            SolrClient solrClient = MCRSolrClassificationUtil.getCore().getClient();
            List<String> reindexList = MCRSolrSearchUtils.listIDs(solrClient,
                "ancestor:" + MCRSolrClassificationUtil.encodeCategoryId(id));
            reindexList.add(id.toString());
            reindexList.add(newParentID.toString());
            MCRSolrClassificationUtil.reindex(MCRSolrClassificationUtil.fromString(reindexList));
        } catch (Exception exc) {
            LOGGER.error("Solr: unable to move categories of category {} to {}", id, newParentID);
        }
    }
}
