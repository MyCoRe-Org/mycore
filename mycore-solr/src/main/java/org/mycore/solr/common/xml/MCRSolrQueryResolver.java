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

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.mycore.common.content.MCRURLContent;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.search.MCRSolrURL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <pre>
 *  solr:{optional core}:query
 *  solr:{optional core}:{optional requestHandler:<requestHandler>}:query
 *  solr:q=%2BobjectType%3Ajpjournal
 * </pre>
 *
 * @author Matthias Eichner
 * @author mcrsherm
 */
public class MCRSolrQueryResolver implements URIResolver {

    private static final String REQUEST_HANDLER_QUALIFIER = "requestHandler";

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        AtomicReference<String> urlQuery = new AtomicReference<>(href.substring(href.indexOf(":") + 1));
        AtomicReference<SolrClient> solrClient = new AtomicReference<>(MCRSolrClientFactory.getMainSolrClient());

        int clientIndex = urlQuery.get().indexOf(":");
        if (clientIndex != -1 && !(urlQuery.get().substring(0, clientIndex + REQUEST_HANDLER_QUALIFIER.length()).contains(REQUEST_HANDLER_QUALIFIER))) {
            String coreID = urlQuery.get().substring(0, clientIndex);
            MCRSolrClientFactory.get(coreID).ifPresent(core -> {
                solrClient.set(core.getClient());
            });
        }

        urlQuery.set(href.substring(href.lastIndexOf(":") + 1));
        MCRSolrURL solrURL = new MCRSolrURL((HttpSolrClient) solrClient.get(), urlQuery.get());

        int handlerIndex = href.indexOf(REQUEST_HANDLER_QUALIFIER);
        if (handlerIndex > -1) {
            solrURL.setRequestHandler("/" + href.substring(handlerIndex).split(":")[1]);
        }

        try {
            MCRURLContent result = new MCRURLContent(solrURL.getUrl());
            return result.getSource();
        } catch (IOException e) {
            throw new TransformerException("Unable to get input stream from solr: " + solrURL.getUrl(), e);
        }
    }
}
