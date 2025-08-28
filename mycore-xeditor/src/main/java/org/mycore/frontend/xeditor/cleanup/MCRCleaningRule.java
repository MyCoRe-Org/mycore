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
import java.util.Objects;

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
    private XPathExpression<Object> xPathExprOfNodesToInspect;

    /** The XPath expression to decide if this node is relevant (or should be removed otherwise) */
    private XPathExpression<Object> xPathExprOfRelevancyTest;

    /**
     * Creates a new cleaning rule.
     * 
     * @param xPathOfNodesToInspect the XPath to select the nodes to inspect for this rule
     * @param xPathOfRelevancyTest the XPath expression to decide if this node is relevant (or otherwise be removed)
     */
    MCRCleaningRule(String xPathOfNodesToInspect, String xPathOfRelevancyTest) {
        this.xPathExprOfNodesToInspect =
            XPathFactory.instance().compile(xPathOfNodesToInspect, Filters.fpassthrough(), null,
                MCRConstants.getStandardNamespaces());
        this.xPathExprOfRelevancyTest =
            XPathFactory.instance().compile(xPathOfRelevancyTest, Filters.fpassthrough(), null,
                MCRConstants.getStandardNamespaces());
    }

    public List<Object> getNodesToInspect(Document xml) {
        return xPathExprOfNodesToInspect.evaluate(xml);
    }

    public boolean isRelevant(Object node) {
        Object found = xPathExprOfRelevancyTest.evaluateFirst(node);
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
        return obj instanceof MCRCleaningRule other
            && this.xPathExprOfNodesToInspect.getExpression().equals(other.xPathExprOfNodesToInspect.getExpression())
            && this.xPathExprOfRelevancyTest.getExpression().equals(other.xPathExprOfRelevancyTest.getExpression());
    }

    @Override
    public int hashCode() {
        return Objects.hash(xPathExprOfNodesToInspect.getExpression(), xPathExprOfRelevancyTest.getExpression());
    }
}
