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

package org.mycore.solr.common.xml;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.HttpSolrClientBase;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.services.http.MCRHttpUtils;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.search.MCRSolrURL;

/**
 * <pre>
 *  Usage:   solr:{optional core}:query
 *  Example: solr:q=%2BobjectType%3Ajpjournal
 *
 *  Usage:   solr:{optional core}:{optional requestHandler:&lt;requestHandler&gt;}:query
 *  Example: solr:requestHandler:browse-inventory:q=%2BobjectType%3Ajpjournal
 *           solr:mysolrcore:requestHandler:browse-inventory:q=%2BobjectType%3Ajpjournal
 * </pre>
 *
 * @author Sebastian Hofmann
 */
public class MCRSolrQueryResolver implements URIResolver {

    public static final String QUERY_GROUP_NAME = "query";

    public static final String CORE_GROUP_NAME = "core";

    private static final String REQUEST_HANDLER_QUALIFIER = "requestHandler";

    public static final String REQUEST_HANDLER_GROUP_NAME = REQUEST_HANDLER_QUALIFIER;

    // not allowed chars for cores are / \ and : according to
    // https://stackoverflow.com/questions/29977519/what-makes-an-invalid-core-name
    // assume they are the same for the requestHandler
    private static final Pattern URI_PATTERN = Pattern
        .compile("\\Asolr:((?!" + REQUEST_HANDLER_QUALIFIER + ")(?<" + CORE_GROUP_NAME + ">[^/:\\\\]+):)?("
            + REQUEST_HANDLER_QUALIFIER + ":(?<" + REQUEST_HANDLER_GROUP_NAME + ">[^/:\\\\]+):)?(?<"
            + QUERY_GROUP_NAME + ">.+)\\z");

    // TODO: remove this pattern in 2023.06 release
    private static final Pattern OLD_URI_PATTERN = Pattern
        .compile("\\Asolr:((?!" + REQUEST_HANDLER_QUALIFIER + ")(?<" + CORE_GROUP_NAME + ">[a-zA-Z0-9-_]+):)?("
            + REQUEST_HANDLER_QUALIFIER + ":(?<" + REQUEST_HANDLER_GROUP_NAME + ">[a-zA-Z0-9-_]+):)?(?<"
            + QUERY_GROUP_NAME + ">.+)\\z");

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public Source resolve(String href, String base) {
        Matcher matcher = OLD_URI_PATTERN.matcher(href);
        Matcher newMatcher = URI_PATTERN.matcher(href);

        if (matcher.matches()) {
            Optional<String> core = Optional.ofNullable(matcher.group(CORE_GROUP_NAME));
            Optional<String> requestHandler = Optional.ofNullable(matcher.group(REQUEST_HANDLER_GROUP_NAME));
            Optional<String> query = Optional.ofNullable(matcher.group(QUERY_GROUP_NAME));

            if (!newMatcher.matches()) {
                printMismatchWarning(href);
            } else {
                String newCore = newMatcher.group(CORE_GROUP_NAME);
                String newRequestHandler = newMatcher.group(REQUEST_HANDLER_GROUP_NAME);
                String newQuery = newMatcher.group(QUERY_GROUP_NAME);
                if (!Objects.equals(core.orElse(null), newCore) ||
                    !Objects.equals(requestHandler.orElse(null), newRequestHandler) ||
                    !Objects.equals(query.orElse(null), newQuery)) {
                    printMismatchWarning(href);
                }
            }

            HttpSolrClientBase client = core.flatMap(MCRSolrCoreManager::get)
                .map(MCRSolrCore::getClient)
                .orElse((HttpSolrClientBase) MCRSolrCoreManager.getMainSolrClient());

            if (query.isPresent()) {
                MCRSolrURL solrURL = new MCRSolrURL(client, query.get());
                requestHandler.map("/"::concat).ifPresent(solrURL::setRequestHandler);

                URI solrRequestURI;
                try {
                    solrRequestURI = solrURL.getUrl().toURI();
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Could not create URI from " + solrURL.getUrl(), e);
                }

                HttpRequest.Builder solrRequestBuilder = MCRHttpUtils.getRequestBuilder().uri(solrRequestURI);

                MCRSolrAuthenticationManager.getInstance()
                    .applyAuthentication(solrRequestBuilder, MCRSolrAuthenticationLevel.SEARCH);

                try (HttpClient httpClient = MCRHttpUtils.getHttpClient()) {
                    HttpRequest solrRequest = solrRequestBuilder.build();
                    HttpResponse<byte[]> response = httpClient.send(solrRequest,
                        HttpResponse.BodyHandlers.ofByteArray());

                    if (response.statusCode() != 200) {
                        throw new MCRException(
                            "Error while executing request: " + response.version() + " " + response.statusCode());
                    }

                    MCRByteContent result = new MCRByteContent(response.body());
                    result.setSystemId(solrRequest.uri().toString());
                    return result.getSource();
                } catch (InterruptedException | IOException e) {
                    throw new MCRException("Error while executing request", e);
                }
            }
        }

        throw new IllegalArgumentException("Did not understand uri: " + href);
    }

    private void printMismatchWarning(String href) {
        LOGGER.warn("The uri {} is probably not encoded correctly. See (MCR-2872)", href);
    }
}
