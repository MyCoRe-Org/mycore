package org.mycore.frontend.xeditor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom2.Namespace;
import org.jdom2.Parent;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.frontend.xeditor.jaxen.MCRJaxenXPathFactory;
import org.mycore.services.i18n.MCRTranslation;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXPathEvaluator {

    private final static Logger LOGGER = Logger.getLogger(MCRBinding.class);

    private final static Pattern PATTERN_XPATH = Pattern.compile("\\{([^\\}]+)\\}");

    private Map<String, Object> variables;

    private List<Object> context;

    public MCRXPathEvaluator(Map<String, Object> variables, Parent context) {
        this.variables = variables;
        this.context = new ArrayList<Object>();
        this.context.add(context);
    }

    public MCRXPathEvaluator(MCRBinding binding) {
        this.variables = binding.buildXPathVariables();
        this.context = binding.getBoundNodes();
    }

    public String replaceXPaths(String text, boolean urlEncode) {
        Matcher m = PATTERN_XPATH.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String replacement = replaceXPathOrI18n(m.group(1));
            if (urlEncode) {
                try {
                    replacement = URLEncoder.encode(replacement, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    throw new MCRException(ex);
                }
            }
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String replaceXPathOrI18n(String expression) {
        if (expression.startsWith("i18n:")) {
            String key = expression.substring(5);
            int pos = key.indexOf(",");
            if (pos != -1) {
                String xPath = key.substring(pos + 1);
                String value = evaluateXPath(xPath);
                key = key.substring(0, pos);
                return MCRTranslation.translate(key, value);
            } else
                return MCRTranslation.translate(key);
        } else
            return evaluateXPath(expression);
    }

    public String evaluateXPath(String xPathExpression) {
        xPathExpression = "string(" + xPathExpression + ")";
        Object result = evaluateFirst(xPathExpression);
        return result == null ? "" : (String) result;
    }

    public boolean test(String xPathExpression) {
        Object result = evaluateFirst(xPathExpression);
        if (result == null)
            return false;
        else if (result instanceof Boolean)
            return ((Boolean) result).booleanValue();
        else
            return true;
    }

    private final static XPathFactory factory = XPathFactory.newInstance(MCRJaxenXPathFactory.class.getName());

    public Object evaluateFirst(String xPathExpression) {
        try {
            List<Namespace> namespaces = MCRConstants.getStandardNamespaces();
            XPathExpression<Object> xPath = factory.compile(xPathExpression, Filters.fpassthrough(), variables, namespaces);
            return xPath.evaluateFirst(context);
        } catch (Exception ex) {
            LOGGER.warn("unable to evaluate XPath: " + xPathExpression);
            LOGGER.warn(ex.getCause());
            return null;
        }
    }

}
