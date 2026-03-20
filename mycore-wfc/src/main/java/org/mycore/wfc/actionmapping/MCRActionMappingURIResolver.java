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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.xml.MCRURIResolver;

/**
 * URI resolver used by XSLT 3 function wrappers for workflow action mapping URLs.
 *
 * Supported operations:
 * <ul>
 * <li>{@code actionmapping:getURLforID/?action=...&id=...&absolute=true|false}</li>
 * <li>{@code actionmapping:getURLforCollection/?action=...&collection=...&absolute=true|false}</li>
 * </ul>
 */
public class MCRActionMappingURIResolver implements URIResolver {

    private static final String GET_URL_FOR_ID_METHOD = "getURLforID";

    private static final String GET_URL_FOR_COLLECTION_METHOD = "getURLforCollection";

    private static final String ACTION_ARG = "action";

    private static final String ID_ARG = "id";

    private static final String COLLECTION_ARG = "collection";

    private static final String ABSOLUTE_ARG = "absolute";

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] parts = href.split(":", 2);
        if (parts.length != 2) {
            throw new TransformerException("Invalid href format: " + href);
        }

        String[] methodParts = parts[1].split("/\\?", 2);
        if (methodParts.length != 2) {
            throw new TransformerException("Invalid href format: " + href);
        }

        String method = methodParts[0];
        Map<String, String> argumentMap = getArgumentMap(methodParts[1]);

        return switch (method) {
            case GET_URL_FOR_ID_METHOD -> {
                requireArguments(argumentMap, GET_URL_FOR_ID_METHOD, ACTION_ARG, ID_ARG, ABSOLUTE_ARG);
                yield MCRURIResolver.createStringResponse(Objects.toString(MCRURLRetriever.getURLforID(
                    argumentMap.get(ACTION_ARG),
                    argumentMap.get(ID_ARG),
                    Boolean.parseBoolean(argumentMap.get(ABSOLUTE_ARG))), ""));
            }
            case GET_URL_FOR_COLLECTION_METHOD -> {
                requireArguments(argumentMap, GET_URL_FOR_COLLECTION_METHOD, ACTION_ARG, COLLECTION_ARG,
                    ABSOLUTE_ARG);
                yield MCRURIResolver.createStringResponse(Objects.toString(MCRURLRetriever.getURLforCollection(
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
        Map<String, String> argumentMap = new HashMap<>();
        for (String arg : arguments.split("&")) {
            String[] pair = arg.split("=", 2);
            if (pair.length == 2) {
                argumentMap.put(pair[0], pair[1]);
            }
        }
        return argumentMap;
    }

}
