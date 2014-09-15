package org.mycore.frontend.xeditor.jaxen;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.XPathFunctionContext;
import org.jdom2.Namespace;
import org.jdom2.filter.Filter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;

public class MCRJaxenXPathFactory extends JaxenXPathFactory {

    private final static Logger LOGGER = Logger.getLogger(MCRJaxenXPathFactory.class);

    private final static String EXTENSION_FUNCTIONS_PREFX = "xedf";

    private final static String EXTENSION_FUNCTIONS_URI = "http://www.mycore.org/xed-functions";

    public MCRJaxenXPathFactory() {
        super();
    }

    @Override
    public <T> XPathExpression<T> compile(String expression, Filter<T> filter, Map<String, Object> variables, Namespace... namespaces) {
        XPathExpression<T> jaxenCompiled = super.compile(expression, filter, variables, namespaces);
        try {
            Field xPathField = jaxenCompiled.getClass().getDeclaredField("xPath");
            xPathField.setAccessible(true);
            XPath xPath = (XPath) (xPathField.get(jaxenCompiled));
            xPathField.setAccessible(false);

            SimpleNamespaceContext nc = new SimpleNamespaceContext();
            nc.addNamespace(EXTENSION_FUNCTIONS_PREFX, EXTENSION_FUNCTIONS_URI);
            if (namespaces.length > 0)
                for (int i = 0; i < namespaces.length; i++)
                    nc.addNamespace(namespaces[i].getPrefix(), namespaces[i].getURI());

            xPath.setNamespaceContext(nc);

            XPathFunctionContext xfc = (XPathFunctionContext) (xPath.getFunctionContext());
            xfc.registerFunction(EXTENSION_FUNCTIONS_URI, "generate-id", new MCRFunctionGenerateID());
            xfc.registerFunction(EXTENSION_FUNCTIONS_URI, "call-java", new MCRFunctionCallJava());
            xPath.setFunctionContext(xfc);

            return jaxenCompiled;
        } catch (Exception ex) {
            LOGGER.warn(ex);
            return jaxenCompiled;
        }
    }
}
