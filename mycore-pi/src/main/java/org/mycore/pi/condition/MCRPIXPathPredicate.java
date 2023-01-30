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

package org.mycore.pi.condition;

import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRBase;

public class MCRPIXPathPredicate extends MCRPIPredicateBase
    implements MCRPICreationPredicate, MCRPIObjectRegistrationPredicate {
    final private XPathExpression<Boolean> expr;

    public MCRPIXPathPredicate(String propertyPrefix) {
        super(propertyPrefix);
        final String xPath = "boolean(" + requireProperty("XPath") + ")";
        XPathFactory factory = XPathFactory.instance();
        expr = factory.compile(xPath, Filters.fboolean(), null, MCRConstants.getStandardNamespaces());
    }

    @Override
    public boolean test(MCRBase mcrBase) {
        return expr.evaluateFirst(mcrBase.createXML()) == Boolean.TRUE;
    }
}
