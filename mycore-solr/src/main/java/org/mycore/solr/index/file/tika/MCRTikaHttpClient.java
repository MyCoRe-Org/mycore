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

package org.mycore.solr.index.file.tika;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.mycore.services.http.MCRHttpUtils;
import org.mycore.solr.MCRSolrUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A simple HTTP client to communicate with a Tika server.
 *
 * @author Sebastian Hofmann
 */
public class MCRTikaHttpClient {

    private final String url;

    public MCRTikaHttpClient(String url) {
        this.url = url.endsWith("/") ? url : url + "/";

    }

    public <T extends Throwable> void extractText(InputStream is, ThrowingConsumer<TreeNode, T> responseConsumer)
        throws IOException, T {
        HttpRequest httpPut = MCRSolrUtils.getRequestBuilder()
            .uri(URI.create(url + "tika/text"))
            .setHeader("Accept", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> is))
            .build();

        try (HttpClient client = MCRHttpUtils.getHttpClient()) {
            HttpResponse<InputStream> response = client.send(httpPut, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Tika server returned " + response.statusCode());
            }

            try (InputStream responseStream = response.body();
                InputStreamReader isr = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {
                JsonParser parser = new ObjectMapper().createParser(isr);
                TreeNode treeNode = parser.readValueAsTree();
                responseConsumer.accept(treeNode);
            }
        } catch (InterruptedException e) {
            throw new IOException("Tika response did not arrive in time.", e);
        }
    }

    public interface ThrowingConsumer<T, E extends Throwable> {
        void accept(T t) throws E;
    }
}
