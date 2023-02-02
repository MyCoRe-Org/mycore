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

package org.mycore.pi.urn.rest;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.pi.MCRPIRegistrationInfo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * <p>Class for registering and updating urn managed by the DNB.</p>
 * @see <a href="https://api.nbn-resolving.org/v2/docs/index.html">URN-Service API</a>
 * Created by chi on 25.01.17.
 *
 * @author Huu Chi Vu
 * @author shermann
 *
 */
public class MCRDNBURNRestClient {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Function<MCRPIRegistrationInfo, MCRURNJsonBundle> jsonProvider;

    private final Optional<UsernamePasswordCredentials> credentials;

    /**
     * Creates a new operator with the given configuration.
     *
     * @param bundleProvider
     */
    public MCRDNBURNRestClient(Function<MCRPIRegistrationInfo, MCRURNJsonBundle> bundleProvider) {
        this(bundleProvider, Optional.empty());
    }

    /**
     * @param bundleProvider the provider creating the required json
     * @param credentials the credentials needed for authentication
     * */
    public MCRDNBURNRestClient(Function<MCRPIRegistrationInfo, MCRURNJsonBundle> bundleProvider,
        Optional<UsernamePasswordCredentials> credentials) {
        this.jsonProvider = bundleProvider;
        this.credentials = credentials;
    }

    /**
     * Returns the base url of the urn registration service.
     *
     * @deprecated see {@link MCRDNBURNRestClient#getBaseServiceURL()}
     * */
    @Deprecated
    protected String getBaseServiceURL(MCRPIRegistrationInfo urn) {
        return this.getBaseServiceURL();
    }

    /**
     * Returns the base url of the urn registration service.
     *
     * @return the base url as set in mycore property MCR.PI.URNGranular.API.BaseURL
     * */
    public static String getBaseServiceURL() {
        return MCRConfiguration2.getString("MCR.PI.URNGranular.API.BaseURL")
            .orElse("https://api.nbn-resolving.org/sandbox/v2/") + "urns/";
    }

    /**
     * Returns the base url for checking the existence of a given urn.
     * @param urn the {@link MCRPIRegistrationInfo} to test
     *
     * @return the request url
     * */
    protected String getBaseServiceCheckExistsURL(MCRPIRegistrationInfo urn) {
        return getUpdateURL(urn);
    }

    /**
     * Returns the url for updating the urls assigned to a given urn.
     *
     * @param urn the urn
     * @return the url for updating the urls
     * */
    protected String getUpdateURL(MCRPIRegistrationInfo urn) {
        return getBaseServiceURL() + "urn/" + urn.getIdentifier() + "/my-urls/";
    }

    /**
     * <p>Please see list of status codes and their meaning:</p>
     *
     * <p>204 No Content: URN is in database. No further information asked.</p>
     * <p>301 Moved Permanently: The given URN is replaced with a newer version.
     * This newer version should be used instead.</p>
     * <p>404 Not Found: The given URN is not registered in system.</p>
     * <p>410 Gone: The given URN is registered in system but marked inactive.</p>
     *
     * @return the registration/update date
     */
    public Optional<Date> register(MCRPIRegistrationInfo urn) {
        MCRURNJsonBundle bundle = this.jsonProvider.apply(urn);
        String url = getBaseServiceCheckExistsURL(urn);
        CloseableHttpResponse response = MCRHttpsClient.get(url, credentials);
        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            LOGGER.warn("GET request for {} returns no status line.", url);
            return Optional.empty();
        }

        int status = statusLine.getStatusCode();

        String identifier = urn.getIdentifier();
        switch (status) {
            case HttpStatus.SC_OK:
                LOGGER.info("URN {} is in database. No further information asked", identifier);
                LOGGER.info("Performing update of url");
                return update(urn);
            case HttpStatus.SC_NOT_FOUND:
                LOGGER.info("URN {} is not registered", identifier);
                return registerNew(urn);
            default:
                logFailure("", response, status, urn.getIdentifier(), bundle.getUrl());
                break;
        }

