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

package org.mycore.pi.doi.client.crossref;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.pi.exceptions.MCRDatacenterException;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import jakarta.validation.constraints.NotNull;

public class MCRCrossrefClient {

    public static final Logger LOGGER = LogManager.getLogger();

    private static final String HTTP_SCHEME_PREFIX = "http://";

    private static final String HTTPS_SCHEME_PREFIX = "https://";

    private static final String NOT_NULL_MESSAGE = "%s needs to be not null!";

    private static final String DEPOSIT_PATH = "servlet/deposit";

    private static final String OPERATION_PARAM = "operation";

    private static final String USER_PARAM = "login_id";

    private static final String PASSWORD_PARAM = "login_passwd";

    private static final String OPERATION_DOMDUPLOAD = "doMDUpload";

    private static final XMLOutputter METADATA_OUTPUTTER = new XMLOutputter(Format.getPrettyFormat());

    private String host, username, password;

    public MCRCrossrefClient(@NotNull String host, @NotNull String username, @NotNull String password) {
        if (host == null) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, NOT_NULL_MESSAGE, "Host"));
        }
        if (username == null) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, NOT_NULL_MESSAGE, "Username"));
        }
        if (password == null) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, NOT_NULL_MESSAGE, "Password"));
        }

        this.host = host;
        this.username = username;
        this.password = password;

        if (host.endsWith("/")) {
            this.host = this.host.substring(0, host.length() - 1);
        }

        if (this.host.startsWith(HTTP_SCHEME_PREFIX)) {
            this.host = this.host.substring(HTTP_SCHEME_PREFIX.length());
        } else if (this.host.startsWith(HTTPS_SCHEME_PREFIX)) {
            this.host = this.host.substring(HTTPS_SCHEME_PREFIX.length());
        }
    }

    private static CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
    }

    public void doMDUpload(Document metadata) throws MCRPersistentIdentifierException {
        final HttpPost postRequest;

        try {
            final URIBuilder uriBuilder;
            uriBuilder = new URIBuilder("https://" + this.host + "/" + DEPOSIT_PATH);
            addAuthParameters(uriBuilder);
            uriBuilder.addParameter(OPERATION_PARAM, OPERATION_DOMDUPLOAD);
            postRequest = new HttpPost(uriBuilder.build());
        } catch (URISyntaxException e) {
            throw new MCRPersistentIdentifierException(
                String.format(Locale.ROOT, "Can not build a valid URL with  host: %s", this.host));
        }

        final String metadataXmlAsString = METADATA_OUTPUTTER.outputString(metadata);

        HttpEntity reqEntity = MultipartEntityBuilder.create()
            .addBinaryBody("fname", metadataXmlAsString.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_XML,
                "crossref_query.xml")
            .build();
        postRequest.setEntity(reqEntity);

        try (CloseableHttpClient client = getHttpClient()) {
            try (CloseableHttpResponse response = client.execute(postRequest)) {
                final int statusCode = response.getStatusLine().getStatusCode();
                final HttpEntity entity = response.getEntity();
                String message = "";

                switch (statusCode) {
                    case 200:
                        if (entity != null) {
                            try (InputStream inputStream = entity.getContent()) {
                                List<String> doc = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
                                message = doc.stream().collect(Collectors.joining(System.lineSeparator()));
                                LOGGER.debug(message);
                            }
                        }
                        return; // everything OK!
                    case 503:
                        LOGGER.error("Seems like the quota of 10000 Entries is exceeded!");
                    default:
                        if (entity != null) {
                            try (InputStream inputStream = entity.getContent()) {
                                List<String> doc = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
                                message = doc.stream().collect(Collectors.joining(System.lineSeparator()));
                            }
                        }
                        throw new MCRDatacenterException(
                            String.format(Locale.ROOT, "Error while doMDUpload: (%d)%s%s%s", statusCode,
                                response.getStatusLine().getReasonPhrase(), System.lineSeparator(), message));
                }
            }
        } catch (IOException e) {
            throw new MCRDatacenterException(String.format(Locale.ROOT, "Error while sending request to %s", host), e);
        }
    }

    private void addAuthParameters(URIBuilder uriBuilder) {
        uriBuilder.addParameter(USER_PARAM, this.username);
        uriBuilder.addParameter(PASSWORD_PARAM, this.password);
    }

}
