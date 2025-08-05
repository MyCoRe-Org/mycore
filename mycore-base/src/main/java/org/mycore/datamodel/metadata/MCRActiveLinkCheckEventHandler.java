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

package org.mycore.datamodel.metadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;

import java.util.Collection;

/**
 * Interrupts deletion of object when there are active Links still referencing it.
 */
public class MCRActiveLinkCheckEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        final Collection<String> sources = MCRLinkTableManager.getInstance().getSourceOf(obj.mcrId,
            MCRLinkTableManager.ENTRY_TYPE_REFERENCE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sources size:{}", sources.size());
        }
        if (!sources.isEmpty()) {
            final MCRActiveLinkException activeLinks = new MCRActiveLinkException("Error while deleting object " + obj.mcrId
                + ". This object is still referenced by other objects and "
                + "can not be removed until all links are released.");
            for (final String curSource : sources) {
                activeLinks.addLink(curSource, obj.mcrId.toString());
            }
            throw new MCRException(activeLinks);
        }
    }
}
