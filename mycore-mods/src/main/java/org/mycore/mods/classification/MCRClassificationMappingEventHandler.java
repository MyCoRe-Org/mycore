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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

/**
 * Maps classifications in Mods-Documents.
 * <p>You can define a label <b><code>x-mapping</code></b> in a classification with space seperated categoryIds
 * to which the classification will be mapped. You can further define a label <b><code>x-mapping-xpath</code></b> in a
 * classification which creates a classification mapping when the XPath inside the x-mapping-xpath attribute
 * is matched. You can also define a label <b><code>x-mapping-xpathfb</code></b> which contains a fallback value
 * in case that no other XPaths inside a classification match.</p>
 * <code>
 * &lt;category ID=&quot;article&quot; counter=&quot;1&quot;&gt;<br>
 * &nbsp;&lt;label xml:lang=&quot;en&quot; text=&quot;Article / Chapter&quot; /&gt;<br>
 * &nbsp;&lt;label xml:lang=&quot;de&quot; text=&quot;Artikel / Aufsatz&quot; /&gt;<br>
 * &nbsp;&lt;label xml:lang=&quot;x-mapping&quot; text=&quot;diniPublType:article&quot; /&gt;<br>
 * &nbsp;&lt;label xml:lang=&quot;x-mapping-xpath&quot; text=&quot;mods:genre[text()='article']&quot; /&gt;<br>
 * &nbsp;&lt;label xml:lang=&quot;x-mapping-xpathfb&quot; text=&quot;mods:genre[text()='other']&quot; /&gt;<br>
 * &lt;/category&gt;
 * </code>
 *
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRClassificationMappingEventHandler extends MCREventHandlerBase {

    public static final String GENERATOR_SUFFIX = "-mycore";

    public static final String LABEL_LANG_XPATH_MAPPING = "x-mapping-xpath";

    public static final String LABEL_LANG_XPATH_MAPPING_FALLBACK = "x-mapping-xpathfb";

    public static final String LABEL_LANG_X_MAPPING = "x-mapping";

    public static final String XPATH_GENERATOR_NAME = "xpathmapping";

    /** This configuration lists all eligible classifications for x-path-mapping */
    private static final String X_PATH_MAPPING_CLASSIFICATIONS =
        MCRConfiguration2.getString("MCR.Category.XPathMapping.ClassIDs").orElse("");

    private static final Logger LOGGER = LogManager.getLogger(MCRClassificationMappingEventHandler.class);

    /**
     * Reads all {@link MCRClassificationMappingEventHandler#LABEL_LANG_X_MAPPING x-mappings} from a category.
     * All mapped categories that exist are returned as a list.
     * @param category the source category containing a mapping
     * @return a list of all mapped categories
     */
    private static List<Map.Entry<MCRCategoryID, MCRCategoryID>> getXMappings(MCRCategory category) {
        Optional<MCRLabel> labelOptional = category.getLabel(LABEL_LANG_X_MAPPING);

        if (labelOptional.isPresent()) {
            final MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
            String label = labelOptional.get().getText();
            return Stream.of(label.split("\\s"))
                .map(MCRCategoryID::fromString)
                .filter(id -> !id.isRootID())
                .filter(dao::exist)
                .map(mappingTarget -> new AbstractMap.SimpleEntry<>(category.getId(), mappingTarget))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Searches all configured classifications
     * (see {@link MCRClassificationMappingEventHandler#X_PATH_MAPPING_CLASSIFICATIONS}) for categories
     * with language labels {@link MCRClassificationMappingEventHandler#LABEL_LANG_XPATH_MAPPING} or
     * {@link MCRClassificationMappingEventHandler#LABEL_LANG_XPATH_MAPPING_FALLBACK}.
     * All categories with said label and present in database are returned in a Map of Sets,
     * separated by their classification.
     * @return a Map with classification-IDs as key and Sets of {@link MCRCategory categories}
     * with the XPath-Mapping label as value
     */
    private static Map<String, Set<MCRCategory>> loadAllXPathMappings() {
        final MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        return Arrays.stream(X_PATH_MAPPING_CLASSIFICATIONS.trim().split(","))
            .collect(Collectors.toMap(
                relevantClass -> relevantClass,
                relevantClass -> Stream.concat(
                    dao.getCategoriesByClassAndLang(relevantClass, LABEL_LANG_XPATH_MAPPING).stream(),
                    dao.getCategoriesByClassAndLang(relevantClass, LABEL_LANG_XPATH_MAPPING_FALLBACK).stream())
                    .collect(Collectors.toSet())));
    }

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        createMapping(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        createMapping(obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        createMapping(obj);
    }

    private static String getGenerator(MCRCategoryID src, MCRCategoryID target) {
        return String.format(Locale.ROOT, "%s2%s%s", src.getRootID(), target.getRootID(), GENERATOR_SUFFIX);
    }

    private static String getXPathMappingGenerator(MCRCategoryID target) {
        return String.format(Locale.ROOT, "%s2%s%s", XPATH_GENERATOR_NAME, target.getRootID(),
            GENERATOR_SUFFIX);
    }

    /**
     * Creates x-mappings and XPath-mappings for a given object.
     * @param obj the {@link MCRObject} to add mappings to
     */
    private void createMapping(MCRObject obj) {
        MCRMODSWrapper mcrmodsWrapper = new MCRMODSWrapper(obj);
        if (mcrmodsWrapper.getMODS() == null) {
            return;
        }
        // vorher alle mit generator *-mycore löschen
        mcrmodsWrapper.getElements("mods:classification[contains(@generator, '" + GENERATOR_SUFFIX + "')]")
            .stream().forEach(Element::detach);

        LOGGER.info("check mappings {}", obj.getId());
        final MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        // check x-mappings
        mcrmodsWrapper.getMcrCategoryIDs().stream()
            .map(categoryId -> {
                return dao.getCategory(categoryId, 0);
            })
            .filter(Objects::nonNull)
            .map(MCRClassificationMappingEventHandler::getXMappings)
            .flatMap(Collection::stream)
            .distinct()
            .forEach(mapping -> {
                String taskMessage = String.format(Locale.ROOT, "add mapping from '%s' to '%s'",
                    mapping.getKey().toString(), mapping.getValue().toString());
                LOGGER.info(taskMessage);
                Element mappedClassification = mcrmodsWrapper.addElement("classification");
                String generator = getGenerator(mapping.getKey(), mapping.getValue());
                mappedClassification.setAttribute("generator", generator);
                MCRClassMapper.assignCategory(mappedClassification, mapping.getValue());
            });

        final Map<String, Set<MCRCategory>> xPathMappings = loadAllXPathMappings();
        for (Set<MCRCategory> categoriesPerClass : xPathMappings.values()) {
            boolean isXPathMatched = false;
            // check x-mapping-xpath-mappings
            for (MCRCategory category : categoriesPerClass) {
                if (category.getLabel(LABEL_LANG_XPATH_MAPPING).isPresent()) {

                    String xPath = category.getLabel(LABEL_LANG_XPATH_MAPPING).get().getText();
                    xPath = org.mycore.common.events.MCRClassificationMappingEventHandler.replacePattern(xPath);
                    MCRXPathEvaluator evaluator = new MCRXPathEvaluator(new HashMap<>(), mcrmodsWrapper.getMODS());

                    if (evaluator.test(xPath)) {
                        String taskMessage = String.format(Locale.ROOT, "add x-path-mapping from '%s'",
                            category.getId().toString());
                        LOGGER.info(taskMessage);
                        Element mappedClassification = mcrmodsWrapper.addElement("classification");
                        String generator = getXPathMappingGenerator(category.getId());
                        mappedClassification.setAttribute("generator", generator);
                        MCRClassMapper.assignCategory(mappedClassification, category.getId());
                        isXPathMatched = true;
                    }
                }
            }
            //check x-mapping-xpath-fallback-mappings
            if (!isXPathMatched) {
                for (MCRCategory category : categoriesPerClass) {
                    if (category.getLabel(LABEL_LANG_XPATH_MAPPING_FALLBACK).isPresent()) {

                        String xPath = category.getLabel(LABEL_LANG_XPATH_MAPPING_FALLBACK).get().getText();
                        xPath = org.mycore.common.events.MCRClassificationMappingEventHandler.replacePattern(xPath);
                        MCRXPathEvaluator evaluator = new MCRXPathEvaluator(new HashMap<>(), mcrmodsWrapper.getMODS());

                        if (evaluator.test(xPath)) {
                            String taskMessage = String.format(Locale.ROOT, "add x-path-mapping-fallback from '%s'",
                                category.getId().toString());
                            LOGGER.info(taskMessage);
                            Element mappedClassification = mcrmodsWrapper.addElement("classification");
                            String generator = getXPathMappingGenerator(category.getId());
                            mappedClassification.setAttribute("generator", generator);
                            MCRClassMapper.assignCategory(mappedClassification, category.getId());
                        }
                    }
                }
            }
        }

        LOGGER.debug("mapping complete.");
    }

}
