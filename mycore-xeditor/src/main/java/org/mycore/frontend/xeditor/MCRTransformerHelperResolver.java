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
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

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
            handleXEditorElement(tfhelper, elementName, attributes, result);
        } catch (JDOMException | IOException | SAXException | JaxenException ex) {
            throw new TransformerException(ex);
        }

        JDOMSource source = new JDOMSource(result);
        // Workaround to prevent URI Caching:
        source.setSystemId(source.getSystemId() + Math.random());
        return source;
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

    @SuppressWarnings("PMD.NcssCount")
    private void handleXEditorElement(MCRTransformerHelper tfhelper, String elementName, Map<String, String> attributes,
        Element result) throws JaxenException, JDOMException, IOException, SAXException, TransformerException {
        switch (elementName) {
            case "form":
                tfhelper.handleForm(attributes);
                break;
            case "preload":
                tfhelper.handlePreload(attributes, result);
                break;
            case "include":
                tfhelper.handleInclude(attributes, result);
                break;
            case "getAdditionalParameters":
                tfhelper.handleGetAdditionalParameters(result);
                break;
            case "bind":
                tfhelper.handleBind(attributes);
                break;
            case "unbind":
                tfhelper.handleUnbind();
                break;
            case "repeat":
                tfhelper.handleRepeat(attributes, result);
                break;
            case "bindRepeatPosition":
                tfhelper.handleBindRepeatPosition(result);
                break;
            case "controls":
                tfhelper.handleControls(attributes, result);
                break;
            case "input":
                tfhelper.handleInput(attributes, result);
                break;
            case "textarea":
                tfhelper.handleTextarea(attributes, result);
                break;
            case "button":
                tfhelper.handleSubmitButton(attributes, result);
                break;
            case "if":
            case "when":
                tfhelper.handleTest(attributes, result);
                break;
            case "source":
                tfhelper.handleSource(attributes);
                break;
            case "cancel":
                tfhelper.handleCancel(attributes);
                break;
            case "post-processor":
                tfhelper.handlePostProcessor(attributes);
                break;
            case "param":
                tfhelper.handleParam(attributes);
                break;
            case "select":
                tfhelper.handleSelect(attributes, result);
                break;
            case "option":
                tfhelper.handleOption(attributes, result);
                break;
            case "cleanup-rule":
                tfhelper.handleCleanupRule(attributes);
                break;
            case "load-resource":
                tfhelper.handleLoadResource(attributes);
                break;
            case "output":
                tfhelper.handleOutput(attributes, result);
                break;
            case "validate":
                tfhelper.handleValidationRule(attributes, result);
                break;
            case "hasValidationError":
                tfhelper.handleHasValidationError(result);
                break;
            case "display-validation-message":
                tfhelper.handleDisplayValidationMessage(result);
                break;
            case "display-validation-messages":
                tfhelper.handleDisplayValidationMessages(result);
                break;
            case "replaceXPaths":
                tfhelper.handleReplaceXPaths(attributes, result);
                break;
            default:
                ;
        }
    }
}
