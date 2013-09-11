/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.xeditor;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Parent;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathFactory;
import org.mycore.frontend.xeditor.MCRXPathParser.MCRLiteral;
import org.mycore.frontend.xeditor.MCRXPathParser.MCRLocationStep;
import org.mycore.frontend.xeditor.MCRXPathParser.MCRXPath;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRNodeBuilder {

    private final static Logger LOGGER = Logger.getLogger(MCRNodeBuilder.class);

    public static Object build(String xPath, String value, Map<String, Object> variables, Parent parent) throws ParseException,
            JDOMException {
        MCRXPath path = MCRXPathParser.parse(xPath);
        return build(path, value, variables, parent);
    }

    public static Object build(MCRXPath xPath, String value, Map<String, Object> variables, Parent parent) throws ParseException,
            JDOMException {
        LOGGER.debug("build xPath " + xPath + " relative to " + MCRXPathBuilder.buildXPath(parent));

        List<MCRLocationStep> steps = xPath.getLocationSteps();
        int i, indexOfLastStep = steps.size() - 1;
        Object existingNode = null;

        for (i = indexOfLastStep; i >= 0; i--) {
            String path = xPath.buildXPathExpression(i);
            LOGGER.debug("testing existence of subpath " + path);
            existingNode = XPathFactory.instance().compile(path, Filters.fpassthrough(), variables, MCRUsedNamespaces.getNamespaces())
                    .evaluateFirst(parent);
            LOGGER.debug("Result is " + existingNode);
            MCRLocationStep currentStep = xPath.getLocationSteps().get(i);

            if (existingNode instanceof Element) {
                LOGGER.debug("element already existing.");
                parent = (Element) existingNode;
                break;
            } else if (existingNode instanceof Attribute) {
                LOGGER.debug("attribute already existing.");
                break;
            } else if ((existingNode instanceof Boolean) && ((Boolean) existingNode).booleanValue()
                    && (currentStep.getAssignedValue() != null)) {
                LOGGER.debug("subpath already existing, but is boolean true: " + path);
                transformValueToAdditionalPredicate(currentStep);
                path = xPath.buildXPathExpression(i);
                LOGGER.debug("subpath with value transformed to predicate: " + path);
                existingNode = XPathFactory.instance().compile(path, Filters.fpassthrough(), null, MCRUsedNamespaces.getNamespaces())
                        .evaluateFirst(parent);
                break;

            } else
                LOGGER.debug("subpath does not exist or is not a node, ignoring: " + path);
        }

        if (i == indexOfLastStep)
            return existingNode;
        else
            return build(steps.subList(i + 1, steps.size()), value, variables, parent);

    }

    private static void transformValueToAdditionalPredicate(MCRLocationStep currentStep) throws ParseException {
        MCRLiteral currentValue = (MCRLiteral) (currentStep.getAssignedValue());
        currentStep.setValue(null);
        String nPath = ".=" + currentValue.getDelimiter() + currentValue.getValue() + currentValue.getDelimiter();
        MCRXPath nxp = MCRXPathParser.parse(nPath);
        currentStep.getPredicates().add(nxp);
    }

    private static Object build(List<MCRLocationStep> locationSteps, String value, Map<String, Object> variables, Parent parent)
            throws ParseException, JDOMException {
        Object node = null;

        for (int i = 0; i < locationSteps.size(); i++) {
            MCRLocationStep step = locationSteps.get(i);
            if (!canBeBuilt(step)) {
                LOGGER.debug("location step can not be built, breaking build: " + step);
                break;
            }

            String valueToSet = i == locationSteps.size() - 1 ? value : null;
            node = build(step, valueToSet, variables, parent);
            if (node instanceof Element)
                parent = (Element) node;
        }
        return node;
    }

    private static boolean canBeBuilt(MCRLocationStep locationStep) {
        String name = locationStep.getLocalName();

        if (name.matches("[0-9]+"))
            return false;
        if (name.contains("(") || name.contains("*") || name.startsWith(".") || name.contains("|") || name.contains(":"))
            return false;

        return true;
    }

    private static Object build(MCRLocationStep locationStep, String value, Map<String, Object> variables, Parent parent)
            throws ParseException, JDOMException {
        LOGGER.debug("build location step " + locationStep + " relative to " + MCRXPathBuilder.buildXPath(parent));

        if (locationStep.getAssignedValue() != null)
            value = locationStep.getAssignedValue().getValue();

        Namespace ns = locationStep.getNamespace();
        String name = locationStep.getLocalName();

        if (locationStep.isAttribute()) {
            return buildAttribute(ns, name, value, (Element) parent);
        } else {
            Element element = parent instanceof Document ? ((Document) parent).getRootElement() : buildElement(ns, name, value, parent);
            for (MCRXPath predicate : locationStep.getPredicates())
                build(predicate, null, variables, element);
            return element;
        }
    }

    private static Attribute buildAttribute(Namespace ns, String name, String value, Element parent) {
        if (value == null)
            value = "";
        Attribute attribute = new Attribute(name, value, ns);
        if (parent != null)
            parent.setAttribute(attribute);
        return attribute;
    }

    private static Element buildElement(Namespace ns, String name, String value, Parent parent) {
        Element element = new Element(name, ns);
        if ((value != null) && (!value.isEmpty()))
            element.setText(value);

        if (parent instanceof Document)
            ((Document) parent).setRootElement(element);
        else if (parent != null)
            parent.addContent(element);

        return element;
    }
}
