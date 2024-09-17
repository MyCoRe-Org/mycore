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

package org.mycore.solr.cloud.configsets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.services.http.MCRHttpUtils;

/**
 * A zipped Solr config set file that is fetched from a web-based resource.
 */
public class MCRURLConfigSetProvider extends MCRSolrConfigSetProvider {

    private URI uri;

    public MCRURLConfigSetProvider(URI uri) {
        this.uri = uri;
    }

    public MCRURLConfigSetProvider() {
    }

    public URI getUrl() {
        return uri;
    }

    @MCRProperty(name = "URL", required = true)
    public void setUrl(String uri) {
        this.uri = URI.create(uri);
    }

    @Override
    public Supplier<InputStream> getStreamSupplier() {
        return () -> {
            try (HttpClient httpClient = MCRHttpUtils.getHttpClient()) {
                HttpRequest request = MCRHttpUtils.getRequestBuilder().uri(uri).build();
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                return new ByteArrayInputStream(response.body());
            } catch (InterruptedException | IOException e) {
                throw new MCRException(e);
            }
        };
    }
}
