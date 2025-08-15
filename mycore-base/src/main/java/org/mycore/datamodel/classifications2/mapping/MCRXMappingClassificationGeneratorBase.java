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
package org.mycore.datamodel.classifications2.mapping;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.mapping.MCRGeneratorClassificationMapperBase.Generator;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * {@link MCRXMappingClassificationGeneratorBase} is a base implementation for data model specific
 * implementations of {@link Generator} that looks for mapping information in all
 * categories already present in the data model representation of a MyCoRe object.
 * <p>
 * For each category ID in the data model representation, if the corresponding category
 * has a <code>x-mapping</code>-label, the content of that label is used
 * to obtain additional category IDs using an {@link Evaluator}.
 */
public abstract class MCRXMappingClassificationGeneratorBase implements Generator {

    protected final Logger logger = LogManager.getLogger(getClass());

    public static final String EVALUATOR_KEY = "Evaluator";

    public static final String ON_MISSING_MAPPED_CATEGORY_KEY = "OnMissingMappedCategory";

    public static final String LABEL_LANG_X_MAPPING = "x-mapping";

    private final Evaluator evaluator;

    private final OnMissingMappedCategory onMissingMappedCategory;

    public MCRXMappingClassificationGeneratorBase(Evaluator evaluator,
        OnMissingMappedCategory onMissingMappedCategory) {
        this.evaluator = Objects.requireNonNull(evaluator,
            "Evaluator must not be null");
        this.onMissingMappedCategory = Objects.requireNonNull(onMissingMappedCategory,
            "On-Missing-Mapped-Category must not be null");
    }

    @Override
    public final List<MCRGeneratorClassificationMapperBase.Mapping> generate(MCRCategoryDAO dao, MCRObject object) {
        return getCategories(dao, object)
            .filter(Objects::nonNull)
            .map(category -> findMappings(dao, object, category))
            .flatMap(Collection::stream)
            .distinct()
            .peek(mapping -> mapping.logInfo(logger))
            .map(XMapping::toMapping)
            .toList();
    }

    protected abstract Stream<MCRCategory> getCategories(MCRCategoryDAO dao, MCRObject object);

    private List<XMapping> findMappings(MCRCategoryDAO dao, MCRObject object, MCRCategory sourceCategory) {
        MCRCategoryID sourceCategoryId = sourceCategory.getId();
        return sourceCategory.getLabel(LABEL_LANG_X_MAPPING)
            .map(label -> evaluator.getCategoryIds(label.getText(), object)
                .stream()
                .filter(categoryId -> !categoryId.isRootID())
                .filter(categoryId -> dao.exist(categoryId) || handleMissingCategory(categoryId, sourceCategoryId))
                .map(targetCategoryId -> new XMapping(sourceCategoryId, targetCategoryId))
                .collect(Collectors.toList()))
            .orElse(List.of());
    }

    private boolean handleMissingCategory(MCRCategoryID categoryID, MCRCategoryID sourceCategoryId) {
        if (onMissingMappedCategory == OnMissingMappedCategory.THROW_EXCEPTION) {
            throw new MCRException("Mapped category ID " + categoryID + " for " + sourceCategoryId + " is missing");
        }
        if (onMissingMappedCategory.warnAboutMissingCategory && logger.isWarnEnabled()) {
            logger.warn("Mapped category ID {} for {} is missing", categoryID, sourceCategoryId);
        }
        return onMissingMappedCategory.addMissingCategory;
    }

    public interface Evaluator {

        Set<MCRCategoryID> getCategoryIds(String mapping, MCRObject object);

    }

    public enum OnMissingMappedCategory {

        THROW_EXCEPTION(true, false),

        WARN_AND_IGNORE(true, false),

        WARN_AND_ADD(true, true),

        IGNORE(false, false),

        ADD(false, true);

        private final boolean warnAboutMissingCategory;

        private final boolean addMissingCategory;

        OnMissingMappedCategory(boolean warn, boolean add) {
            this.warnAboutMissingCategory = warn;
            this.addMissingCategory = add;
        }

    }

    protected record XMapping(MCRCategoryID sourceCategoryId, MCRCategoryID targetCategoryId) {

        private void logInfo(Logger logger) {
            if (logger.isInfoEnabled()) {
                logger.info("found mapping from {} to {}", sourceCategoryId.toString(),
                    targetCategoryId.toString());
            }
        }

        private MCRGeneratorClassificationMapperBase.Mapping toMapping() {
            String generator = getGenerator(sourceCategoryId, targetCategoryId);
            return new MCRGeneratorClassificationMapperBase.Mapping(generator, targetCategoryId);
        }

        private String getGenerator(MCRCategoryID sourceCategoryId, MCRCategoryID targetCategoryId) {
            return String.format(Locale.ROOT, "%s2%s", sourceCategoryId.getRootID(), targetCategoryId.getRootID());
        }

    }

}
