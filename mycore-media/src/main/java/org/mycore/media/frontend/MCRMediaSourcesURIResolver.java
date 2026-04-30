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

package org.mycore.media.frontend;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.media.video.MCRMediaSource;
import org.mycore.media.video.MCRMediaSourceProvider;
import org.mycore.services.http.MCRURLQueryParameter;

/**
 * URI resolver used by XSLT 3 function wrappers for media source lookup.
 *
 * Supported operations:
 * <ul>
 * <li>{@code mediasources:getSources?derivateId=...&path=...&userAgent=...}</li>
 * </ul>
 */
public class MCRMediaSourcesURIResolver implements URIResolver {

    private static final String GET_SOURCES_METHOD = "getSources";

    private static final String DERIVATE_ID_ARG = "derivateId";

    private static final String PATH_ARG = "path";

    private static final String USER_AGENT_ARG = "userAgent";

    private static final String[] EMPTY_PARAMETERS = new String[0];

    private final MCRMediaSourceProviderFactory mediaSourceProviderFactory;

    public MCRMediaSourcesURIResolver() {
        this((derivateId, path, userAgent) -> new MCRMediaSourceProvider(derivateId, path, userAgent,
            () -> EMPTY_PARAMETERS).getSources());
    }

    MCRMediaSourcesURIResolver(MCRMediaSourceProviderFactory mediaSourceProviderFactory) {
        this.mediaSourceProviderFactory = mediaSourceProviderFactory;
    }

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

        if (!GET_SOURCES_METHOD.equals(method)) {
            throw new TransformerException("Unknown method: " + method);
        }

        requireArguments(argumentMap, GET_SOURCES_METHOD, DERIVATE_ID_ARG, PATH_ARG);

        try {
            Optional<String> userAgent = Optional.ofNullable(argumentMap.get(USER_AGENT_ARG))
                .filter(s -> !s.isBlank());
            List<MCRMediaSource> sources = mediaSourceProviderFactory.create(argumentMap.get(DERIVATE_ID_ARG),
                argumentMap.get(PATH_ARG), userAgent);
            return createResultSource(sources);
        } catch (IOException | URISyntaxException e) {
            throw new TransformerException("Could not resolve media sources for " + href, e);
        }
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

    private static Source createResultSource(List<MCRMediaSource> mediaSources) {
        Element root = new Element("sources");

        mediaSources.stream()
            .map(MCRMediaSourcesURIResolver::createSourceElement)
            .forEach(root::addContent);

        return new JDOMSource(root);
    }

    private static Element createSourceElement(MCRMediaSource source) {
        Element sourceElement = new Element("source");
        sourceElement.setAttribute("src", source.getUri());
        sourceElement.setAttribute("type", source.getType().getMimeType());
        return sourceElement;
    }

    @FunctionalInterface
    interface MCRMediaSourceProviderFactory {
        List<MCRMediaSource> create(String derivateId, String path, Optional<String> userAgent)
            throws IOException, URISyntaxException;
    }
}
