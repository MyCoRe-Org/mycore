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

package org.mycore.frontend.xeditor.jaxen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.Function;
import org.jaxen.XPath;
import org.jaxen.XPathFunctionContext;
import org.jdom2.Namespace;
import org.jdom2.filter.Filter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.mycore.common.MCRConstants;

/**
 * @author Frank Lützenkirchen
 */
public class MCRJaxenXPathFactory extends JaxenXPathFactory {

    private static final Logger LOGGER = LogManager.getLogger(MCRJaxenXPathFactory.class);

    private List<ExtensionFunction> functions = new ArrayList<>();

    public MCRJaxenXPathFactory() {
        super();
        functions.add(new ExtensionFunction("xed", "generate-id", new MCRFunctionGenerateID()));
        functions.add(new ExtensionFunction("xed", "call-java", new MCRFunctionCallJava()));
        functions.add(new ExtensionFunction("i18n", "translate", new MCRFunctionTranslate()));
    }

    @Override
    public <T> XPathExpression<T> compile(String expression, Filter<T> filter, Map<String, Object> variables,
        Namespace... namespaces) {
        XPathExpression<T> jaxenCompiled = super.compile(expression, filter, variables, namespaces);

        if (functions.stream().anyMatch(function -> function.isCalledIn(expression))) {
            addExtensionFunctions(jaxenCompiled, namespaces);
        }
        return jaxenCompiled;
    }

    private <T> void addExtensionFunctions(XPathExpression<T> jaxenCompiled, Namespace... namespaces) {
        try {
            XPath xPath = getXPath(jaxenCompiled);
            addExtensionFunctions(xPath);
        } catch (Exception ex) {
            LOGGER.warn(ex);
        }
    }

    private void addExtensionFunctions(XPath xPath) {
        XPathFunctionContext xfc = (XPathFunctionContext) (xPath.getFunctionContext());
        for (ExtensionFunction function : functions)
            function.register(xfc);
        xPath.setFunctionContext(xfc);
    }

    private <T> XPath getXPath(XPathExpression<T> jaxenCompiled) throws NoSuchFieldException, IllegalAccessException {
        Field xPathField = jaxenCompiled.getClass().getDeclaredField("xPath");
        xPathField.setAccessible(true);
        XPath xPath = (XPath) (xPathField.get(jaxenCompiled));
        xPathField.setAccessible(false);
        return xPath;
    }

    class ExtensionFunction {

        String prefix;

        String localName;

        Function function;

        public ExtensionFunction(String prefix, String localName, Function function) {
            this.prefix = prefix;
            this.localName = localName;
            this.function = function;
        }

        public void register(XPathFunctionContext context) {
            context.registerFunction(MCRConstants.getStandardNamespace(prefix).getURI(), localName, function);
        }

        public boolean isCalledIn(String xPathExpression) {
            return xPathExpression.contains(prefix + ":" + localName + "(");
        }
    }
}
