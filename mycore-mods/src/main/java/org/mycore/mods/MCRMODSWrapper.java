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

package org.mycore.mods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaXML;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectMetadata;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.mods.classification.MCRClassMapper;

/**
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler
 */
public class MCRMODSWrapper {

    public static final String MODS_OBJECT_TYPE = MCRConfiguration2.getStringOrThrow("MCR.MODS.NewObjectType");
    //ancestor::mycoreobject required for MCR-927
    static final String LINKED_RELATED_ITEMS = "mods:relatedItem[@type='host'"
        + " and ancestor::mycoreobject/structure/parents/parent or"
        + " string-length(substring-after(@xlink:href,'_')) > 0 and"
        + " string-length(substring-after(substring-after(@xlink:href,'_'), '_')) > 0 and"
        + " number(substring-after(substring-after(@xlink:href,'_'),'_')) > 0 and"
        + " (" + xPathRelationshipTypeTest() + ")]";
    private static final String MODS_CONTAINER = "modsContainer";
    private static final String DEF_MODS_CONTAINER = "def.modsContainer";
    private static final String MODS_DATAMODEL = "datamodel-mods.xsd";

    private static final List<String> TOP_LEVEL_ELEMENT_ORDER = new ArrayList<>();

    private static final Set<String> SUPPORTED_TYPES = MCRConfiguration2
        .getOrThrow("MCR.MODS.Types", MCRConfiguration2::splitValue)
        .collect(Collectors.toSet());

    static {
        TOP_LEVEL_ELEMENT_ORDER.add("typeOfResource");
        TOP_LEVEL_ELEMENT_ORDER.add("titleInfo");
        TOP_LEVEL_ELEMENT_ORDER.add("name");
        TOP_LEVEL_ELEMENT_ORDER.add("genre");
        TOP_LEVEL_ELEMENT_ORDER.add("originInfo");
        TOP_LEVEL_ELEMENT_ORDER.add("language");
        TOP_LEVEL_ELEMENT_ORDER.add("abstract");
        TOP_LEVEL_ELEMENT_ORDER.add("note");
        TOP_LEVEL_ELEMENT_ORDER.add("subject");
        TOP_LEVEL_ELEMENT_ORDER.add("classification");
        TOP_LEVEL_ELEMENT_ORDER.add("relatedItem");
        TOP_LEVEL_ELEMENT_ORDER.add("identifier");
        TOP_LEVEL_ELEMENT_ORDER.add("location");
        TOP_LEVEL_ELEMENT_ORDER.add("accessCondition");
    }

    private final MCRObject object;

    public MCRMODSWrapper() {
        this(new MCRObject());
    }

    public MCRMODSWrapper(MCRObject object) {
        this.object = object;
        if (object.getSchema() == null || object.getSchema().isEmpty()) {
            object.setSchema(MODS_DATAMODEL);
        }
    }

    private static int getRankOf(Element topLevelElement) {
        return TOP_LEVEL_ELEMENT_ORDER.indexOf(topLevelElement.getName());
    }

    public static MCRObject wrapMODSDocument(Element modsDefinition, String projectID) {
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        wrapper.setID(projectID, 0);
        wrapper.setMODS(modsDefinition);
        return wrapper.getMCRObject();
    }

    /**
     * returns true if the given MCRObject can handle MODS metadata
     * @param obj - the MCRObject
     * @return true, if mods is supported
     */
    public static boolean isSupported(MCRObject obj) {
        return isSupported(obj.getId())
            || (obj.getMetadata() != null && obj.getMetadata().getMetadataElement(DEF_MODS_CONTAINER) != null
                && obj.getMetadata().getMetadataElement(DEF_MODS_CONTAINER).getElementByName(MODS_CONTAINER) != null);
    }

    /**
     * Returns true of the given {@link MCRObjectID} has a mods type. Does not look at the object.
     * @param id - the {@link MCRObjectID}
     * @return true if has a mods type
     */
    public static boolean isSupported(MCRObjectID id) {
        return SUPPORTED_TYPES.contains(id.getTypeId());
    }

    private static String xPathRelationshipTypeTest() {
        return Stream.of(MCRMODSRelationshipType.values())
            .map(s -> String.format(Locale.ROOT, "@type='%s'", s.getValue()))
            .collect(Collectors.joining(" or "));
    }

    /**
     * @return the mods:mods Element at /metadata/def.modsContainer/modsContainer
     */
    public Element getMODS() {
        try {
            MCRMetaXML mx = (MCRMetaXML) (object.getMetadata().getMetadataElement(DEF_MODS_CONTAINER).getElement(0));
            for (Content content : mx.getContent()) {
                if (content instanceof Element element) {
                    return element;
                }
            }
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            //do nothing
        }
        return null;
    }

    public void setMODS(Element mods) {
        MCRObjectMetadata om = object.getMetadata();
        if (om.getMetadataElement(DEF_MODS_CONTAINER) != null) {
            om.removeMetadataElement(DEF_MODS_CONTAINER);
        }

        MCRMetaXML modsContainer = new MCRMetaXML(MODS_CONTAINER, null, 0);
        List<MCRMetaXML> list = Collections.nCopies(1, modsContainer);
        MCRMetaElement defModsContainer = new MCRMetaElement(MCRMetaXML.class, DEF_MODS_CONTAINER, false, true, list);
        om.setMetadataElement(defModsContainer);

        modsContainer.addContent(mods);
    }

