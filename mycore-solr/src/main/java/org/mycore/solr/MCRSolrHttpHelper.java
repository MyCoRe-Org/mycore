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

package org.mycore.solr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

public class MCRSolrHttpHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    public static CloseableHttpClient createHttpClient() {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.build();
    }

    public static SolrClient getSolrClient(URI solrServerURL) {
        return new HttpSolrClient.Builder(solrServerURL.toString())
                .withConnectionTimeout(30000)
                .withSocketTimeout(30000)
                .build();
    }

    public static void printResponse(CloseableHttpResponse response) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
            BufferedReader bufferedReader = new BufferedReader(reader)) {
            bufferedReader.lines().forEach(LOGGER::info);
        }
    }
}
