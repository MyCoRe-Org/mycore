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

package org.mycore.dedup;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Keeps the deduplication keys of an object in sync with its metadata. Whenever an object is created,
 * updated or repaired, its {@link MCRDeDupCriterion}s are (re)computed via the configured
 * {@link MCRDeDupCriteriaProvider} and stored by the {@link MCRDeDupKeyManager}. When an object is
 * deleted, its keys and the no-duplicate markings referencing it are removed.
 * <p>
 * The handler is generic: it stores keys for every object type for which deduplication criterion
 * builders are configured (see {@link MCRDeDupCriteriaProvider}). For object types without configured
 * builders no keys are produced.
 */
public class MCRDeDupEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectCreated(MCREvent event, MCRObject object) {
        updateKeys(object);
    }

    @Override
    protected void handleObjectUpdated(MCREvent event, MCRObject object) {
        updateKeys(object);
    }

    @Override
    protected void handleObjectRepaired(MCREvent event, MCRObject object) {
        updateKeys(object);
    }

    @Override
    protected void handleObjectDeleted(MCREvent event, MCRObject object) {
        LOGGER.info("Removing deduplication keys and no-duplicate markings for object {}", object::getId);
        MCRDeDupKeyManager keyManager = MCRDeDupKeyManager.obtainInstance();
        keyManager.removeKeys(object.getId());
        keyManager.removeNoDuplicates(object.getId());
    }

    private void updateKeys(MCRObject object) {
        Set<MCRDeDupCriterion> criteria = MCRDeDupCriteriaProvider.obtainInstance().getCriteria(object);
        LOGGER.info("Updating {} deduplication key(s) for object {}", criteria::size, object::getId);
        MCRDeDupKeyManager.obtainInstance().storeKeys(object.getId(), criteria);
    }
}
