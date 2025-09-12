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
import java.util.ArrayList;
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

    private static final String CONTROL_INSERT = "insert";
    private static final String CONTROL_APPEND = "append";
    private static final String CONTROL_REMOVE = "remove";
    private static final String CONTROL_UP = "up";
    private static final String CONTROL_DOWN = "down";

    private static final String DEFAULT_CONTROLS =
        String.join(" ", CONTROL_INSERT, CONTROL_REMOVE, CONTROL_UP, CONTROL_DOWN);

    private static final String ATTR_URL = "url";
    private static final String ATTR_URI = "uri";
    private static final String ATTR_REF = "ref";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_XPATH = "xpath";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_TEXT = "text";
    private static final String ATTR_I18N = "i18n";
    private static final String ATTR_HREF = "xed:href";
    private static final String ATTR_TARGET = "xed:target";
    private static final String ATTR_STYLE = "style";
    private static final String ATTR_RELEVANT_IF = "relevant-if";
    private static final String ATTR_MULTIPLE = "multiple";
    private static final String ATTR_DEFAULT = "default";
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_TEST = "test";
    private static final String ATTR_METHOD = "method";
    private static final String ATTR_MAX = "max";
    private static final String ATTR_MIN = "min";

    private static final String TYPE_CHECKBOX = "checkbox";
    private static final String TYPE_RADIO = "radio";

    private static final String VALUE_SELECTED = "selected";
    private static final String VALUE_CHECKED = "checked";

    private static final String PREDICATE_IS_FIRST = "[1]";

    private static final String PREFIX_XMLNS = "xmlns:";

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
            handleXEditorElement(tfhelper, elementName, attributes, result);
        } catch (JDOMException | IOException | SAXException | JaxenException ex) {
            throw new TransformerException(ex);
        }

        JDOMSource source = new JDOMSource(result);
        // Workaround to prevent URI Caching:
        source.setSystemId(source.getSystemId() + Math.random());
        return source;
    }

    @SuppressWarnings("PMD.NcssCount")
    private void handleXEditorElement(MCRTransformerHelper tfhelper, String elementName, Map<String, String> attributes,
        Element result) throws JaxenException, JDOMException, IOException, SAXException, TransformerException {
        switch (elementName) {
            case "form":
                handleForm(tfhelper, attributes);
                break;
            case "preload":
                handlePreload(tfhelper, attributes, result);
                break;
            case "include":
                handleInclude(tfhelper, attributes, result);
                break;
            case "getAdditionalParameters":
                handleGetAdditionalParameters(tfhelper, result);
                break;
            case "bind":
                handleBind(tfhelper, attributes);
                break;
            case "unbind":
                handleUnbind(tfhelper);
                break;
            case "repeat":
                handleRepeat(tfhelper, attributes, result);
                break;
            case "bindRepeatPosition":
                handleBindRepeatPosition(tfhelper, result);
                break;
            case "controls":
                handleControls(tfhelper, attributes, result);
                break;
            case "input":
                handleInput(tfhelper, attributes, result);
                break;
            case "textarea":
                handleTextarea(tfhelper, result);
                break;
            case "button":
                handleSubmitButton(tfhelper, attributes, result);
                break;
            case "if":
            case "when":
                handleTest(tfhelper, attributes, result);
                break;
            case "source":
                handleSource(tfhelper, attributes);
                break;
            case "cancel":
                handleCancel(tfhelper, attributes);
                break;
            case "post-processor":
                handlePostProcessor(tfhelper, attributes);
                break;
            case "param":
                handleParam(tfhelper, attributes);
                break;
            case "select":
                handleSelect(tfhelper, attributes, result);
                break;
            case "option":
                handleOption(tfhelper, attributes, result);
                break;
            case "cleanup-rule":
                handleCleanupRule(tfhelper, attributes);
                break;
            case "load-resource":
                handleLoadResource(tfhelper, attributes);
                break;
            case "output":
                handleOutput(tfhelper, attributes, result);
                break;
            case "displayValidationMessage":
                handleDisplayValidationMessage(tfhelper, result);
                break;
            case "displayValidationMessages":
                handleDisplayValidationMessages(tfhelper, result);
                break;
            default:
                ;
        }
    }

    private void handleDisplayValidationMessages(MCRTransformerHelper tfhelper, Element result) {
        result.addContent(new ArrayList<>(tfhelper.getFailedValidationRules()));
    }

    private void handleDisplayValidationMessage(MCRTransformerHelper tfhelper, Element result) {
        if (tfhelper.hasValidationError()) {
            result.addContent(tfhelper.getFailedValidationRule().clone());
        }
    }

    private void handleUnbind(MCRTransformerHelper tfhelper) {
        tfhelper.unbind();
    }

    private void handleForm(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        registerAdditionalNamespaces(tfhelper, attributes);
    }

    private void handleSubmitButton(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        String target = attributes.get(ATTR_TARGET);
        String href = attributes.get(ATTR_HREF);

        StringBuilder name = new StringBuilder();
        name.append("_xed_submit_").append(target);

        if ("subselect".equals(target)) {
            name.append(':').append(tfhelper.getSubselectParam(href));
        } else if (Strings.isNotBlank(href)) {
            name.append(':').append(href);
        }

        result.setAttribute(ATTR_NAME, name.toString());
    }

    private void handleBindRepeatPosition(MCRTransformerHelper tfhelper, Element result) {
        Element anchor = new Element("a");
        String id = "rep-" + tfhelper.bindRepeatPosition();
        anchor.setAttribute("id", id);
        result.addContent(anchor);
    }

    private void handlePreload(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        replaceParameters(tfhelper, attributes, result, ATTR_URI);
    }

    private void handleInclude(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        replaceParameters(tfhelper, attributes, result, ATTR_URI, ATTR_REF);
    }

    private void replaceParameters(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result,
        String... attributesToHandle) {
        for (String attribute : attributesToHandle) {
            if (attributes.containsKey(attribute)) {
                result.setAttribute(attribute, tfhelper.replaceParameters(attributes.get(attribute)));
            }
        }
    }

    private void handleGetAdditionalParameters(MCRTransformerHelper tfhelper, Element result) {
        Element div = new Element("div").setAttribute(ATTR_STYLE, "visibility:hidden");
        div.addContent(tfhelper.getAdditionalParameters());
        result.addContent(div);
    }

    private void handleBind(MCRTransformerHelper tfhelper, Map<String, String> attributes) throws JaxenException {
        handleForm(tfhelper, attributes);
        tfhelper.bind(attributes);
    }

    private void handleTextarea(MCRTransformerHelper tfhelper, Element result) {
        result.setAttribute(ATTR_NAME, tfhelper.getAbsoluteXPath());

        String value = tfhelper.getValue();
        if (value != null) {
            result.setText(value);
        }
    }

    private void handleSource(MCRTransformerHelper tfhelper, Map<String, String> attributes)
        throws JDOMException, IOException, SAXException, TransformerException {
        String uri = attributes.get(ATTR_URI);
        tfhelper.readSourceXML(uri);
    }

    private void handleCancel(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        String url = attributes.get(ATTR_URL);
        tfhelper.setCancelURL(url);
    }

    private void handleOutput(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        String value = attributes.getOrDefault(ATTR_VALUE, null);
        String i18n = attributes.getOrDefault(ATTR_I18N, null);
        String output = tfhelper.output(value, i18n);
        result.setText(output);
    }

    private void handleLoadResource(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        String uri = attributes.get(ATTR_URI);
        String name = attributes.get(ATTR_NAME);
        tfhelper.loadResource(uri, name);
    }

    private void handleCleanupRule(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        String xPath = attributes.get(ATTR_XPATH);
        String relevantIf = attributes.get(ATTR_RELEVANT_IF);
        tfhelper.addCleanupRule(xPath, relevantIf);
    }

    private void handleOption(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        if (tfhelper.isWithinSelectElement()) {
            String value = attributes.getOrDefault(ATTR_VALUE, attributes.get(ATTR_TEXT));

            if ((!Strings.isEmpty(value)) && tfhelper.hasValue(value)) {
                result.setAttribute(VALUE_SELECTED, VALUE_SELECTED);
            }
        }
    }

    private void handleSelect(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        String multiple = attributes.getOrDefault(ATTR_MULTIPLE, null);
        tfhelper.toggleWithinSelectElement(multiple);

        if (tfhelper.isWithinSelectElement()) {
            setXPath(tfhelper, result, tfhelper.isWithinSelectMultiple());
        }
    }

    private void handleParam(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        String name = attributes.get(ATTR_NAME);
        String def = attributes.getOrDefault(ATTR_DEFAULT, null);
        tfhelper.declareParameter(name, def);
    }

    private void handlePostProcessor(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        String clazz = attributes.getOrDefault(ATTR_CLASS, null);
        if (clazz != null) {
            tfhelper.setPostProcessor(clazz);
        }
        tfhelper.initializePostprocessor(attributes);
    }

    private void handleTest(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        String test = attributes.get(ATTR_TEST);
        boolean testResult = tfhelper.test(test);
        result.setText(Boolean.toString(testResult));
    }

    private void handleInput(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result) {
        String type = attributes.get(ATTR_TYPE);

        setXPath(tfhelper, result, TYPE_CHECKBOX.equals(type));

        if (TYPE_RADIO.equals(type) || TYPE_CHECKBOX.equals(type)) {
            String value = attributes.get(ATTR_VALUE);
            if (tfhelper.hasValue(value)) {
                result.setAttribute(VALUE_CHECKED, VALUE_CHECKED);
            }
        } else {
            result.setAttribute(ATTR_VALUE, tfhelper.getValue());
        }
    }

    private void handleControls(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result)
        throws JaxenException {
        int pos = tfhelper.getRepeatPosition();
        int num = tfhelper.getNumRepeats();
        int max = tfhelper.getMaxRepeats();

        String text = attributes.getOrDefault(ATTR_TEXT, DEFAULT_CONTROLS);
        for (String token : text.split("\\s+")) {
            if ((CONTROL_APPEND.equals(token) && (pos < num)) ||
                (CONTROL_UP.equals(token) && (pos == 1)) ||
                (CONTROL_DOWN.equals(token) && (pos == num)) ||
                ((CONTROL_INSERT.equals(token) || CONTROL_APPEND.equals(token)) && (num == max))) {
                continue;
            }

            Element control = new Element("control").setText(token);

            StringBuilder name = new StringBuilder();
            name.append("_xed_submit_").append(token).append(':');

            if (CONTROL_APPEND.equals(token) || CONTROL_INSERT.equals(token)) {
                name.append(tfhelper.getInsertParameter());
            } else if (CONTROL_REMOVE.equals(token)) {
                name.append(tfhelper.getAbsoluteXPath());
            } else if (CONTROL_UP.equals(token) || CONTROL_DOWN.equals(token)) {
                name.append(tfhelper.getSwapParameter(token));
            }

            name.append("|rep-");

            if (CONTROL_REMOVE.equals(token) && (pos > 1)) {
                name.append(tfhelper.previousAnchorID());
            } else {
                name.append(tfhelper.getAnchorID());
            }

            control.setAttribute(ATTR_NAME, name.toString());
            result.addContent(control);
        }
    }

    private void handleRepeat(MCRTransformerHelper tfhelper, Map<String, String> attributes, Element result)
        throws JaxenException {
        handleForm(tfhelper, attributes);

        String xPath = attributes.get(ATTR_XPATH);
        int minRepeats = Integer.parseInt(attributes.getOrDefault(ATTR_MIN, "0"));
        int maxRepeats = Integer.parseInt(attributes.getOrDefault(ATTR_MAX, "0"));
        String method = attributes.get(ATTR_METHOD);
        List<Element> repeats = tfhelper.repeat(xPath, minRepeats, maxRepeats, method);
        result.addContent(repeats);
    }

    private void setXPath(MCRTransformerHelper tfhelper, Element result, boolean fixPathForMultiple) {
        String xPath = tfhelper.getAbsoluteXPath();
        if (fixPathForMultiple && xPath.endsWith(PREDICATE_IS_FIRST)) {
            xPath = xPath.substring(0, xPath.length() - 3);
        }
        result.setAttribute(ATTR_NAME, xPath);
    }

    private void registerAdditionalNamespaces(MCRTransformerHelper tfhelper, Map<String, String> attributes) {
        attributes.forEach((key, value) -> {
            if (key.startsWith(PREFIX_XMLNS)) {
                String prefix = key.substring(PREFIX_XMLNS.length());
                tfhelper.addNamespace(prefix, attributes.get(key));
            }
        });
        attributes.keySet().removeIf(key -> key.startsWith(PREFIX_XMLNS));
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
