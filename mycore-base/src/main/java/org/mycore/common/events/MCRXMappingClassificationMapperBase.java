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
package org.mycore.common.events;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCRClassificationMappingEventHandlerBase.Mapper;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * {@link MCRXMappingClassificationMapperBase} is a base implementation for data model specific implementations of
 * {@link Mapper} that that looks for mapping information in all classification values already present in the
 * intermediate representation of a MyCoRe object.
 * <p>
 * For each classification value, if the corresponding classification category contains a <code>x-mapping</code>
 * label, the content of that label is used as a space separated list of classification category IDs.
 *
 * @param <I> The intermediate representation used by the corresponding implementation of 
 * {@link MCRXPathClassificationMapperBase}
 * @param <V> The value type used by the corresponding implementation of 
 * {@link MCRXPathClassificationMapperBase}
 */
public abstract class MCRXMappingClassificationMapperBase<I, V> implements Mapper<I, V> {

    protected final Logger logger = LogManager.getLogger(getClass());

    public static final String LABEL_LANG_X_MAPPING = "x-mapping";

    @Override
    public final List<V> findMappings(MCRCategoryDAO dao, I intermediateRepresentation) {
        return getCategories(dao, intermediateRepresentation)
            .filter(Objects::nonNull)
            .map(category -> findMappings(dao, category))
            .flatMap(Collection::stream)
            .distinct()
            .peek(mapping -> mapping.logInfo(logger))
            .map(this::toMappedValue)
            .toList();
    }

    protected abstract Stream<MCRCategory> getCategories(MCRCategoryDAO dao, I intermediateRepresentation);

    protected abstract V toMappedValue(XMapping xPathMapping);

    private List<XMapping> findMappings(MCRCategoryDAO dao, MCRCategory sourceCategory) {
        MCRCategoryID sourceCategoryId = sourceCategory.getId();
        return sourceCategory.getLabel(LABEL_LANG_X_MAPPING)
            .map(label -> Stream.of(label.getText().split("\\s"))
                .map(MCRCategoryID::ofString)
                .filter(id -> !id.isRootID())
                .filter(dao::exist)
                .map(targetCategoryId -> new XMapping(sourceCategoryId, targetCategoryId))
                .collect(Collectors.toList()))
            .orElse(List.of());
    }

    protected record XMapping(MCRCategoryID sourceCategoryId, MCRCategoryID targetCategoryId) {

        private void logInfo(Logger logger) {
            if (logger.isInfoEnabled()) {
                logger.info("found mapping from {} to {}", sourceCategoryId.toString(),
                    targetCategoryId.toString());
            }
        }

    }

}
