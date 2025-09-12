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

package org.mycore.frontend.xeditor.transformer;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRConstants;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCREditorSessionStoreUtils;
import org.xml.sax.SAXException;

public class MCRTransformerHelperResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        MCRTransformerHelperCall call = new MCRTransformerHelperCall(href);

        try {
            handleXEditorElement(call);
        } catch (JDOMException | IOException | SAXException | JaxenException ex) {
            throw new TransformerException(ex);
        }

        JDOMSource source = new JDOMSource(call.getReturnElement());
        // Workaround to prevent URI Caching:
        source.setSystemId(source.getSystemId() + Math.random());
        return source;
    }

    @SuppressWarnings("PMD.NcssCount")
    private void handleXEditorElement(MCRTransformerHelperCall call)
        throws JaxenException, JDOMException, IOException, SAXException, TransformerException {

        MCRTransformerHelper tfhelper = call.getTransformerHelper();

        switch (call.getMethod()) {
            case "form":
                tfhelper.handleForm(call);
                break;
            case "preload":
                tfhelper.handlePreload(call);
                break;
            case "include":
                tfhelper.handleInclude(call);
                break;
            case "getAdditionalParameters":
                tfhelper.handleGetAdditionalParameters(call);
                break;
            case "bind":
                tfhelper.handleBind(call);
                break;
            case "unbind":
                tfhelper.handleUnbind();
                break;
            case "repeat":
                tfhelper.repeatTransformer.handleRepeat(call);
                break;
            case "bindRepeatPosition":
                tfhelper.repeatTransformer.handleBindRepeatPosition(call);
                break;
            case "controls":
                tfhelper.repeatTransformer.handleControls(call);
                break;
            case "input":
                tfhelper.handleInput(call);
                break;
            case "textarea":
                tfhelper.handleTextarea(call);
                break;
            case "button":
                tfhelper.handleSubmitButton(call);
                break;
            case "if":
            case "when":
                tfhelper.handleTest(call);
                break;
            case "source":
                tfhelper.handleSource(call);
                break;
            case "cancel":
                tfhelper.handleCancel(call);
                break;
            case "post-processor":
                tfhelper.handlePostProcessor(call);
                break;
            case "param":
                tfhelper.handleParam(call);
                break;
            case "select":
                tfhelper.selectTransformer.handleSelect(call);
                break;
            case "option":
                tfhelper.selectTransformer.handleOption(call);
                break;
            case "cleanup-rule":
                tfhelper.handleCleanupRule(call);
                break;
            case "load-resource":
                tfhelper.handleLoadResource(call);
                break;
            case "output":
                tfhelper.handleOutput(call);
                break;
            case "validate":
                tfhelper.validationTransformer.handleValidationRule(call);
                break;
            case "hasValidationError":
                tfhelper.validationTransformer.handleHasValidationError(call);
                break;
            case "display-validation-message":
                tfhelper.validationTransformer.handleDisplayValidationMessage(call);
                break;
            case "display-validation-messages":
                tfhelper.validationTransformer.handleDisplayValidationMessages(call);
                break;
            case "replaceXPaths":
                tfhelper.handleReplaceXPaths(call);
                break;
            default:
                ;
        }
    }
}

class MCRTransformerHelperCall {

    private static final String PREFIX_XMLNS = "xmlns:";

    private MCRTransformerHelper helper;

    private String method;

    private Map<String, String> attributeMap = new HashMap<>();

    private Map<String, String> namespaceMap = new HashMap<>();

    private Element returnElement = new Element("result");

    MCRTransformerHelperCall(String href) {
        StringTokenizer uriTokenizer = new StringTokenizer(href, ":");
        uriTokenizer.nextToken(); // remove schema

        String sessionID = uriTokenizer.nextToken();
        this.helper = lookupTransformerHelper(sessionID);

        this.method = uriTokenizer.nextToken();

        parseParameters(uriTokenizer);
    }

    private void parseParameters(StringTokenizer uriTokenizer) {
        while (uriTokenizer.hasMoreTokens()) {
            String name = uriTokenizer.nextToken("=").substring(1);
            String value = uriTokenizer.nextToken("&").substring(1);
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);

            if (name.startsWith(PREFIX_XMLNS)) {
                String prefix = name.substring(PREFIX_XMLNS.length());
                namespaceMap.put(prefix, value);
            } else {
                attributeMap.put(name, value);
            }
        }
    }

    MCRTransformerHelper lookupTransformerHelper(String sessionID) {
        MCREditorSession session = MCREditorSessionStoreUtils.getSessionStore().getSession(sessionID);
        return session.getTransformerHelper();
    }

    MCRTransformerHelper getTransformerHelper() {
        return helper;
    }

    String getMethod() {
        return method;
    }

    String getAttributeValue(String name) {
        return attributeMap.get(name);
    }

    String getAttributeValueOrDefault(String name, String defaultValue) {
        return attributeMap.getOrDefault(name, defaultValue);
    }

    Map<String, String> getAttributeMap() {
        return attributeMap;
    }

    void registerDeclaredNamespaces() {
        namespaceMap.entrySet().stream()
            .map(entry -> Namespace.getNamespace(entry.getKey(), entry.getValue()))
            .forEach(MCRConstants::registerNamespace);
    }

    Element getReturnElement() {
        return returnElement;
    }
}
