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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * A {@link MCRXMappingClassificationMapper} is a {@link MCRClassificationMapper} that looks for mapping
 * information for a given list of classifications, by checking if XPaths configured for categories of such
 * classifications match the metadata document.
 * <p>
 * For each classification, the set of classification categories containing a <code>x-mapping-xpath</code> label is
 * obtained. For all such classification categories, the corresponding XPath is evaluated and if the metadata document
 * matches, the classification category is provided. If no classification is provided for a given classification,
 * the same procedure is performed for the <code>x-mapping-xpathfb</code> labes as a fallback.
 * </p>
 * Example form <code>foo_bar</code>:
 * <pre><code>
 * &lt;category ID=&quot;article&quot;&gt;
 * &nbsp;&lt;label xml:lang=&quot;en&quot; text=&quot;Article&quot; /&gt;
 * &nbsp;&lt;label xml:lang=&quot;x-mapping-xpath&quot; text=&quot;mods:genre[text()='article']&quot; /&gt;
 * &nbsp;&lt;label xml:lang=&quot;x-mapping-xpathfb&quot; text=&quot;mods:genre[text()='other']&quot; /&gt;
 * &lt;/category&gt;
 * </code></pre>
 * If <code>foo_bar</code> is in the list of classifications, the classification category
 * <code>foo_bar:article</code> will be provided, if the XPath <code>mods:genre[text()='article']</code> is matching
 * (or if no other classification category has a matching XPath and <code>mods:genre[text()='other']</code> is
 * matching).
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRXPathClassificationMapper#CLASSIFICATION_IDS_KEY} can be used to
 * specify the comma separated list of classifications IDs.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.events.MCRXPathClassificationMapper
 * [...].ClassificationIDs=foo_bar,foo_baz
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRXPathClassificationMapper.Factory.class)
public class MCRXPathClassificationMapper implements MCRClassificationMapper {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String LABEL_LANG_XPATH_MAPPING = "x-mapping-xpath";

    private static final String LABEL_LANG_XPATH_MAPPING_FALLBACK = "x-mapping-xpathfb";

    public static final String CLASSIFICATION_IDS_KEY = "ClassificationIds";

    private final List<String> classificationsIds;

    public MCRXPathClassificationMapper(String... classificationsIds) {
        this(Arrays.asList(Objects.requireNonNull(classificationsIds, "Classification IDs must not be null")));
    }

    public MCRXPathClassificationMapper(List<String> classificationsIds) {
        this.classificationsIds = new ArrayList<>(Objects
            .requireNonNull(classificationsIds, "Classification IDs must not be null"));
        this.classificationsIds.forEach(filter -> Objects
            .requireNonNull(filter, "Classification ID must not be null"));
    }

    @Override
    public List<MCRCategoryID> findMappings(MCRCategoryDAO dao, Document metadataDocument) {
        return classificationsIds.stream()
            .flatMap(xPathClassificationId -> findMappings(dao, metadataDocument, xPathClassificationId))
            .peek(XPathMapping::logInfo)
            .map(XPathMapping::toCategoryId)
            .toList();
    }

    private Stream<XPathMapping> findMappings(MCRCategoryDAO dao, Document metadataDocument, String classificationId) {
        List<XPathMapping> mappings = findMappings(dao, metadataDocument, classificationId, LABEL_LANG_XPATH_MAPPING);
        if (!mappings.isEmpty()) {
            return mappings.stream();
        }
        return findMappings(dao, metadataDocument, classificationId, LABEL_LANG_XPATH_MAPPING_FALLBACK).stream();
    }

    private List<XPathMapping> findMappings(MCRCategoryDAO dao, Document metadataDocument,
        String xPathClassificationId, String xLanguage) {
        List<XPathMapping> mappings = new ArrayList<>();
        dao.getCategoriesByClassAndLang(xPathClassificationId, xLanguage).forEach(category -> {
            assert category.getLabel(xLanguage).isPresent();
            String xPath = category.getLabel(xLanguage).get().getText();
            xPath = MCRClassificationMappingUtil.replacePattern(xPath);
            MCRXPathEvaluator evaluator = new MCRXPathEvaluator(new HashMap<>(), metadataDocument);
            if (evaluator.test(xPath)) {
                mappings.add(new XPathMapping(xLanguage, category.getId()));
            }
        });
        return mappings;
    }

    private record XPathMapping(String xLanguage, MCRCategoryID targetCategoryId) {

        private void logInfo() {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("found x-path-mapping to {} for {}", targetCategoryId.toString(), xLanguage);
            }
        }

        private MCRCategoryID toCategoryId() {
            return targetCategoryId;
        }

    }

    public static class Factory implements Supplier<MCRXPathClassificationMapper> {

        @MCRProperty(name = CLASSIFICATION_IDS_KEY, required = false)
        public String classificationIds;

        @Override
        public MCRXPathClassificationMapper get() {
            return new MCRXPathClassificationMapper(getClassificationsIds());
        }

        private List<String> getClassificationsIds() {
            return MCRConfiguration2.splitValue(classificationIds == null ? "" : classificationIds).toList();
        }

    }

}
