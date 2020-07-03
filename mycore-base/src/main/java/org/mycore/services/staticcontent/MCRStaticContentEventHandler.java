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

package org.mycore.services.staticcontent;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;

public class MCRStaticContentEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        MCRObjectStaticContentGenerator.getContentGenerators()
            .stream()
            .map(MCRObjectStaticContentGenerator::new)
            .forEach(cg -> {
                try {
                    cg.generate(obj);
                } catch (IOException e) {
                    LOGGER.error(
                        "Error while creating static content " + cg.getTransformer() + " for " + obj.getId() + "!", e);
                }
            });
    }
}
