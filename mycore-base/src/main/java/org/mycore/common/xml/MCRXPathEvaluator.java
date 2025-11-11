/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import org.mycore.common.config.MCRConfiguration2;

/**
 * @author Frank Lützenkirchen
 */
public class MCRXPathEvaluator {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern PATTERN_XPATH = Pattern.compile("\\{([^\\}]+)\\}");

    private static final XPathFactory XPATH_FACTORY = MCRConfiguration2.getString("MCR.XPathFactory.Class")
        .map(XPathFactory::newInstance)
        .orElseGet(XPathFactory::instance);

    private Map<String, Object> variables;

    private List<Object> context;

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
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String replacement = replaceXPathOrI18n(m.group(1));
            if (urlEncode) {
                replacement = URLEncoder.encode(replacement, StandardCharsets.UTF_8);
            }
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String replaceXPathOrI18n(String expression) {
        return evaluateFirstAsString(migrateLegacyI18nSyntaxToExtensionFunction(expression));
    }

    private String migrateLegacyI18nSyntaxToExtensionFunction(String expression) {
        String updatedI18nExpression = expression;
        if (expression.startsWith("i18n:")) {
            updatedI18nExpression = expression.substring(5);
            if (updatedI18nExpression.contains(",")) {
                int pos = updatedI18nExpression.indexOf(',');
                String key = updatedI18nExpression.substring(0, pos);
                String xPath = updatedI18nExpression.substring(pos + 1);
                updatedI18nExpression = "i18n:translate('" + key + "'," + xPath + ")";
            } else {
                updatedI18nExpression = "i18n:translate('" + updatedI18nExpression + "')";
            }
        }
        return updatedI18nExpression;
    }

    public String evaluateFirstAsString(String xPathExpression) {
        Object result = evaluateFirst("string(" + xPathExpression + ")");
        return result == null ? "" : (String) result;
    }

    public List<String> evaluateAllAsString(String xPathExpression) {
        List<Object> result = evaluateAll("string(" + xPathExpression + ")");
        return result.stream().map(Object::toString).toList();
    }

    public boolean test(String xPathExpression) {
        Object result = evaluateFirst(xPathExpression);
        if (result == null) {
            return false;
        } else if (result instanceof Boolean b) {
            return b;
        } else {
            return true;
        }
    }

    public Object evaluateFirst(String xPathExpression) {
        try {
            XPathExpression<Object> xPath = XPATH_FACTORY.compile(xPathExpression, Filters.fpassthrough(), variables,
                MCRConstants.getStandardNamespaces());
            return xPath.evaluateFirst(context);
        } catch (Exception ex) {
            LOGGER.warn("unable to evaluate XPath: {}", xPathExpression);
            LOGGER.warn("XPath factory used is {} {}", () -> XPATH_FACTORY.getClass().getCanonicalName(),
                () -> MCRConfiguration2.getString("MCR.XPathFactory.Class").orElse(null));
            LOGGER.warn(ex);
            return null;
        }
    }

    public List<Object> evaluateAll(String xPathExpression) {
        try {
            XPathExpression<Object> xPath = XPATH_FACTORY.compile(xPathExpression, Filters.fpassthrough(), variables,
                    MCRConstants.getStandardNamespaces());
            return xPath.evaluate(context);
        } catch (Exception ex) {
            LOGGER.warn("unable to evaluate XPath: {}", xPathExpression);
            LOGGER.warn("XPath factory used is {} {}", () -> XPATH_FACTORY.getClass().getCanonicalName(),
                    () -> MCRConfiguration2.getString("MCR.XPathFactory.Class").orElse(null));
            LOGGER.warn(ex);
            return List.of();
        }
    }

}
