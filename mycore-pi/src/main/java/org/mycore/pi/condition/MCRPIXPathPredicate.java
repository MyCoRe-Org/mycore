package org.mycore.pi.condition;

import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRBase;

public class MCRPIXPathPredicate extends MCRPIPredicateBase
    implements MCRPICreationPredicate, MCRPIObjectRegistrationPredicate {
    public MCRPIXPathPredicate(String propertyPrefix) {
        super(propertyPrefix);
        final String xPath = requireProperty("XPath");
        XPathFactory factory = XPathFactory.instance();
        expr = factory.compile(xPath, Filters.fpassthrough(), null, MCRConstants.getStandardNamespaces());
    }

    final private XPathExpression<Object> expr;

    @Override
    public boolean test(MCRBase mcrBase) {
        return expr.evaluate(mcrBase.createXML()).size() > 0;
    }
}
