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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.metadata.MCRBase;

public class MCRPIClassificationXPathPredicate extends MCRPIPredicateBase
    implements MCRPICreationPredicate, MCRPIObjectRegistrationPredicate {

    private static final Logger LOGGER = LogManager.getLogger();

    private XPathExpression<Element> classificationBaseExpression;

    private XPathExpression<String> classificationIdExpression;

    private XPathExpression<String> categoryIdExpression;

    private XPathExpression<Boolean> expression;

    @MCRProperty(name = "BaseXPath")
    public void setClassificationBaseXPath(String xPath) {
        classificationBaseExpression = compileXpath(xPath, Filters.element());
    }

    @MCRProperty(name = "ClassIdXPath")
    public void setClassificationIdXPath(String xPath) {
        classificationIdExpression = compileXpath(xPath, Filters.fstring());
    }

    @MCRProperty(name = "CategIdXPath")
    public void setCategoryIdXPath(String xPath) {
        categoryIdExpression = compileXpath(xPath, Filters.fstring());
    }

    @MCRProperty(name = "XPath")
    public void setXPath(String xPath) {
        expression = compileXpath("boolean(" + xPath + ")", Filters.fboolean());
    }

    @Override
    public boolean test(MCRBase mcrBase) {
        MCRURIResolver resolver = MCRURIResolver.instance();
        return classificationBaseExpression.evaluate(mcrBase.createXML()).stream().anyMatch(baseElement -> {

            String classificationId = classificationIdExpression.evaluateFirst(baseElement);
            String categoryId = categoryIdExpression.evaluateFirst(baseElement);

            if (classificationId == null || categoryId == null) {
                return false;
            }

            String classificationUri = "classification:metadata:0:children:" + classificationId + ":" + categoryId;

            try {
                Document classificationDocument = new Document();
                classificationDocument.setRootElement(resolver.resolve(classificationUri));
                return expression.evaluateFirst(classificationDocument) == Boolean.TRUE;
            } catch (Exception e) {
                LOGGER.warn("Failed to load " + classificationUri, e);
                return false;
            }

        });
    }

}
