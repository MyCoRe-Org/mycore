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

package org.mycore.frontend.xeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

public class MCRXMLCleaner {

    private static final MCRCleaningRule REMOVE_EMPTY_ATTRIBUTES = new MCRCleaningRule("//@*", "string-length(.) > 0");

    private static final MCRCleaningRule REMOVE_EMPTY_ELEMENTS = new MCRCleaningRule("//*",
        "@* or * or (string-length(text()) > 0)");

    private static final MCRCleaningRule PRESERVE_STRUCTURE_AND_SERVICE = new MCRCleaningRule(
        "/mycoreobject/structure|/mycoreobject/service", "true()");

    private List<MCRCleaningRule> rules = new ArrayList<>();

    private Map<Object, MCRCleaningRule> nodes2rules = new HashMap<>();

    public MCRXMLCleaner() {
        addRule(REMOVE_EMPTY_ATTRIBUTES);
        addRule(REMOVE_EMPTY_ELEMENTS);
        addRule(PRESERVE_STRUCTURE_AND_SERVICE);
    }

    public void addRule(String xPathExprNodesToInspect, String xPathExprRelevancyTest) {
        addRule(new MCRCleaningRule(xPathExprNodesToInspect, xPathExprRelevancyTest));
    }

    public void addRule(MCRCleaningRule rule) {
        rules.remove(rule);
        rules.add(rule);
    }

    public Document clean(Document xml) {
        Document clone = xml.clone();
        do
            mapNodesToRules(clone);
        while (clean(clone.getRootElement()));
        return clone;
    }

    private void mapNodesToRules(Document xml) {
        nodes2rules.clear();
        for (MCRCleaningRule rule : rules)
            for (Object object : rule.getNodesToInspect(xml))
                nodes2rules.put(object, rule);
    }

    private boolean clean(Element element) {
        boolean changed = false;

        for (Iterator<Element> children = element.getChildren().iterator(); children.hasNext();) {
            Element child = children.next();
            if (clean(child))
                changed = true;
            if (!isRelevant(child)) {
                changed = true;
                children.remove();
            }
        }

        for (Iterator<Attribute> attributes = element.getAttributes().iterator(); attributes.hasNext();) {
            Attribute attribute = attributes.next();
            if (!isRelevant(attribute)) {
                changed = true;
                attributes.remove();
            }
        }

        return changed;
    }

    private boolean isRelevant(Object node) {
        MCRCleaningRule rule = nodes2rules.get(node);
        return (rule == null || rule.isRelevant(node));
    }
}

class MCRCleaningRule {

    private String xPathExprNodesToInspect;

    private XPathExpression<Object> xPathNodesToInspect;

    private XPathExpression<Object> xPathRelevancyTest;

    public MCRCleaningRule(String xPathExprNodesToInspect, String xPathExprRelevancyTest) {
        this.xPathExprNodesToInspect = xPathExprNodesToInspect;
        this.xPathNodesToInspect = XPathFactory.instance().compile(xPathExprNodesToInspect, Filters.fpassthrough(),
            null,
            MCRConstants.getStandardNamespaces());
        this.xPathRelevancyTest = XPathFactory.instance().compile(xPathExprRelevancyTest, Filters.fpassthrough(), null,
            MCRConstants.getStandardNamespaces());
    }

    public List<Object> getNodesToInspect(Document xml) {
        return xPathNodesToInspect.evaluate(xml);
    }

    public boolean isRelevant(Object node) {
        Object found = xPathRelevancyTest.evaluateFirst(node);
        if (found == null)
            return false;
        else if (found instanceof Boolean)
            return (Boolean) found;
        else
            return true; // something matching found
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRCleaningRule)
            return xPathExprNodesToInspect.equals(((MCRCleaningRule) obj).xPathExprNodesToInspect);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return xPathExprNodesToInspect.hashCode();
    }
}
