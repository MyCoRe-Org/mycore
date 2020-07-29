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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

public class MCRDerivateDefaultClassEventHandler extends MCREventHandlerBase {
    private static final String DEFAULT_CATEGORY = MCRConfiguration2
        .getString("MCR." + MCRDerivateDefaultClassEventHandler.class.getSimpleName() + ".DefaultCategory")
        .orElse("content");

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        final ArrayList<MCRMetaClassification> classifications = der.getDerivate().getClassifications();
        if (!classifications.isEmpty()) {
            return;
        }
        MCRCategoryID category = new MCRCategoryID("derivate_types", DEFAULT_CATEGORY);
        if (!MCRCategoryDAOFactory.getInstance().exist(category)) {
            LOGGER.error("Category does not exist: " + category);
            return;
        }
        LOGGER.warn(der.getId() + " has no classification, using " + DEFAULT_CATEGORY + " by default.");
        classifications
            .add(new MCRMetaClassification("classification", 0, null, category.getRootID(), category.getID()));
    }
}
