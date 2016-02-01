package org.mycore.frontend.xeditor.jaxen;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.log4j.Logger;
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

    private final static Logger LOGGER = Logger.getLogger(MCRJaxenXPathFactory.class);

    private final static String EXTENSION_FUNCTIONS_PREFX = "xed";

    private final static String EXTENSION_FUNCTIONS_URI = MCRConstants.getStandardNamespace(EXTENSION_FUNCTIONS_PREFX).getURI();

    public MCRJaxenXPathFactory() {
        super();
    }

    @Override
    public <T> XPathExpression<T> compile(String expression, Filter<T> filter, Map<String, Object> variables, Namespace... namespaces) {
        XPathExpression<T> jaxenCompiled = super.compile(expression, filter, variables, namespaces);
        if (expression.contains(EXTENSION_FUNCTIONS_PREFX))
            patchJaxenCompiled(jaxenCompiled, namespaces);
        return jaxenCompiled;
    }

    private <T> void patchJaxenCompiled(XPathExpression<T> jaxenCompiled, Namespace... namespaces) {
        try {
            XPath xPath = getXPath(jaxenCompiled);
            addExtensionFunctions(xPath);
        } catch (Exception ex) {
            LOGGER.warn(ex);
        }
    }

    private void addExtensionFunctions(XPath xPath) {
        XPathFunctionContext xfc = (XPathFunctionContext) (xPath.getFunctionContext());
        xfc.registerFunction(EXTENSION_FUNCTIONS_URI, "generate-id", new MCRFunctionGenerateID());
        xfc.registerFunction(EXTENSION_FUNCTIONS_URI, "call-java", new MCRFunctionCallJava());
        xfc.registerFunction(MCRConstants.getStandardNamespace("i18n").getURI(), "translate", new MCRFunctionTranslate());
        xPath.setFunctionContext(xfc);
    }

    private <T> XPath getXPath(XPathExpression<T> jaxenCompiled) throws NoSuchFieldException, IllegalAccessException {
        Field xPathField = jaxenCompiled.getClass().getDeclaredField("xPath");
        xPathField.setAccessible(true);
        XPath xPath = (XPath) (xPathField.get(jaxenCompiled));
        xPathField.setAccessible(false);
        return xPath;
    }
}
