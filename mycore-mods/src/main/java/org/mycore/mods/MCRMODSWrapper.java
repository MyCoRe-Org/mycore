/*
 * $Revision: 5697 $ $Date: 07.04.2011 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaXML;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectMetadata;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.mods.classification.MCRClassMapper;

/**
 * @author Frank L\u00FCtzenkirchen
 * @author Thomas Scheffler
 */
public class MCRMODSWrapper {

    //ancestor::mycoreobject required for MCR-927
    private static final String LINKED_RELATED_ITEMS = "mods:relatedItem[@type='host' and ancestor::mycoreobject/structure/parents/parent or"
        + " contains(@xlink:href,'_mods_') and"
        + " number(substring-after(@xlink:href,'_mods_')) > 0 and"
        + " contains('"
        + MCRMODSRelationshipType.xPathList() + "', @type)]";

    private static final String MODS_CONTAINER = "modsContainer";

    private static final String DEF_MODS_CONTAINER = "def.modsContainer";

    public static final String MODS_OBJECT_TYPE = "mods";

    private static final String MODS_DATAMODEL = "datamodel-mods.xsd";

    private static List<String> topLevelElementOrder = new ArrayList<String>();

    static {
        topLevelElementOrder.add("titleInfo");
        topLevelElementOrder.add("name");
        topLevelElementOrder.add("originInfo");
        topLevelElementOrder.add("language");
        topLevelElementOrder.add("abstract");
        topLevelElementOrder.add("note");
        topLevelElementOrder.add("subject");
        topLevelElementOrder.add("classification");
        topLevelElementOrder.add("relatedItem");
        topLevelElementOrder.add("identifier");
        topLevelElementOrder.add("location");
        topLevelElementOrder.add("accessCondition");
    }

    private static int getRankOf(Element topLevelElement) {
        return topLevelElementOrder.indexOf(topLevelElement.getName());
    }

    public static MCRObject wrapMODSDocument(Element modsDefinition, String projectID) {
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        wrapper.setID(projectID, 0);
        wrapper.setMODS(modsDefinition);
        return wrapper.getMCRObject();
    }

    private MCRObject object;

    public MCRMODSWrapper() {
        this(new MCRObject());
    }

    public MCRMODSWrapper(MCRObject object) {
        this.object = object;
        object.setSchema(MODS_DATAMODEL);
    }

    public Element getMODS() {
        MCRMetaXML mx = (MCRMetaXML) (object.getMetadata().getMetadataElement(DEF_MODS_CONTAINER).getElement(0));
        for (Content content : mx.getContent())
            if (content instanceof Element)
                return (Element) content;
        return null;
    }

    public MCRObject getMCRObject() {
        return object;
    }

    public MCRObjectID setID(String projectID, int ID) {
        MCRObjectID objID = MCRObjectID.getInstance(MCRObjectID.formatID(projectID, MODS_OBJECT_TYPE, ID));
        object.setId(objID);
        return objID;
    }

    public void setMODS(Element mods) {
        MCRObjectMetadata om = object.getMetadata();
        if (om.getMetadataElement(DEF_MODS_CONTAINER) != null)
            om.removeMetadataElement(DEF_MODS_CONTAINER);

        MCRMetaXML modsContainer = new MCRMetaXML(MODS_CONTAINER, null, 0);
        List<MCRMetaXML> list = Collections.nCopies(1, modsContainer);
        MCRMetaElement defModsContainer = new MCRMetaElement(MCRMetaXML.class, DEF_MODS_CONTAINER, false, true, list);
        om.setMetadataElement(defModsContainer);

        modsContainer.addContent(mods);
    }

    private XPathExpression<Element> buildXPath(String xPath) throws JDOMException {
        return XPathFactory.instance().compile(xPath, Filters.element(), null, MCRConstants.MODS_NAMESPACE,
            MCRConstants.XLINK_NAMESPACE);
    }

    public Element getElement(String xPath) {
        try {
            return buildXPath(xPath).evaluateFirst(getMODS());
        } catch (JDOMException ex) {
            String msg = "Could not get MODS element from " + xPath;
            throw new MCRException(msg, ex);
        }
    }

    public List<Element> getElements(String xPath) {
        try {
            return buildXPath(xPath).evaluate(getMODS());
        } catch (JDOMException ex) {
            String msg = "Could not get elements at " + xPath;
            throw new MCRException(msg, ex);
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

            xPath.append("[");
            Iterator<Map.Entry<String, String>> attributeIterator = attributes.entrySet().iterator();
            while (attributeIterator.hasNext()) {
                Map.Entry<String, String> attribute = attributeIterator.next();
                xPath.append("@" + attribute.getKey() + "='" + attribute.getValue() + "']");

                if (attributeIterator.hasNext())
                    xPath.append(" and ");
            }
        }
        Element element = getElement(xPath.toString());

        if (element == null) {
            element = addElement(elementName);
            if (isAttributeDataPresent) {
                for (Map.Entry<String, String> entry : attributes.entrySet())
                    element.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        if (isValuePresent)
            element.setText(elementValue.trim());

        return Optional.of(element);
    }

    public Optional<Element> setElement(String elementName, String attributeName, String attributeValue, String elementValue) {
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
        if (!element.getNamespace().equals(MCRConstants.MODS_NAMESPACE))
            throw new IllegalArgumentException("given element is no mods element");

        insertTopLevelElement(element);
    }

    private void insertTopLevelElement(Element element) {
        int rankOfNewElement = getRankOf(element);
        List<Element> topLevelElements = getMODS().getChildren();
        for (int pos = 0; pos < topLevelElements.size(); pos++)
            if (getRankOf(topLevelElements.get(pos)) > rankOfNewElement) {
                getMODS().addContent(pos, element);
                return;
            }

        getMODS().addContent(element);
    }

    public void removeElements(String xPath) {
        Iterator<Element> selected;
        try {
            selected = buildXPath(xPath).evaluate(getMODS()).iterator();
        } catch (JDOMException ex) {
            String msg = "Could not remove elements at " + xPath;
            throw new MCRException(msg, ex);
        }

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
        return (os.isFlagTypeSet(type) ? os.getFlags(type).get(0) : null);
    }

    public void setServiceFlag(String type, String value) {
        MCRObjectService os = object.getService();
        if (os.isFlagTypeSet(type))
            os.removeFlags(type);
        if ((value != null) && !value.trim().isEmpty())
            os.addFlag(type, value.trim());
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
        return getElements("mods:typeOfResource | mods:accessCondition | .//*[(@authority or @authorityURI) and not(ancestor::mods:relatedItem[@type='host'])]");
    }
}
