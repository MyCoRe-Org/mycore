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
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.mycore.common.content.MCRURLContent;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrCore;
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
    public Source resolve(String href, String base) throws TransformerException {
        Matcher matcher = OLD_URI_PATTERN.matcher(href);
        Matcher newMatcher = URI_PATTERN.matcher(href);

        if (matcher.matches()) {
            Optional<String> core = Optional.ofNullable(matcher.group(CORE_GROUP_NAME));
            Optional<String> requestHandler = Optional.ofNullable(matcher.group(REQUEST_HANDLER_GROUP_NAME));
            Optional<String> query = Optional.ofNullable(matcher.group(QUERY_GROUP_NAME));

            if (!newMatcher.matches()) {
                printMismatchWarning(href, base);
            } else {
                String newCore = newMatcher.group(CORE_GROUP_NAME);
                String newRequestHandler = newMatcher.group(REQUEST_HANDLER_GROUP_NAME);
                String newQuery = newMatcher.group(QUERY_GROUP_NAME);
                if (!Objects.equals(core.orElse(null), newCore) ||
                    !Objects.equals(requestHandler.orElse(null), newRequestHandler) ||
                    !Objects.equals(query.orElse(null), newQuery)) {
                    printMismatchWarning(href, base);
                }
            }

            HttpSolrClient client = core.flatMap(MCRSolrClientFactory::get)
                .map(MCRSolrCore::getClient)
                .orElse((HttpSolrClient) MCRSolrClientFactory.getMainSolrClient());

            if (query.isPresent()) {
                MCRSolrURL solrURL = new MCRSolrURL(client, query.get());
                requestHandler.map("/"::concat).ifPresent(solrURL::setRequestHandler);

                try {
                    return new MCRURLContent(solrURL.getUrl()).getSource();
                } catch (IOException e) {
                    throw new TransformerException("Unable to get input stream from solr: " + solrURL.getUrl(), e);
                }
            }
        }

        throw new IllegalArgumentException("Did not understand uri: " + href);
    }

    private void printMismatchWarning(String href, String base) {
        LOGGER.warn("The uri {} is probably not encoded correctly. See (MCR-2872)", href);
    }
}
