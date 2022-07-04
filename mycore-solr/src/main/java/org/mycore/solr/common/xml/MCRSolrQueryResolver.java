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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

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

    private static final String REQUEST_HANDLER_QUALIFIER = "requestHandler";

    public static final String REQUEST_HANDLER_GROUP_NAME = REQUEST_HANDLER_QUALIFIER;

    public static final String QUERY_GROUP_NAME = "query";

    public static final String CORE_GROUP_NAME = "core";

    private static final Pattern URI_PATTERN = Pattern
        .compile("\\Asolr:((?<" + CORE_GROUP_NAME + ">[a-zA-Z0-9-]+):)?(" + REQUEST_HANDLER_QUALIFIER + ":(?<"
            + REQUEST_HANDLER_GROUP_NAME + ">[a-zA-Z0-9-]+):)?(?<" + QUERY_GROUP_NAME + ">.+)\\z");

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        Matcher matcher = URI_PATTERN.matcher(href);

        if (matcher.matches()) {
            Optional<String> core = Optional.ofNullable(matcher.group(CORE_GROUP_NAME));
            Optional<String> requestHandler = Optional.ofNullable(matcher.group(REQUEST_HANDLER_GROUP_NAME));
            Optional<String> query = Optional.ofNullable(matcher.group(QUERY_GROUP_NAME));

            HttpSolrClient client = core.map(MCRSolrClientFactory::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
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
        throw new IllegalArgumentException("Didn't understand uri: " + href);
    }
}
