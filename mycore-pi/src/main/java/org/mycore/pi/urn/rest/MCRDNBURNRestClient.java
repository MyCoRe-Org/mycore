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

package org.mycore.pi.urn.rest;

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

import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.pi.MCRPIRegistrationInfo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
        return getBaseServiceURL() + "urn/" + urn.getIdentifier();
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
        return MCRHttpsClient.get(url, credentials, response -> {
            int status = response.getCode();

            String identifier = urn.getIdentifier();
            switch (status) {
                case HttpStatus.SC_OK -> {
                    LOGGER.info("URN {} is in database. No further information asked", identifier);
                    LOGGER.info("Performing update of url");
                    return update(urn);
                }
                case HttpStatus.SC_NOT_FOUND -> {
                    LOGGER.info("URN {} is not registered", identifier);
                    return registerNew(urn);
                }
                default -> {
                    LOGGER.error("Error while check if URN {} exists using url {}.", identifier, url);
                    logFailure("", response, status, urn.getIdentifier(), bundle.getUrl());
                }
            }

            return Optional.empty();
        });
    }

    public Optional<JsonObject> getRegistrationInfo(MCRPIRegistrationInfo urn) {
        String identifier = urn.getIdentifier();
        return getRegistrationInfo(identifier);
    }

    public static Optional<JsonObject> getRegistrationInfo(String identifier) {
        String url = getBaseServiceURL() + "/urn/" + identifier;
        return MCRHttpsClient.get(url, Optional.empty(), response -> {
            int statusCode = response.getCode();
            switch (statusCode) {
                case HttpStatus.SC_OK -> {
                    HttpEntity entity = response.getEntity();
                    try {
                        Reader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
                        JsonElement jsonElement = JsonParser.parseReader(reader);
                        return Optional.of(jsonElement.getAsJsonObject());
                    } catch (Exception e) {
                        LOGGER.error(() -> "Could not read Response from " + url, e);
                    }
                }
                default -> {
                    LOGGER.error("Error while get registration info for URN {} using url {}.", identifier, url);
                    logFailure("", response, statusCode, identifier, url);
                }
            }
            return Optional.empty();
        });

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
        String json = bundle.toJSON(MCRURNJsonBundle.Format.REGISTER);
        String baseServiceURL = getBaseServiceURL();
        return MCRHttpsClient.post(baseServiceURL, ContentType.APPLICATION_JSON.toString(), json, credentials,
            response -> {
                int postStatus = response.getCode();

                String identifier = urn.getIdentifier();
                URL url = bundle.getUrl();
                switch (postStatus) {
                    case HttpStatus.SC_CREATED -> {
                        LOGGER.info("URN {} registered to {}", identifier, url);
                        return Optional.ofNullable(response.getFirstHeader("date"))
                            .map(Header::getValue)
                            .map(DateTimeFormatter.RFC_1123_DATE_TIME::parse)
                            .map(Instant::from)
                            .map(Date::from);
                    }
                    default -> {
                        LOGGER.error("Error while register new URN {} using url {}.", identifier, url);
                        logFailure(json, response, postStatus, identifier, url);
                    }
                }
                return Optional.empty();
            });

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
        String json = bundle.toJSON(MCRURNJsonBundle.Format.UPDATE);
        String updateURL = getUpdateURL(urn);
        return MCRHttpsClient.patch(updateURL, ContentType.APPLICATION_JSON.toString(), json, credentials, response -> {
            int patchStatus = response.getCode();

            String identifier = urn.getIdentifier();
            switch (patchStatus) {
                case HttpStatus.SC_NO_CONTENT -> {
                    LOGGER.info("URN {} updated to {}", () -> identifier, bundle::getUrl);
                    return Optional.ofNullable(response.getFirstHeader("date"))
                        .map(Header::getValue)
                        .map(DateTimeFormatter.RFC_1123_DATE_TIME::parse)
                        .map(Instant::from)
                        .map(Date::from);
                }
                default -> {
                    LOGGER.error("Error while update URN {} using url {}.", identifier, updateURL);
                    logFailure(json, response, patchStatus, identifier, bundle.getUrl());
                }
            }

            return Optional.empty();
        });
    }

    public static void logFailure(String json, ClassicHttpResponse response, int postStatus, String identifier,
        URL url) {
        logFailure(json, response, postStatus, identifier, url == null ? null : url.toString());
    }

    public static void logFailure(String json, ClassicHttpResponse response, int status, String identifier,
        String url) {
        HttpEntity entity = response.getEntity();
        LOGGER.error(
            "Could not handle urn http request: status={}, " +
                "urn={}, url={} json={}",
            status, identifier, url, json);
        try {
            String errBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            LOGGER.error("Server error message: {}", errBody);
        } catch (IOException | ParseException e) {
            LOGGER.error("Could not get error body from http request", e);
        }
    }
}