    public MCRObject getMCRObject() {
        return object;
    }

    public MCRObjectID setID(String projectID, int id) {
        MCRObjectID objID = MCRObjectID.getInstance(MCRObjectID.formatID(projectID, MODS_OBJECT_TYPE, id));
        object.setId(objID);
        return objID;
    }

    private XPathExpression<Element> buildXPath(String xPath) {
        return XPathFactory.instance().compile(xPath, Filters.element(), null, MCRConstants.MODS_NAMESPACE,
            MCRConstants.XLINK_NAMESPACE);
    }

    public Element getElement(String xPath) {
        return buildXPath(xPath).evaluateFirst(getMODS());
    }

    public List<Element> getElements(String xPath) {
        Element eMODS = getMODS();
        if (eMODS != null) {
            return buildXPath(xPath).evaluate(eMODS);
        } else {
            return Collections.emptyList();
        }
    }

    public List<Element> getLinkedRelatedItems() {
        return getElements(LINKED_RELATED_ITEMS);
    }

    public String getElementValue(String xPath) {
        Element element = getElement(xPath);
        return (element == null ? null : element.getTextTrim());
    }

    /**
     * Sets or adds an element with target name and value. The element name and attributes are used as xpath expression
     * to filter for an element. The attributes are used with and operation if present.
     */
    public Optional<Element> setElement(String elementName, String elementValue, Map<String, String> attributes) {
        boolean isAttributeDataPresent = attributes != null && !attributes.isEmpty();
        boolean isValuePresent = elementValue != null && !elementValue.isEmpty();

        if (!isValuePresent && !isAttributeDataPresent) {
            return Optional.empty();
        }

        StringBuilder xPath = new StringBuilder("mods:");
        xPath.append(elementName);

        // add attributes to xpath with and operator
        if (isAttributeDataPresent) {

            xPath.append('[');
            Iterator<Map.Entry<String, String>> attributeIterator = attributes.entrySet().iterator();
            while (attributeIterator.hasNext()) {
                Map.Entry<String, String> attribute = attributeIterator.next();
                xPath.append('@').append(attribute.getKey()).append("='").append(attribute.getValue()).append('\'');

                if (attributeIterator.hasNext()) {
                    xPath.append(" and ");
                }
            }
            xPath.append(']');
        }
        Element element = getElement(xPath.toString());

        if (element == null) {
            element = addElement(elementName);
            if (isAttributeDataPresent) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    element.setAttribute(entry.getKey(), entry.getValue());
                }
            }
        }

        if (isValuePresent) {
            element.setText(elementValue.trim());
        }

        return Optional.of(element);
    }

    public Optional<Element> setElement(String elementName, String attributeName, String attributeValue,
        String elementValue) {
        Map<String, String> attributes = Collections.emptyMap();
        if (attributeName != null && attributeValue != null) {
            attributes = new HashMap<>();
            attributes.put(attributeName, attributeValue);
        }
        return setElement(elementName, elementValue, attributes);
    }

    public Optional<Element> setElement(String elementName, String elementValue) {
        return setElement(elementName, null, null, elementValue);
    }

    public Element addElement(String elementName) {
        Element element = new Element(elementName, MCRConstants.MODS_NAMESPACE);
        insertTopLevelElement(element);
        return element;
    }

    public void addElement(Element element) {
        if (!element.getNamespace().equals(MCRConstants.MODS_NAMESPACE)) {
            throw new IllegalArgumentException("given element is no mods element");
        }

        insertTopLevelElement(element);
    }

    private void insertTopLevelElement(Element element) {
        int rankOfNewElement = getRankOf(element);
        List<Element> topLevelElements = getMODS().getChildren();
        for (int pos = 0; pos < topLevelElements.size(); pos++) {
            if (getRankOf(topLevelElements.get(pos)) > rankOfNewElement) {
                getMODS().addContent(pos, element);
                return;
            }
        }

        getMODS().addContent(element);
    }

    public void removeElements(String xPath) {
        Iterator<Element> selected;
        selected = buildXPath(xPath).evaluate(getMODS()).iterator();

        while (selected.hasNext()) {
            Element element = selected.next();
            element.detach();
        }
    }

    public void removeInheritedMetadata() {
        String xPath = LINKED_RELATED_ITEMS + "/*[local-name()!='part']";
        removeElements(xPath);
    }

    public String getServiceFlag(String type) {
        MCRObjectService os = object.getService();
        return (os.isFlagTypeSet(type) ? os.getFlags(type).getFirst() : null);
    }

    public void setServiceFlag(String type, String value) {
        MCRObjectService os = object.getService();
        if (os.isFlagTypeSet(type)) {
            os.removeFlags(type);
        }
        if (value != null && !value.isBlank()) {
            os.addFlag(type, value.trim());
        }
    }

    public List<MCRCategoryID> getMcrCategoryIDs() {
        final List<Element> categoryNodes = getCategoryNodes();
        final List<MCRCategoryID> categories = new ArrayList<>(categoryNodes.size());
        for (Element node : categoryNodes) {
            final MCRCategoryID categoryID = MCRClassMapper.getCategoryID(node);
            if (categoryID != null) {
                categories.add(categoryID);
            }
        }
        return categories;
    }

    private List<Element> getCategoryNodes() {
        return getElements(
            "mods:typeOfResource | mods:accessCondition | .//*[(@authority or @authorityURI)"
                + " and not(ancestor::mods:relatedItem)]");
    }
}
