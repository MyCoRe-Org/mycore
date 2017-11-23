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

package org.mycore.common.xml;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Parent;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXPathEvaluator {

    private static final Logger LOGGER = LogManager.getLogger(MCRXPathEvaluator.class);

    private static final Pattern PATTERN_XPATH = Pattern.compile("\\{([^\\}]+)\\}");

    private static final XPathFactory factory;

    private Map<String, Object> variables;

    private List<Object> context;

    static {
        String factoryClass = MCRConfiguration.instance().getString("MCR.XPathFactory.Class", null);
        factory = factoryClass == null ? XPathFactory.instance() : XPathFactory.newInstance(factoryClass);
    }

    public MCRXPathEvaluator(Map<String, Object> variables, List<Object> context) {
        this.variables = variables;
        this.context = context;
    }

    public MCRXPathEvaluator(Map<String, Object> variables, Parent context) {
        this.variables = variables;
        this.context = new ArrayList<>();
        this.context.add(context);
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
        expression = migrateLegacyI18nSyntaxToExtensionFunction(expression);
        return evaluateXPath(expression);
    }

    private String migrateLegacyI18nSyntaxToExtensionFunction(String expression) {
        if (expression.startsWith("i18n:")) {
            expression = expression.substring(5);
            if (expression.contains(",")) {
                int pos = expression.indexOf(",");
                String key = expression.substring(0, pos);
                String xPath = expression.substring(pos + 1);
                expression = "i18n:translate('" + key + "'," + xPath + ")";
            } else
                expression = "i18n:translate('" + expression + "')";
        }
        return expression;
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
            return (Boolean) result;
        else
            return true;
    }

    public Object evaluateFirst(String xPathExpression) {
        try {
            XPathExpression<Object> xPath = factory.compile(xPathExpression, Filters.fpassthrough(), variables,
                MCRConstants.getStandardNamespaces());
            return xPath.evaluateFirst(context);
        } catch (Exception ex) {
            LOGGER.warn("unable to evaluate XPath: {}", xPathExpression);
            LOGGER.warn("XPath factory used is {} {}", factory.getClass().getCanonicalName(),
                MCRConfiguration.instance().getString("MCR.XPathFactory.Class", null));
            LOGGER.warn(ex);
            return null;
        }
    }

}
