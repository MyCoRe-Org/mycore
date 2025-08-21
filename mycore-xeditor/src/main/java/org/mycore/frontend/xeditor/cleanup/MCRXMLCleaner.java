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

package org.mycore.frontend.xeditor.cleanup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

/**
 * <p>
 *   Removes irrelevant nodes from XML, for cleanup purposes after form submission.
 *   Rules can be configured as default rules or defined in the *.xed code.
 * </p>
 * 
 * <code>
 *   <xed:cleanup-rule xpath="//@*" relevant-if="string-length(.) &gt; 0" />
 * </code>
 * 
 * @see MCRDefaultRules
 * 
 * @author Frank Lu00FCtzenkirchen
 */
public class MCRXMLCleaner {

    private List<MCRCleaningRule> rules = new ArrayList<>();

    private Map<Object, MCRCleaningRule> nodes2rules = new HashMap<>();

    public MCRXMLCleaner() {
        rules.addAll(MCRDefaultRules.getDefaultRules());
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
        do {
            mapNodesToRules(clone);
        } while (clean(clone.getRootElement()));
        return clone;
    }

    private void mapNodesToRules(Document xml) {
        nodes2rules.clear();
        for (MCRCleaningRule rule : rules) {
            for (Object object : rule.getNodesToInspect(xml)) {
                nodes2rules.put(object, rule);
            }
        }
    }

    private boolean clean(Element element) {
        boolean changed = false;

        for (Iterator<Element> children = element.getChildren().iterator(); children.hasNext();) {
            Element child = children.next();
            if (clean(child)) {
                changed = true;
            }
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
