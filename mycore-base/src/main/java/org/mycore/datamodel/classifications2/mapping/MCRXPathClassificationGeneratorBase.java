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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Parent;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * {@link MCRDefaultXMappingClassificationGenerator} is a base implementation for data model specific implementations
 * of {@link MCRGeneratorClassificationMapperBase.Generator} that looks for mapping information for a given list
 * of classifications, by checking if XPaths configured for categories of such classifications match an XML value
 * obtained from the data model representation of a MyCoRe object.
 * <p>
 * For each classification, the set of classification categories containing a <code>x-mapping-xpath</code> label is
 * obtained. For all such classification categories, the corresponding XPath is evaluated and if the XML value
 * matches, the classification category is provided. If no classification is provided for a given classification,
 * the same procedure is performed for the <code>x-mapping-xpathfb</code> labes as a fallback.
 */
public abstract class MCRXPathClassificationGeneratorBase implements MCRGeneratorClassificationMapperBase.Generator {

    protected final Logger logger = LogManager.getLogger(getClass());

    private static final String LABEL_LANG_XPATH_MAPPING = "x-mapping-xpath";

    private static final String LABEL_LANG_XPATH_MAPPING_FALLBACK = "x-mapping-xpathfb";

    public static final String CLASSIFICATION_IDS_KEY = "ClassificationIds";

    private final List<String> classificationsIds;

    public MCRXPathClassificationGeneratorBase(List<String> classificationsIds) {
        this.classificationsIds = new ArrayList<>(Objects
            .requireNonNull(classificationsIds, "Classification IDs must not be null"));
        this.classificationsIds.forEach(filter -> Objects
            .requireNonNull(filter, "Classification ID must not be null"));
    }

    @Override
    public final List<MCRGeneratorClassificationMapperBase.Mapping> generate(MCRCategoryDAO dao, MCRObject object) {
        Parent parent = toJdomParent(dao, object);
        return classificationsIds.stream()
            .flatMap(classificationId -> findMappings(dao, parent, classificationId))
            .peek(xPathMapping -> xPathMapping.logInfo(logger))
            .map(XPathMapping::toMapping)
            .toList();
    }

    protected abstract Parent toJdomParent(MCRCategoryDAO dao, MCRObject object);

    private Stream<XPathMapping> findMappings(MCRCategoryDAO dao, Parent parent, String classificationId) {
        List<XPathMapping> mappings = findMappings(dao, parent, classificationId, LABEL_LANG_XPATH_MAPPING);
        if (!mappings.isEmpty()) {
            return mappings.stream();
        }
        return findMappings(dao, parent, classificationId, LABEL_LANG_XPATH_MAPPING_FALLBACK).stream();
    }

    private List<XPathMapping> findMappings(MCRCategoryDAO dao, Parent parent, String classificationId, String lang) {
        List<XPathMapping> mappings = new ArrayList<>();
        dao.getCategoriesByClassAndLang(classificationId, lang).forEach(category -> {
            assert category.getLabel(lang).isPresent();
            String xPath = category.getLabel(lang).get().getText();
            xPath = MCRClassificationMappingUtil.replacePattern(xPath);
            MCRXPathEvaluator evaluator = new MCRXPathEvaluator(new HashMap<>(), parent);
            if (evaluator.test(xPath)) {
                mappings.add(new XPathMapping(lang, category.getId()));
            }
        });
        return mappings;
    }

    protected record XPathMapping(String xLanguage, MCRCategoryID targetCategoryId) {

        private void logInfo(Logger logger) {
            if (logger.isInfoEnabled()) {
                logger.info("found x-path-mapping to {} for {}", targetCategoryId.toString(), xLanguage);
            }
        }

        private MCRGeneratorClassificationMapperBase.Mapping toMapping() {
            String generator = String.format(Locale.ROOT, "xpathmapping2%s", targetCategoryId.getRootID());
            return new MCRGeneratorClassificationMapperBase.Mapping(generator, targetCategoryId);
        }

    }

}
