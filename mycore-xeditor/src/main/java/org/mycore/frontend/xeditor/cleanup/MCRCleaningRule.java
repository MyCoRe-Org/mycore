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

import java.util.List;

import org.jdom2.Document;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

/**
 * <p>
 *   Represents a rule that decides if a node should be removed from XML.
 *   Each rule consists of two XPath expressions:
 * </p>
 * <ul>
 *   <li>The XPath to select the nodes to inspect for this rule
 *   <li>The XPath expression to decide if this node is relevant or should be removed
 * </ul>
 * 
 * @author Frank L\u00FCtzenkirchen
 */
class MCRCleaningRule {

    /** The XPath to select the nodes to inspect for this rule */
    private String xPathExprNodesToInspect;

    /** The XPath to select the nodes to inspect for this rule */
    private XPathExpression<Object> xPathNodesToInspect;

    /** The XPath expression to decide if this node is relevant (or should be removed otherwise) */
    private XPathExpression<Object> xPathRelevancyTest;

    /**
     * Creates a new cleaning rule.
     * 
     * @param xPathExprNodesToInspect the XPath to select the nodes to inspect for this rule
     * @param xPathExprRelevancyTest the XPath expression to decide if this node is relevant (or otherwise be removed)
     */
    MCRCleaningRule(String xPathExprNodesToInspect, String xPathExprRelevancyTest) {
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
        if (found == null) {
            return false;
        } else if (found instanceof Boolean b) {
            return b;
        } else {
            return true; // something matching found
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MCRCleaningRule cleaningRule
            && xPathExprNodesToInspect.equals(cleaningRule.xPathExprNodesToInspect);
    }

    @Override
    public int hashCode() {
        return xPathExprNodesToInspect.hashCode();
    }
}
