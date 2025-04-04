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

package org.mycore.datamodel.classifications2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Removes Categories which can not be mapped from a other classification.
 */
public class MCRUnmappedCategoryRemover {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String classificationID;

    // contains mappings like mir_genres:article -> marcgt:article
    private Map<MCRCategoryID, MCRCategoryID> toFromMapping;

    private List<MCRCategoryID> filtered;

    /**
     * @param classificationID of the classification to filter
     */
    public MCRUnmappedCategoryRemover(String classificationID) {
        this.classificationID = classificationID;
        this.initializeMapping();
    }

    public void filter() {
        filtered = new ArrayList<>();
        final MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();
        final MCRCategory category = dao
            .getCategory(MCRCategoryID.ofString(classificationID), -1);
        collectRemovableCategories(category);

        filtered.forEach(categoryToDelete -> {
            LOGGER.info("Delete Category {}", categoryToDelete);
            dao.deleteCategory(categoryToDelete);
        });
    }

    private boolean collectRemovableCategories(MCRCategory category) {
        final MCRCategoryID categoryID = category.getId();

        LOGGER.info("Filter Category: {}", categoryID);

        boolean hasMapping = toFromMapping.containsKey(categoryID);

        final List<MCRCategory> children = category.getChildren();
        for (MCRCategory child : children) {
            hasMapping = collectRemovableCategories(child) || hasMapping;
        }

        if (!hasMapping) {
            filtered.add(categoryID);
            // remove children from deleted list so we have only one delete operation for this category
            children.stream()
                .map(MCRCategory::getId)
                .forEach(filtered::remove);
        }

        return hasMapping;
    }

    private void initializeMapping() {
        toFromMapping = new HashMap<>();
        final MCRCategoryDAO dao = MCRCategoryDAOFactory
            .obtainInstance();
        final List<MCRCategory> rootCategories = dao
            .getRootCategories()
            .stream()
            .map(category -> dao.getRootCategory(category.getId(), -1))
            .collect(Collectors.toList());

        for (MCRCategory rootCategory : rootCategories) {
            initializeMapping(rootCategory);
        }
    }

    private void initializeMapping(MCRCategory category) {
        final Optional<MCRLabel> mapping = category.getLabel("x-mapping");

        LOGGER.info("Find mappings for category: {}", category::getId);

        mapping.ifPresent(label -> {
            final String[] mappingTargets = label.text.split(" ");
            for (String mappingTarget : mappingTargets) {
                final String[] kv = mappingTarget.split(":");
                String clazz = kv[0];
                if (classificationID.equals(clazz)) {
                    LOGGER.info("Found mapping from {} to {}", category::getId, () -> mappingTarget);
                    toFromMapping.put(MCRCategoryID.ofString(mappingTarget), category.getId());
                }
            }
        });
        category.getChildren().forEach(this::initializeMapping);
    }

}
