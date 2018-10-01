package org.mycore.datamodel.classifications2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Filters a Classification so it only contains Elements (or Elements with Children) which can be mapped from a other classification.
 */
public class MCRClassificationMappingFilter {

    private static final Logger  LOGGER = LogManager.getLogger();

    private final String classificationID;

    // contains mappings like mir_genres:article -> marcgt:article
    private HashMap<String, String> toFromMapping;

    private ArrayList<String> filtered;

    /**
     * @param classificationID of the classification to filter
     */
    public MCRClassificationMappingFilter(String classificationID) {
        this.classificationID = classificationID;
        this.initializeMapping();
    }

    public void filter() {
        filtered = new ArrayList<>();
        final MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        final MCRCategory category = dao
            .getCategory(MCRCategoryID.fromString(classificationID), -1);
        thisOrChildHasMapping(category);

        filtered.forEach(categoryToDelete-> {
            LOGGER.info("Delete Category {}", categoryToDelete);
            dao.deleteCategory(MCRCategoryID.fromString(categoryToDelete));
        });
    }

    private boolean thisOrChildHasMapping(MCRCategory category) {
        final String categoryID = category.getId().toString();

        LOGGER.info("Filter Category: {}", categoryID);

        boolean thisORChildContains = false;

        if (toFromMapping.containsKey(categoryID)) {
            thisORChildContains = true;
        }

        final List<MCRCategory> children = category.getChildren();
        for (MCRCategory child : children) {
            thisORChildContains = thisOrChildHasMapping(child) || thisORChildContains;
        }

        if (!thisORChildContains) {
            filtered.add(categoryID);
        }

        return thisORChildContains;
    }

    private void initializeMapping() {
        toFromMapping = new HashMap<>();
        final MCRCategoryDAO dao = MCRCategoryDAOFactory
            .getInstance();
        final List<MCRCategory> rootCategories = dao
            .getRootCategories()
            .stream()
            .map(category -> dao.getRootCategory(category.getId(),-1 ))
            .collect(Collectors.toList());

        for (MCRCategory rootCategory : rootCategories) {
            initializeMapping(rootCategory);
        }
    }

    private void initializeMapping(MCRCategory category) {
        final Optional<MCRLabel> mapping = category.getLabel("x-mapping");

        LOGGER.info("Find mappings for category: {}", category.getId().toString());

        mapping.ifPresent(label -> {
            final String[] mappingTargets = label.text.split(" ");
            for (String mappingTarget : mappingTargets) {
                final String[] kv = mappingTarget.split(":");
                String clazz = kv[0];
                String categ = kv[1];
                if (classificationID.equals(clazz)) {
                    LOGGER.info("Found mapping from {} to {}", category.getId(), mappingTarget );
                    toFromMapping.put(mappingTarget, category.getId().toString());
                }
            }
        });

        category.getChildren().forEach(this::initializeMapping);
    }

}
