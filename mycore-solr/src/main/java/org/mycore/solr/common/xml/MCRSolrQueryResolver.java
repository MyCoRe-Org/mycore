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
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.mycore.common.content.MCRURLContent;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.search.MCRSolrURL;

/**
 * <p>solr:{optional core}:query</p>
 * <p>solr:main:q=%2BobjectType%3Ajpjournal</p>
 *
 * @author Matthias Eichner
 */
public class MCRSolrQueryResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        AtomicReference<String> urlQuery = new AtomicReference<>(href.substring(href.indexOf(":") + 1));
        AtomicReference<SolrClient> solrClient = new AtomicReference<>(MCRSolrClientFactory.getMainSolrClient());
        int clientIndex = urlQuery.get().indexOf(":");
        if (clientIndex != -1) {
            String coreID = urlQuery.get().substring(0, clientIndex);
            MCRSolrClientFactory.get(coreID).ifPresent(core -> {
                solrClient.set(core.getClient());
                urlQuery.set(urlQuery.get().substring(clientIndex + 1));
            });
        }
        MCRSolrURL solrURL = new MCRSolrURL((HttpSolrClient) solrClient.get(), urlQuery.get());
        try {
            MCRURLContent result = new MCRURLContent(solrURL.getUrl());
            return result.getSource();
        } catch (IOException e) {
            throw new TransformerException("Unable to get input stream from solr: " + solrURL.getUrl(), e);
        }
    }

}
