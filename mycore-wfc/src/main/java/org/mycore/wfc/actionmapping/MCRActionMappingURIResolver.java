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

package org.mycore.wfc.actionmapping;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.xsl.uriresolver.MCRURIResolverResponse;
import org.mycore.services.http.MCRURLQueryParameter;

/**
 * {@link URIResolver} that resolves workflow action mapping URLs for use in XSLT 3 function wrappers.
 */
public class MCRActionMappingURIResolver implements URIResolver {

    private static final String GET_URL_FOR_ID_METHOD = "getURLforID";

    private static final String GET_URL_FOR_COLLECTION_METHOD = "getURLforCollection";

    private static final String ACTION_ARG = "action";

    private static final String ID_ARG = "id";

    private static final String COLLECTION_ARG = "collection";

    private static final String ABSOLUTE_ARG = "absolute";

    /**
     * Resolves the given action mapping query and returns the resulting URL as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:getURLforID?action={action}&amp;id={id}&amp;absolute={true|false}
     *   &lt;scheme&gt;:getURLforCollection?action={action}&amp;collection={collection}&amp;absolute={true|false}
     * </pre>
     * <p>Example request:
     * <pre>
     *   someScheme:getURLforID?action=edit&amp;id=mcr_document_00000001&amp;absolute=true
     *   someScheme:getURLforCollection?action=search&amp;collection=mcr&amp;absolute=false
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <string>https://example.org/edit/mcr_document_00000001</string>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping a {@code <string>} element containing the resolved URL,
     *         or an empty string if no URL is found
     * @throws TransformerException if the URI is malformed, a required argument is missing,
     *                              or the method is unknown
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] parts = href.split(":", 2);
        if (parts.length != 2) {
            throw new TransformerException("Invalid href format: " + href);
        }

        String[] methodParts = parts[1].split("\\?", 2);
        if (methodParts.length != 2) {
            throw new TransformerException("Invalid href format: " + href);
        }

        String method = methodParts[0];
        Map<String, String> argumentMap = getArgumentMap(methodParts[1]);

        return switch (method) {
            case GET_URL_FOR_ID_METHOD -> {
                requireArguments(argumentMap, GET_URL_FOR_ID_METHOD, ACTION_ARG, ID_ARG, ABSOLUTE_ARG);
                yield MCRURIResolverResponse.ofString(Objects.toString(MCRURLRetriever.getURLforID(
                    argumentMap.get(ACTION_ARG),
                    argumentMap.get(ID_ARG),
                    Boolean.parseBoolean(argumentMap.get(ABSOLUTE_ARG))), ""));
            }
            case GET_URL_FOR_COLLECTION_METHOD -> {
                requireArguments(argumentMap, GET_URL_FOR_COLLECTION_METHOD, ACTION_ARG, COLLECTION_ARG,
                    ABSOLUTE_ARG);
                yield MCRURIResolverResponse.ofString(Objects.toString(MCRURLRetriever.getURLforCollection(
                    argumentMap.get(ACTION_ARG),
                    argumentMap.get(COLLECTION_ARG),
                    Boolean.parseBoolean(argumentMap.get(ABSOLUTE_ARG))), ""));
            }
            default -> throw new TransformerException("Unknown method: " + method);
        };
    }

    private static void requireArguments(Map<String, String> argumentMap, String functionName, String... requiredArgs)
        throws TransformerException {
        for (String arg : requiredArgs) {
            if (!argumentMap.containsKey(arg)) {
                throw new TransformerException(
                    "Missing required argument '" + arg + "' in function " + functionName + "!");
            }
        }
    }

    private static Map<String, String> getArgumentMap(String arguments) {
        return MCRURLQueryParameter.parse(arguments)
            .stream()
            .collect(Collectors.toMap(MCRURLQueryParameter::name, MCRURLQueryParameter::value));
    }

}
