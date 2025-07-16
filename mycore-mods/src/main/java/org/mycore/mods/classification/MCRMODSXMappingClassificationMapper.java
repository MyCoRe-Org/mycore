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
package org.mycore.mods.classification;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.mods.MCRMODSWrapper;

/**
 * A {@link MCRMODSXMappingClassificationMapper} is a {@link MCRMODSClassificationMapper} that looks for mapping
 * information in all classification values already present in the MODS document.
 * <p>
 * For each classification value, if the corresponding classification category contains a <code>x-mapping</code>
 * label, the content of that label is used as a space separated list of classification category IDs.
 * </p>
 * Example form <code>foo_bar</code>:
 * <pre><code>
 * &lt;category ID=&quot;article&quot;&gt;
 * &nbsp;&lt;label xml:lang=&quot;en&quot; text=&quot;ScolarlyArticle&quot; /&gt;
 * &nbsp;&lt;label xml:lang=&quot;x-mapping&quot; text=&quot;foo_baz:Article&quot; /&gt;
 * &lt;/category&gt;
 * </code></pre>
 * If a MODS document contains the classification category <code>foo_bar:article</code>, the
 * classification category <code>foo_baz:ScolarlyArticle</code> will be provided. The corresponding generator
 * will be named <code>foo_bar2foo_baz</code>.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.mods.classification.MCRMODSXMappingClassificationMapper
 * </code></pre>
 */
public class MCRMODSXMappingClassificationMapper implements MCRMODSClassificationMapper {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String LABEL_LANG_X_MAPPING = "x-mapping";

    @Override
    public List<Mapping> findMappings(MCRCategoryDAO dao, MCRMODSWrapper wrapper) {
        return wrapper.getMcrCategoryIDs().stream()
            .map(categoryId -> dao.getCategory(categoryId, 0))
            .filter(Objects::nonNull)
            .map(category -> findMappings(dao, category))
            .flatMap(Collection::stream)
            .distinct()
            .peek(XMapping::logInfo)
            .map(XMapping::toMapping)
            .toList();
    }

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

    private record XMapping(MCRCategoryID sourceCategoryId, MCRCategoryID targetCategoryId) {

        private void logInfo() {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("found mapping from {} to {}", sourceCategoryId.toString(),
                    targetCategoryId.toString());
            }
        }

        private Mapping toMapping() {
            return new MCRMODSClassificationMapper.Mapping(getGenerator(), targetCategoryId);
        }

        private String getGenerator() {
            return String.format(Locale.ROOT, "%s2%s", sourceCategoryId.getRootID(),
                targetCategoryId.getRootID());
        }

    }

}
