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

package org.mycore.solr.index.file.tika;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

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

    public <T extends Throwable> void extractText(InputStream is, ThrowingConsumer<JsonObject, T> responseConsumer)
        throws IOException, T {
        HttpPut httpPut = new HttpPut(url + "tika/text");
        httpPut.setHeader("Accept", "application/json");
        httpPut.setEntity(new InputStreamEntity(is));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Tika server returned " + response.getStatusLine().getStatusCode());
                }

                try (InputStream responseStream = response.getEntity().getContent();
                    InputStreamReader isr = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {
                    JsonStreamParser parser = new JsonStreamParser(isr);
                    JsonElement root = parser.next();
                    JsonObject rootObject = root.getAsJsonObject();

                    responseConsumer.accept(rootObject);
                }
            }
        }
    }

    public interface ThrowingConsumer<T, E extends Throwable> {
        void accept(T t) throws E;
    }
}
