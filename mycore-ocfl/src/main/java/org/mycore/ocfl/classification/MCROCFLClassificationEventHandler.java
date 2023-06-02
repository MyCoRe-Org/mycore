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

package org.mycore.ocfl.classification;

import static org.mycore.ocfl.MCROCFLPersistenceTransaction.addClassficationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLClassificationEventHandler implements MCREventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void doHandleEvent(MCREvent evt) throws MCRException {
        if (evt.getObjectType() == MCREvent.ObjectType.CLASS) {
            MCRCategory mcrCg = (MCRCategory) evt.get(MCREvent.CLASS_KEY);
            LOGGER.debug("{} handling {} {}", getClass().getName(), mcrCg.getId(), evt.getEventType());
            switch (evt.getEventType()) {
                case CREATE -> addClassficationEvent(mcrCg.getRoot().getId(), MCRAbstractMetadataVersion.CREATED);
                case UPDATE -> addClassficationEvent(mcrCg.getRoot().getId(), MCRAbstractMetadataVersion.UPDATED);
                case DELETE -> {
                    if (mcrCg.getId().isRootID()) {
                        // delete complete classification
                        addClassficationEvent(mcrCg.getRoot().getId(), MCRAbstractMetadataVersion.DELETED);
                    } else {
                        // update classification to new version
                        addClassficationEvent(mcrCg.getRoot().getId(), MCRAbstractMetadataVersion.UPDATED);
                    }
                }
                default -> LOGGER.error("No Method available for {}", evt.getEventType());
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
}
