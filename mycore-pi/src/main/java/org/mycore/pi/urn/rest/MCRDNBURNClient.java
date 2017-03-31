package org.mycore.pi.urn.rest;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.urn.MCRHttpUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by chi on 25.01.17.
 * @author shermann
 * @author Huu Chi Vu
 */
public class MCRDNBURNClient {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Function<MCRPIRegistrationInfo, MCREpicurLite> epicurProvider;

    /**
     * Creates a new operator with the given configuration.
     */
    public MCRDNBURNClient(Function<MCRPIRegistrationInfo, MCREpicurLite> epicurProvider) {
        this.epicurProvider = epicurProvider;
    }

    private String getServiceURL() {
        return "https://restapi.nbn-resolving.org/urns/";
    }

    /**
     * Please see list of status codes and their meaning:
     * <br><br>
     * 204 No Content: URN is in database. No further information asked.<br>
     * 301 Moved Permanently: The given URN is replaced with a newer version. This newer version should be used instead.<br>
     * 404 Not Found: The given URN is not registered in system.<br>
     * 410 Gone: The given URN is registered in system but marked inactive.<br>
     *
     * @return the status code of the request
     */
    public <T> Optional<T> head(MCRPIRegistrationInfo urnInfo,
                                BiFunction<HttpResponse, MCRPIRegistrationInfo, T> handler) {
        HttpHead httpHead = new HttpHead(getServiceURL() + urnInfo.getIdentifier());
        return httpClientExec(httpHead)
                .map(response -> handler.apply(response, urnInfo));
    }

    /**
     * Registers a new URN.
     * <br><br>
     * 201 Created: URN-Record is successfully created.<br>
     * 303 See other: At least one of the given URLs is already registered under another URN, which means you should use this existing URN instead of assigning a new one<br>
     * 409 Conflict: URN-Record already exists and can not be created again.<br>
     *
     * @return the status code of the request
     */
    public <T> Optional<T> put(MCRPIRegistrationInfo urnInfo, BiFunction<HttpResponse, MCREpicurLite, T> handler) {
        MCREpicurLite elp = epicurProvider.apply(urnInfo);

        String content = epicureToString(elp);
        LOGGER.debug("EpicurLite \"put\" for urn " + elp.getUrn().getIdentifier() + "\n" + content);

        String putURL = getServiceURL() + elp.getUrn().getIdentifier();

        return setupRequest(HttpPut::new, putURL, content)
                .flatMap(this::httpClientExec)
                .map(response -> handler.apply(response, elp));
    }


    /**
     * Updates all URLS to a given URN.
     * <br><br>
     * 204 URN was updated successfully<br>
     * 301 URN has a newer version<br>
     * 303 URL is registered for another URN<br>
     *
     * @return the status code of the request
     */
    public <T> Optional<T> post(MCRPIRegistrationInfo urnInfo, BiFunction<HttpResponse, MCREpicurLite, T> handler) {
        MCREpicurLite elp = epicurProvider.apply(urnInfo);
        String content = epicureToString(elp);
        LOGGER.debug("EpicurLite \"posted\" for urn " + elp.getUrn().getIdentifier() + "\n" + content);

        String postURL = getServiceURL() + elp.getUrn().getIdentifier() + "/links";

        return setupRequest(HttpPost::new, postURL, content)
                .flatMap(this::httpClientExec)
                .map(response -> handler.apply(response, elp));
    }

    private Optional<CloseableHttpResponse> httpClientExec(HttpUriRequest request) {
        try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient()) {
            return Optional.of(httpClient.execute(request));
        } catch (IOException e) {
            if (e instanceof ClientProtocolException) {
                LOGGER.error("There is an http protocol error.");
            } else {
                LOGGER.error("There is a problem or the connection was aborted.");
            }
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private RequestConfig noRedirect() {
        return RequestConfig
                .copy(RequestConfig.DEFAULT)
                .setRedirectsEnabled(false)
                .build();
    }

    private <R extends HttpEntityEnclosingRequestBase> Optional<R> setupRequest(Supplier<R> requestSupp, String url,
                                                                                String content) {
        R request = requestSupp.get();
        try {
            request.setURI(new URI(url));
            request.setHeader("content-type", "application/xml");
            request.setConfig(noRedirect());
            request.setEntity(new StringEntity(content, "UTF-8"));
            return Optional.of(request);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private String epicureToString(MCREpicurLite elp) {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(elp.toXML());
    }
}
