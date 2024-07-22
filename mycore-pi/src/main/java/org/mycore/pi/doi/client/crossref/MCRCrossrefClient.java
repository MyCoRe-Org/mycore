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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.pi.exceptions.MCRDatacenterException;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.util.MCRResultOrException;
import org.mycore.services.http.MCRHttpUtils;
import org.mycore.services.http.MCRQueryParameter;

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
        return HttpClientBuilder.create()
            .setUserAgent(MCRHttpUtils.getHttpUserAgent())
            .build();
    }

    public void doMDUpload(Document metadata) throws MCRPersistentIdentifierException {
        final HttpPost postRequest;

        try {
            URI uri = getUploadURI();
            postRequest = new HttpPost(uri);
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
            client.<MCRResultOrException<Object, MCRPersistentIdentifierException>>execute(postRequest, response -> {
                final int statusCode = response.getCode();
                final HttpEntity entity = response.getEntity();
                String message = "";

                return switch (statusCode) {
                    case 200:
                        if (entity != null && LOGGER.isDebugEnabled()) {
                            try (InputStream inputStream = entity.getContent()) {
                                message = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                                LOGGER.debug(message);
                            }
                        }
                        yield MCRResultOrException.ofResult(null); // everything OK!
                    case 503:
                        LOGGER.error("Seems like the quota of 10000 Entries is exceeded!");
                        yield MCRResultOrException.ofResult(null);
                    default:
                        if (entity != null) {
                            try (InputStream inputStream = entity.getContent()) {
                                message = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                            }
                        }
                        yield MCRResultOrException.ofException(
                            new MCRDatacenterException(
                                String.format(Locale.ROOT, "Error while doMDUpload: (%d)%s%s%s", statusCode,
                                    response.getReasonPhrase(), System.lineSeparator(), message)));
                };
            }).getResultOrThrow();
        } catch (IOException e) {
            throw new MCRDatacenterException(String.format(Locale.ROOT, "Error while sending request to %s", host), e);
        }
    }

    private URI getUploadURI() throws URISyntaxException {
        List<MCRQueryParameter> parameters = List.of(
            new MCRQueryParameter(USER_PARAM, this.username),
            new MCRQueryParameter(PASSWORD_PARAM, this.password),
            new MCRQueryParameter(OPERATION_PARAM, OPERATION_DOMDUPLOAD));
        return new URI(String.format(Locale.ROOT, "https://%s/%s%s",
            this.host,
            DEPOSIT_PATH,
            MCRQueryParameter.toQueryString(parameters)));
    }

}
