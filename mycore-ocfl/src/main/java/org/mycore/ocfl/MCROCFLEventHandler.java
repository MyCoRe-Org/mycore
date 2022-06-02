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

package org.mycore.ocfl;

import static org.mycore.ocfl.MCROCFLPersistenceTransaction.addClassfication;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.datamodel.classifications2.MCRCategory;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLEventHandler implements MCREventHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLEventHandler.class);

    protected MCROCFLXMLClassificationManager manager = MCRConfiguration2
        .getSingleInstanceOf("MCR.Classification.Manager", MCROCFLXMLClassificationManager.class)
        .orElse(new MCROCFLXMLClassificationManager());

    @Override
    public void doHandleEvent(MCREvent evt) throws MCRException {
        if (Objects.equals(evt.getObjectType(), MCREvent.CLASS_TYPE)) {
            MCRCategory mcrCg = (MCRCategory) evt.get("class");
            LOGGER.debug("{} handling {} {}", getClass().getName(), mcrCg.getId(), evt.getEventType());
            switch (evt.getEventType()) {
                case MCREvent.CREATE_EVENT:
                case MCREvent.UPDATE_EVENT:
                        addClassfication(mcrCg.getRoot().getId(), mcrCg.getRoot());
                    break;
                case MCREvent.DELETE_EVENT:
                    if (mcrCg.getId().isRootID()) {
                        // delete complete classification
                        addClassfication(mcrCg.getRoot().getId(), null);
                    } else {
                        // update classification to new version
                        addClassfication(mcrCg.getRoot().getId(), mcrCg.getRoot());
                    }
                    break;
                default:
                    LOGGER.error("No Method available for {}", evt.getEventType());
                    break;
            }
        }
    }

    @Override
    public void undoHandleEvent(MCREvent evt) throws MCRException {
        if (Objects.equals(evt.getObjectType(), MCREvent.CLASS_TYPE)) {
            LOGGER.debug("{} handling undo of {} {}", getClass().getName(), ((MCRCategory) evt.get("class")).getId(),
                evt.getEventType());
            LOGGER.info("Doing nothing for undo of {} {}", ((MCRCategory) evt.get("class")).getId(),
                evt.getEventType());
        }
    }
}
