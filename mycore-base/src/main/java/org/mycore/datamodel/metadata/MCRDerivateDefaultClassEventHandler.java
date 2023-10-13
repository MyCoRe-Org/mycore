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
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

public class MCRDerivateDefaultClassEventHandler extends MCREventHandlerBase {
    private static final List<String> DEFAULT_CATEGORIES = MCRConfiguration2
        .getString("MCR." + MCRDerivateDefaultClassEventHandler.class.getSimpleName() + ".DefaultCategories")
        .map(MCRConfiguration2::splitValue)
        .orElseGet(() -> Stream.of("derivate_types:content"))
        .collect(Collectors.toUnmodifiableList());

    private static final Logger LOGGER = LogManager.getLogger();

    private static MCRMetaClassification asMetaClassification(MCRCategoryID categoryId) {
        return new MCRMetaClassification("classification", 0, null,
            categoryId.getRootID(), categoryId.getId());
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        final ArrayList<MCRMetaClassification> classifications = der.getDerivate().getClassifications();
        if (!classifications.isEmpty()) {
            //already defined at creation
            return;
        }

        final List<MCRCategoryID> categories = DEFAULT_CATEGORIES.stream()
            .map(MCRCategoryID::fromString) //checks syntax
            .collect(Collectors.toList());
        final MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.getInstance();
        final List<MCRCategoryID> missingCategories = categories.stream()
            .filter(Predicate.not(categoryDAO::exist))
            .collect(Collectors.toList());

        if (!missingCategories.isEmpty()) {
            LOGGER.error(() -> "Categories do not exist: " + missingCategories.stream()
                .map(MCRCategoryID::toString)
                .collect(Collectors.joining(", ")));
            return;
        }
        LOGGER.warn(() -> der.getId() + " has no classification, using "
            + DEFAULT_CATEGORIES.stream().collect(Collectors.joining(", ")) + " by default.");
        classifications.addAll(categories.stream()
            .map(MCRDerivateDefaultClassEventHandler::asMetaClassification)
            .collect(Collectors.toList()));
    }
}
