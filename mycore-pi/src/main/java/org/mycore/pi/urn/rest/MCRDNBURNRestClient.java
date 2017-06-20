package org.mycore.pi.urn.rest;

import static org.apache.http.entity.ContentType.APPLICATION_XML;

import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.pi.MCRPIRegistrationInfo;

/**
 * Created by chi on 25.01.17.
 *
 * @author shermann
 * @author Huu Chi Vu
 */
public class MCRDNBURNRestClient {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Function<MCRPIRegistrationInfo, MCREpicurLite> epicurProvider;

    /**
     * Creates a new operator with the given configuration.
     */
    public MCRDNBURNRestClient(Function<MCRPIRegistrationInfo, MCREpicurLite> epicurProviderFunc) {
        this.epicurProvider = epicurProviderFunc;
    }

    private String getBaseServiceURL(MCRPIRegistrationInfo urn) {
        return "https://restapi.nbn-resolving.org/urns/" + urn.getIdentifier();
    }

    private String getUpdateURL(MCRPIRegistrationInfo urn) {
        return getBaseServiceURL(urn) + "/links";
    }

    /**
     * Please see list of status codes and their meaning:
     * <br><br>
     * 204 No Content: URN is in database. No further information asked.<br>
     * 301 Moved Permanently: The given URN is replaced with a newer version.
     * This newer version should be used instead.<br>
     * 404 Not Found: The given URN is not registered in system.<br>
     * 410 Gone: The given URN is registered in system but marked inactive.<br>
     *
     * @return the status code of the request
     */
    public Optional<Date> register(MCRPIRegistrationInfo urn) {
        String url = getBaseServiceURL(urn);
        CloseableHttpResponse response = MCRHttpsClient.head(url);

        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            LOGGER.warn("HEAD request for {} returns no status line.", url);
            return Optional.empty();
        }

        int headStatus = statusLine.getStatusCode();

        String identifier = urn.getIdentifier();
        switch (headStatus) {
            case HttpStatus.SC_NO_CONTENT:
                LOGGER.info("URN {} is in database. No further information asked.", identifier);
                LOGGER.info("Performing update of url.");
                return update(urn);
            case HttpStatus.SC_NOT_FOUND:
                LOGGER.info("The given URN {} is not registered in system.", identifier);
                return registerNew(urn);
            case HttpStatus.SC_MOVED_PERMANENTLY:
                LOGGER.warn("The given URN {} is replaced with a newer version. \n "
                    + "This newer version should be used instead.", identifier);
                break;
            case HttpStatus.SC_GONE:
                LOGGER.warn("The given URN {} is registered in system but marked inactive.", identifier);
                break;
            default:
                LOGGER.warn("Could not handle request for urnInfo {} Status code {}.", identifier, headStatus);
                break;
        }

        return Optional.empty();
    }

    /**
     * Registers a new URN.
     * <br><br>
     * 201 Created: URN-Record is successfully created.<br>
     * 303 See other: At least one of the given URLs is already registered under another URN,
     * which means you should use this existing URN instead of assigning a new one<br>
     * 409 Conflict: URN-Record already exists and can not be created again.<br>
     *
     * @return the status code of the request
     */
    private Optional<Date> registerNew(MCRPIRegistrationInfo urn) {
        MCREpicurLite elp = epicurProvider.apply(urn);
        String elpXML = elp.asXMLString();
        String baseServiceURL = getBaseServiceURL(urn);
        CloseableHttpResponse response = MCRHttpsClient.put(baseServiceURL, APPLICATION_XML.toString(), elpXML);

        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            LOGGER.warn("PUT request for {} returns no status line.", baseServiceURL);
            return Optional.empty();
        }

        int putStatus = statusLine.getStatusCode();

        String identifier = urn.getIdentifier();
        URL url = elp.getUrl();
        switch (putStatus) {
            case HttpStatus.SC_CREATED:
                LOGGER.info("URN " + identifier + " registered to " + url);
                return Optional.ofNullable(response.getFirstHeader("Last-Modified"))
                    .map(Header::getValue)
                    .map(DateTimeFormatter.RFC_1123_DATE_TIME::parse)
                    .map(Instant::from)
                    .map(Date::from);
            case HttpStatus.SC_SEE_OTHER:
                LOGGER.warn("At least one of the given URLs is already registered under another URN, "
                    + "which means you should use this existing URN instead of assigning a new one.");
                LOGGER.warn("URN {} could NOT registered to {}.", identifier, url);
                break;
            case HttpStatus.SC_CONFLICT:
                LOGGER.warn("URN-Record already exists and can not be created again.");
                LOGGER.warn("URN {} could NOT registered to {}.", identifier, url);
                break;
            default:
                LOGGER.warn("Could not handle urnInfo request: status={}, urn={}, url={}.", putStatus, identifier, url);
                LOGGER.warn("Epicur Lite:");
                LOGGER.warn(elpXML);
                break;
        }

        return Optional.empty();
    }

    /**
     * Updates all URLS to a given URN.
     * <br><br>
     * 204 No Content: URN was updated successfully<br>
     * 301 Moved Permanently: URN has a newer version<br>
     * 303 See other: URL is registered for another URN<br>
     *
     * @return the status code of the request
     */

    private Optional<Date> update(MCRPIRegistrationInfo urn) {
        MCREpicurLite elp = epicurProvider.apply(urn);
        String elpXML = elp.asXMLString();
        String updateURL = getUpdateURL(urn);
        CloseableHttpResponse response = MCRHttpsClient.post(updateURL, APPLICATION_XML.toString(), elpXML);
        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            LOGGER.warn("POST request for {} returns no status line.", updateURL);
            return Optional.empty();
        }

        int postStatus = statusLine.getStatusCode();

        String identifier = urn.getIdentifier();
        switch (postStatus) {
            case HttpStatus.SC_NO_CONTENT:
                LOGGER.info("URN {} updated to {}.", identifier, elp.getUrl());
                return Optional.ofNullable(response.getFirstHeader("Last-Modified"))
                    .map(Header::getValue)
                    .map(DateTimeFormatter.RFC_1123_DATE_TIME::parse)
                    .map(Instant::from)
                    .map(Date::from);
            case HttpStatus.SC_MOVED_PERMANENTLY:
                LOGGER.warn("URN {} has a newer version.", identifier);
                break;
            case HttpStatus.SC_SEE_OTHER:
                LOGGER.warn("URL {} is registered for another URN.", elp.getUrl());
                break;
            default:
                LOGGER.warn("URN {} could not be updated. Status {}.", identifier, postStatus);
                break;
        }

        return Optional.empty();
    }
}
