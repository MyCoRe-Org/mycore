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
