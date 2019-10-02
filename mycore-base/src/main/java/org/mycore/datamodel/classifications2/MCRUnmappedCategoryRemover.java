package org.mycore.datamodel.classifications2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private HashMap<MCRCategoryID, MCRCategoryID> toFromMapping;

    private ArrayList<MCRCategoryID> filtered;

    /**
     * @param classificationID of the classification to filter
     */
    public MCRUnmappedCategoryRemover(String classificationID) {
        this.classificationID = classificationID;
        this.initializeMapping();
    }

    public void filter() {
        filtered = new ArrayList<>();
        final MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        final MCRCategory category = dao
            .getCategory(MCRCategoryID.fromString(classificationID), -1);
        collectRemovableCategories(category);

        filtered.forEach(categoryToDelete -> {
            LOGGER.info("Delete Category {}", categoryToDelete);
            dao.deleteCategory(categoryToDelete);
        });
    }

    private boolean collectRemovableCategories(MCRCategory category) {
        final MCRCategoryID categoryID = category.getId();

        LOGGER.info("Filter Category: {}", categoryID);

        boolean hasMapping = false;

        if (toFromMapping.containsKey(categoryID)) {
            hasMapping = true;
        }

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
            .getInstance();
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

        LOGGER.info("Find mappings for category: {}", category.getId());

        mapping.ifPresent(label -> {
            final String[] mappingTargets = label.text.split(" ");
            for (String mappingTarget : mappingTargets) {
                final String[] kv = mappingTarget.split(":");
                String clazz = kv[0];
                if (classificationID.equals(clazz)) {
                    LOGGER.info("Found mapping from {} to {}", category.getId(), mappingTarget);
                    toFromMapping.put(MCRCategoryID.fromString(mappingTarget), category.getId());
                }
            }
        });
        category.getChildren().forEach(this::initializeMapping);
    }

}
