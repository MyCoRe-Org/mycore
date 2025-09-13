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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRConstants;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCREditorSessionStoreUtils;

/**
 * Parses the URI of a document() call within xed transformation and 
 * provides methods to access attributes etc.
 * 
 * @author Frank Lützenkirchen
 */
class MCRTransformerHelperCall {

    private static final String PREFIX_XMLNS = "xmlns:";

    private MCRTransformationState helper;

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

    MCRTransformationState lookupTransformerHelper(String sessionID) {
        MCREditorSession session = MCREditorSessionStoreUtils.getSessionStore().getSession(sessionID);
        return session.getTransformationState();
    }

    MCRTransformationState getTransformerHelper() {
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
