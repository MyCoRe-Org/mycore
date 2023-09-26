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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRClassificationUpdateType;

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
                case CREATE -> MCRSolrClassificationUtil.reindex(categ, categParent);
                case UPDATE -> processUpdate(evt, categ, categParent);
                case DELETE -> MCRSolrClassificationUtil.solrDelete(categ.getId(), categ.getParent());
                default -> LOGGER.error("No Method available for {}", evt.getEventType());
            }
        }
    }

    private void processUpdate(MCREvent evt, MCRCategory categ, MCRCategory categParent) {
        switch ((MCRClassificationUpdateType) evt.get(MCRClassificationUpdateType.KEY)) {
            case MOVE -> MCRSolrClassificationUtil.solrMove(categ.getId(), categParent.getId());
            case REPLACE -> {
                @SuppressWarnings("unchecked")
                Collection<MCRCategory> replaced = (Collection<MCRCategory>) evt.get("replaced");
                MCRSolrClassificationUtil.solrDelete(categ.getId(), categ.getParent());
                MCRSolrClassificationUtil.reindex(replaced.toArray(MCRCategory[]::new));
            }
            default -> MCRSolrClassificationUtil.reindex(categ, categParent);
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
}
