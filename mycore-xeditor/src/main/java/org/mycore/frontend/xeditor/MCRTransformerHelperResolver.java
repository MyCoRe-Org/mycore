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

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.util.Strings;
import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.transform.JDOMSource;
import org.xml.sax.SAXException;

public class MCRTransformerHelperResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        StringTokenizer uriTokenizer = new StringTokenizer(href, ":");
        uriTokenizer.nextToken(); // remove schema

        String sessionID = uriTokenizer.nextToken();
        MCREditorSession session = MCREditorSessionStoreUtils.getSessionStore().getSession(sessionID);
        MCRTransformerHelper tfhelper = session.getTransformerHelper();

        String elementName = uriTokenizer.nextToken();
        Map<String, String> attributes = parseAttributes(uriTokenizer);

        Element result = new Element("result");
        try {
            switch (elementName) {
                case "form": {
                    registerAdditionalNamespaces(tfhelper, attributes);
                    break;
                }
                case "bind": {
                    registerAdditionalNamespaces(tfhelper, attributes);
                    tfhelper.bind(attributes);
                    break;
                }
                case "unbind": {
                    tfhelper.unbind();
                    break;
                }
                case "repeat": {
                    registerAdditionalNamespaces(tfhelper, attributes);
                    handleRepeat(tfhelper, attributes, result);
                    break;
                }
                case "controls": {
                    handleControls(tfhelper, attributes, result);
                    break;
                }
                case "input": {
                    handleInput(tfhelper, attributes, result);
                    break;
                }
                case "textarea": {
                    result.setAttribute("name", tfhelper.getAbsoluteXPath());
                    break;
                }
                case "if": {
                    handleTest(tfhelper, attributes, result);
                    break;
                }
                case "source": {
                    String uri = attributes.get("uri");
                    tfhelper.readSourceXML(uri);
                    break;
                }
                case "cancel": {
                    String url = attributes.get("url");
                    tfhelper.setCancelURL(url);
                    break;
                }
                case "post-processor": {
                    handlePostProcessor(tfhelper, attributes);
                    break;
                }
                case "param": {
                    handleParam(tfhelper, attributes);
                    break;
                }
                case "select": {
                    handleSelect(tfhelper, attributes, result);
                    break;
                }
                case "option": {
                    handleOption(tfhelper, attributes, result);
                    break;
                }
                case "cleanup-rule": {
                    handleCleanupRule(tfhelper, attributes);
                    break;
                }
                case "load-resource": {
                    handleLoadResource(tfhelper, attributes);
                    break;
                }
                case "output": {
                    handleOutput(tfhelper, attributes, result);
                    break;
                }
                default: {
                }
            }
        } catch (JDOMException | IOException | SAXException | JaxenException ex) {
            throw new TransformerException(ex);
        }

        JDOMSource source = new JDOMSource(result);
        // Workaround to prevent URI Caching:
        source.setSystemId(source.getSystemId() + Math.random());
        return source;
    }

    private void handleOutput(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        String value = attributes.getOrDefault("value", null);
        String i18n = attributes.getOrDefault("i18n", null);
        String output = tfhelper.output(value, i18n);
        result.setText(output);
    }

    private void handleLoadResource(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        String uri = attributes.get("uri");
        String name = attributes.get("name");
        tfhelper.loadResource(uri, name);
    }

    private void handleCleanupRule(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        String xPath = attributes.get("xpath");
        String relevantIf = attributes.get("relevant-if");
        tfhelper.addCleanupRule(xPath, relevantIf);
    }

    private void handleOption(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        if (tfhelper.isWithinSelectElement()) {
            String value = attributes.getOrDefault("value", attributes.get("text"));

            if ((!Strings.isEmpty(value)) && tfhelper.hasValue(value)) {
                result.setAttribute("selected", "selected");
            }
        }
    }

    private void handleSelect(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        String multiple = attributes.getOrDefault("multiple", null);
        tfhelper.toggleWithinSelectElement(multiple);

        if (tfhelper.isWithinSelectElement()) {
            setXPath(tfhelper, result, tfhelper.isWithinSelectMultiple());
        }
    }

    private void handleParam(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        String name = attributes.get("name");
        String def = attributes.getOrDefault("default", null);
        tfhelper.declareParameter(name, def);
    }

    private void handlePostProcessor(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        String clazz = attributes.getOrDefault("class", null);
        if (clazz != null) {
            tfhelper.setPostProcessor(clazz);
        }
        tfhelper.initializePostprocessor(attributes);
    }

    private void handleTest(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        String test = attributes.get("test");
        boolean testResult = tfhelper.test(test);
        result.setText(Boolean.toString(testResult));
    }

    private void handleInput(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        String type = attributes.get("type");

        setXPath(tfhelper, result, "checkbox".equals(type));

        if ("radio".equals(type) || "checkbox".equals(type)) {
            String value = attributes.get("value");
            if (tfhelper.hasValue(value)) {
                result.setAttribute("checked", "checked");
            }
        } else {
            result.setAttribute("value", tfhelper.getValue());
        }
    }

    private void handleControls(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result)
        throws JaxenException {
        int pos = tfhelper.getRepeatPosition();
        int num = tfhelper.getNumRepeats();
        int max = tfhelper.getMaxRepeats();

        String text = attributes.getOrDefault("text", "insert remove up down");
        for (String token : text.split("\\s+")) {
            if ("append".equals(token) && (pos < num)) {
                continue;
            }
            if ("up".equals(token) && (pos == 1)) {
                continue;
            }
            if ("down".equals(token) && (pos == num)) {
                continue;
            }
            if ("insert".equals(token) && (num == max)) {
                continue;
            }
            if ("append".equals(token) && (num == max)) {
                continue;
            }

            Element control = new Element("control").setText(token);

            StringBuilder name = new StringBuilder();
            name.append("_xed_submit_");
            name.append(token);
            name.append(':');

            if ("append".equals(token) || "insert".equals(token)) {
                name.append(tfhelper.getInsertParameter());
            } else if ("remove".equals(token)) {
                name.append(tfhelper.getAbsoluteXPath());
            } else if ("up".equals(token) || "down".equals(token)) {
                name.append(tfhelper.getSwapParameter(token));
            }

            name.append("|rep-");

            if ("remove".equals(token) && (pos > 1)) {
                name.append(tfhelper.previousAnchorID());
            } else {
                name.append(tfhelper.getAnchorID());
            }

            control.setAttribute("name", name.toString());
            result.addContent(control);
        }
    }

    private void handleRepeat(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result)
        throws JaxenException {
        String xPath = attributes.get("xpath");
        int minRepeats = Integer.parseInt(attributes.getOrDefault("min", "0"));
        int maxRepeats = Integer.parseInt(attributes.getOrDefault("max", "0"));
        String method = attributes.get("method");
        List<Element> repeats = tfhelper.repeat(xPath, minRepeats, maxRepeats, method);
        result.addContent(repeats);
    }

    private void setXPath(MCRTransformerHelper tfhelper, Element result, boolean fixPathForMultiple) {
        String xPath = tfhelper.getAbsoluteXPath();
        if (fixPathForMultiple && xPath.endsWith("[1]")) {
            xPath = xPath.substring(0, xPath.length() - 3);
        }
        result.setAttribute("name", xPath);
    }

    private void registerAdditionalNamespaces(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        attributes.forEach((key, value) -> {
            if (key.startsWith("xmlns:")) {
                String prefix = key.substring("xmlns:".length());
                tfhelper.addNamespace(prefix, attributes.get(key));
            }
        });
        attributes.keySet().removeIf(key -> key.startsWith("xmlns:"));
    }

    private Map<String, String> parseAttributes(StringTokenizer st) {
        Map<String, String> attributes = new HashMap<>();
        while (st.hasMoreTokens()) {
            String name = st.nextToken("=").substring(1);
            String value = st.nextToken("&").substring(1);
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
            attributes.put(name, value);
        }
        return attributes;
    }
}