        return Optional.empty();
    }

    public Optional<JsonObject> getRegistrationInfo(MCRPIRegistrationInfo urn) {
        String identifier = urn.getIdentifier();
        return getRegistrationInfo(identifier);
    }

    public static Optional<JsonObject> getRegistrationInfo(String identifier) {
        String url = MCRDNBURNRestClient.getBaseServiceURL() + "/urn/" + identifier;
        CloseableHttpResponse response = MCRHttpsClient.get(url, Optional.empty());

        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
            case HttpStatus.SC_OK:
                HttpEntity entity = response.getEntity();
                try {
                    Reader reader = new InputStreamReader(entity.getContent());
                    JsonElement jsonElement = JsonParser.parseReader(reader);
                    return Optional.of(jsonElement.getAsJsonObject());
                } catch (Exception e) {
                    LOGGER.error("Could not read Response from " + url);
                }
                break;
            default:
                MCRDNBURNRestClient.logFailure("", response, statusCode, identifier, url);
                break;
        }
        return Optional.empty();
    }

    /**
     * <p>Registers a new URN.<p/>
     *
     * <p>201 Created: URN-Record is successfully created.</p>
     * <p>303 See other: At least one of the given URLs is already registered under another URN,
     * which means you should use this existing URN instead of assigning a new one</p>
     * <p>409 Conflict: URN-Record already exists and can not be created again.</p>
     *
     * @return the registration date
     */
    private Optional<Date> registerNew(MCRPIRegistrationInfo urn) {
        MCRURNJsonBundle bundle = jsonProvider.apply(urn);
        String json = bundle.toJSON(MCRURNJsonBundle.Format.register);
        String baseServiceURL = getBaseServiceURL();
        CloseableHttpResponse response = MCRHttpsClient.post(baseServiceURL, APPLICATION_JSON.toString(), json,
            credentials);

        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            LOGGER.warn("POST request for {} returns no status line", baseServiceURL);
            return Optional.empty();
        }

        int postStatus = statusLine.getStatusCode();

        String identifier = urn.getIdentifier();
        URL url = bundle.getUrl();
        switch (postStatus) {
            case HttpStatus.SC_CREATED:
                LOGGER.info("URN {} registered to {}", identifier, url);
                return Optional.ofNullable(response.getFirstHeader("date"))
                    .map(Header::getValue)
                    .map(DateTimeFormatter.RFC_1123_DATE_TIME::parse)
                    .map(Instant::from)
                    .map(Date::from);
            default:
                logFailure(json, response, postStatus, identifier, url);
                break;
        }
        return Optional.empty();
    }

    /**
     * <p>Updates all URLS to a given URN.</p>
     *
     * <p>204 No Content: URN was updated successfully</p>
     * <p>301 Moved Permanently: URN has a newer version</p>
     * <p>303 See other: URL is registered for another URN</p>
     *
     * @return the status code of the request
     */

    private Optional<Date> update(MCRPIRegistrationInfo urn) {
        MCRURNJsonBundle bundle = jsonProvider.apply(urn);
        String json = bundle.toJSON(MCRURNJsonBundle.Format.update);
        String updateURL = getUpdateURL(urn);
        CloseableHttpResponse response = MCRHttpsClient.patch(updateURL, APPLICATION_JSON.toString(), json,
            credentials);
        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            LOGGER.warn("PATCH request for {} returns no status line", updateURL);
            return Optional.empty();
        }

        int patchStatus = statusLine.getStatusCode();

        String identifier = urn.getIdentifier();
        switch (patchStatus) {
            case HttpStatus.SC_NO_CONTENT:
                LOGGER.info("URN {} updated to {}", identifier, bundle.getUrl());
                return Optional.ofNullable(response.getFirstHeader("date"))
                    .map(Header::getValue)
                    .map(DateTimeFormatter.RFC_1123_DATE_TIME::parse)
                    .map(Instant::from)
                    .map(Date::from);
            default:
                logFailure(json, response, patchStatus, identifier, bundle.getUrl());
                break;
        }

        return Optional.empty();
    }

    public static void logFailure(String json, CloseableHttpResponse response, int postStatus, String identifier,
        URL url) {
        logFailure(json, response, postStatus, identifier, url.toString());
    }

    public static void logFailure(String json, CloseableHttpResponse response, int postStatus, String identifier,
        String url) {
        try (Reader reader = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            LOGGER.error("{}: {} ({})", postStatus,
                jsonElement.getAsJsonObject().get("developerMessage"), identifier);
        } catch (IOException | JsonParseException e) {
            LOGGER.error(
                "Could not handle urn post request: status={}, " +
                    "urn={}, url={} json={}",
                postStatus, identifier, url, json);
        }
    }
}
