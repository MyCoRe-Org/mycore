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
package org.mycore.oai.classmapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * This class implements an event handler, which reloads classification entries
 * stored in datafield mappings/mapping. These entries are retrieved from other 
 * classifications where they are stored in as labels with language "x-mapping" or "x-mapping-xpath".
 *
 * @author Robert Stephan
 *
 */
public class MCRClassificationMappingEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger(MCRClassificationMappingEventHandler.class);

    public static final String LABEL_LANG_XPATH_MAPPING = "x-mapping-xpath";

    public static final String LABEL_LANG_XPATH_MAPPING_FALLBACK = "x-mapping-xpathfb";

    public static final String LABEL_LANG_X_MAPPING = "x-mapping";

    /** This configuration lists all eligible classifications for x-path-mapping */
    private static final String X_PATH_MAPPING_CLASSIFICATIONS
        = MCRConfiguration2.getString("MCR.Category.XPathMapping.ClassIDs").orElse("");

    private MCRMetaElement oldMappings = null;

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

    @Override
    protected void undoObjectCreated(MCREvent evt, MCRObject obj) {
        undo(obj);
    }

    @Override
    protected void undoObjectUpdated(MCREvent evt, MCRObject obj) {
        undo(obj);
    }

    @Override
    protected void undoObjectRepaired(MCREvent evt, MCRObject obj) {
        undo(obj);
    }

    /**
     * Creates x-mappings and XPath-mappings for a given object.
     * @param obj the {@link MCRObject} to add mappings to
     */
    private void createMapping(MCRObject obj) {
        MCRMetaElement mappings = obj.getMetadata().getMetadataElement("mappings");
        if (mappings != null) {
            oldMappings = mappings.clone();
            obj.getMetadata().removeMetadataElement("mappings");
        }

        MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();

        try {
            Document doc = new Document(obj.getMetadata().createXML().detach());
            List<MCRCategory> relevantCategories = new ArrayList<>();

            relevantCategories.addAll(getXMappingCategories(doc, dao));
            relevantCategories.addAll(getXPathMappingCategories(doc, dao));

            if (!relevantCategories.isEmpty()) {
                mappings = createMappingsElement();
                obj.getMetadata().setMetadataElement(mappings);
            }
            addMappings(mappings, relevantCategories);
        } catch (Exception je) {
            LOGGER.error("Error while finding classification elements", je);
        } finally {
            if (mappings == null || mappings.size() == 0) {
                obj.getMetadata().removeMetadataElement("mappings");
            }
        }
    }

    /**
     * Helper-method to create an empty element with the "mappings"-tag that will later contain all generated mappings
     * @return an {@link MCRMetaElement} containing the mappings
     */
    private MCRMetaElement createMappingsElement() {
        MCRMetaElement mappings = new MCRMetaElement();
        mappings.setTag("mappings");
        mappings.setClass(MCRMetaClassification.class);
        mappings.setHeritable(false);
        mappings.setNotInherit(true);
        return mappings;
    }

    /**
     * Searches for elements marked as a category inside a {@link Document} and
     * returns all matching {@link MCRCategory MCRCategories} that were found through the given DAO.
     * @param doc the document to search
     * @param dao the {@link MCRCategoryDAO} that gives access to all stored categories
     * @return a list of all categories matching the document
     */
    private List<MCRCategory> getXMappingCategories(Document doc, MCRCategoryDAO dao) {
        List<MCRCategory> categories = new ArrayList<>();
        XPathExpression<Element> classElementPath = XPathFactory.instance().compile("//*[@categid]",
            Filters.element());
        List<Element> classList = classElementPath.evaluate(doc);
        for (Element classElement : classList) {
            MCRCategory category = dao.getCategory(new MCRCategoryID(classElement.getAttributeValue("classid"),
                classElement.getAttributeValue("categid")), 0);
            if (category == null) {
                LOGGER.warn("Could not find a category for {}", new XMLOutputter().outputString(classElement));
            } else {
                categories.add(category);
            }
        }
        return categories;
    }

    /**
     * For a list of configured classifications
     * (see {@link MCRClassificationMappingEventHandler#X_PATH_MAPPING_CLASSIFICATIONS}),
     * searches for categories with at least one of the labels
     * {@link MCRClassificationMappingEventHandler#LABEL_LANG_XPATH_MAPPING}
     * or {@link MCRClassificationMappingEventHandler#LABEL_LANG_XPATH_MAPPING_FALLBACK}
     * through a given DAO. The XPaths attached to those categories are then evaluated against a given document.
     * The fallback is evaluated per classification.
     * A list with all {@link MCRCategory MCRCategories} with matching XPaths is returned.
     * @param doc the document to evaluate XPaths against
     * @param dao the {@link MCRCategoryDAO} that gives access to all stored categories
     * @return a list of all categories with matching XPaths in the document
     */
    private List<MCRCategory> getXPathMappingCategories(Document doc, MCRCategoryDAO dao) {
        List<MCRCategory> listToAdd = new ArrayList<>();
        final Map<String, Set<MCRCategory>> xPathMappingRelevantCategories = Arrays
            .stream(X_PATH_MAPPING_CLASSIFICATIONS.trim().split(",")).collect(Collectors.toMap(
                relevantClass -> relevantClass,
                relevantClass -> Stream.concat(
                    dao.getCategoriesByClassAndLang(relevantClass, LABEL_LANG_XPATH_MAPPING).stream(),
                    dao.getCategoriesByClassAndLang(relevantClass, LABEL_LANG_XPATH_MAPPING_FALLBACK).stream())
                    .collect(Collectors.toSet())));

        // check x-mapping-xpath-mappings
        for (Set<MCRCategory> categoriesPerClass : xPathMappingRelevantCategories.values()) {
            boolean isXPathMatched = false;
            for (MCRCategory category : categoriesPerClass) {
                if (category.getLabel(LABEL_LANG_XPATH_MAPPING).isPresent()) {

                    String xPath = category.getLabel(LABEL_LANG_XPATH_MAPPING).get().getText();
                    MCRXPathEvaluator evaluator = new MCRXPathEvaluator(new HashMap<>(), doc);

                    if (evaluator.test(xPath)) {
                        String taskMessage = String.format(Locale.ROOT, "adding x-path-mapping from '%s'",
                            category.getId().toString());
                        LOGGER.info(taskMessage);
                        listToAdd.add(category);
                        isXPathMatched = true;
                    }
                }
            }
            if (!isXPathMatched) {
                //check x-mapping-xpath-fallback-mappings
                for (MCRCategory category : categoriesPerClass) {
                    if (category.getLabel(LABEL_LANG_XPATH_MAPPING_FALLBACK).isPresent()) {

                        String xPath = category.getLabel(LABEL_LANG_XPATH_MAPPING_FALLBACK).get().getText();
                        MCRXPathEvaluator evaluator = new MCRXPathEvaluator(new HashMap<>(), doc);

                        if (evaluator.test(xPath)) {
                            String taskMessage = String.format(Locale.ROOT,
                                "adding x-path-mapping-fallback from '%s'",
                                category.getId().toString());
                            LOGGER.info(taskMessage);
                            listToAdd.add(category);
                        }
                    }
                }
            }
        }
        return listToAdd;
    }

    /**
     * Searches a given list with {@link MCRCategory MCRCategories} for labels that signal a mapping to a
     * classification. When the relevant labels are found, new mappings are added to the given
     * {@link MCRMetaElement mappings-element}.
     * The relevant language labels are: {@link MCRClassificationMappingEventHandler#LABEL_LANG_X_MAPPING},
     * {@link MCRClassificationMappingEventHandler#LABEL_LANG_XPATH_MAPPING} and
     * {@link MCRClassificationMappingEventHandler#LABEL_LANG_XPATH_MAPPING_FALLBACK}.
     * @param mappings the element that contains all mappings
     * @param categories the relevant categories that should be searched for specific language labels
     */
    private void addMappings(MCRMetaElement mappings, List<MCRCategory> categories) {
        for (MCRCategory category : categories) {
            category.getLabel(LABEL_LANG_X_MAPPING).ifPresent(label -> {
                String[] str = label.getText().split("\\s");
                for (String s : str) {
                    if (s.contains(":")) {
                        String[] mapClass = s.split(":");
                        MCRMetaClassification metaClass = new MCRMetaClassification("mapping", 0, null, mapClass[0],
                            mapClass[1]);
                        mappings.addMetaObject(metaClass);
                    }
                }
            });
            // If category is in list, the XPath is already successfully evaluated
            category.getLabel(LABEL_LANG_XPATH_MAPPING).ifPresent(label -> {
                MCRMetaClassification metaClass
                    = new MCRMetaClassification("mapping", 0, null, category.getId().getRootID(),
                        category.getId().getId());
                mappings.addMetaObject(metaClass);
            });
        }
    }

    /**
     * Undos an executed mapping event by either removing all mappings or restoring mappings from before the execution.
     * @param obj the {@link MCRObject} in which changes are rolled back
     */
    private void undo(MCRObject obj) {
        if (oldMappings == null) {
            obj.getMetadata().removeMetadataElement("mappings");
        } else {
            MCRMetaElement mmap = obj.getMetadata().getMetadataElement("mappings");
            if (mmap == null) {
                obj.getMetadata().setMetadataElement(createMappingsElement());
            }
            for (int i = 0; i < oldMappings.size(); i++) {
                mmap.addMetaObject(oldMappings.getElement(i));
            }
        }
    }
}
