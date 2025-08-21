package org.mycore.frontend.xeditor.cleanup;

import java.util.List;

import org.jdom2.Document;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

class MCRCleaningRule {

    private String xPathExprNodesToInspect;

    private XPathExpression<Object> xPathNodesToInspect;

    private XPathExpression<Object> xPathRelevancyTest;

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