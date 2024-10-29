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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Function;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.pi.util.MCRHttpUtils;

/**
 * Convinience class for sending http requests to the DNB urn service api.
 *
 * Created by chi on 08.05.17.
 *
 * @author Huu Chi Vu
 */
public class MCRHttpsClient {
    final private static Logger LOGGER = LogManager.getLogger(MCRHttpsClient.class);

    private static RequestConfig noRedirect() {
        return RequestConfig
            .copy(RequestConfig.DEFAULT)
            .setRedirectsEnabled(false)
            .build();
    }

    public static <R> R head(String url, HttpClientResponseHandler<R> responseHandler) {
        return head(url, Optional.empty(), responseHandler);
    }

    public static <R> R head(String url, Optional<UsernamePasswordCredentials> credentials,
        HttpClientResponseHandler<R> responseHandler) {
        HttpHead httpHead = new HttpHead(url);
        try {
            try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient().build()) {
                return httpClient.execute(httpHead, responseHandler);
            }
        } catch (IOException e) {
            LOGGER.error("There is a problem or the connection was aborted for URL: {}", url, e);
        }

        return null;
    }

    public static <R> R get(String url, Optional<UsernamePasswordCredentials> credentials,
        HttpClientResponseHandler<R> responseHandler) {
        HttpGet get = new HttpGet(url);

        if (credentials.isPresent()) {
            setAuthorizationHeader(get, credentials);
        }
        try {
            try (CloseableHttpClient httpsClient = MCRHttpUtils.getHttpClient().build()) {
                return httpsClient.execute(get, responseHandler);
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return null;
    }

    public static <R> R put(String url, String contentType, String data, HttpClientResponseHandler<R> responseHandler) {
        return put(url, contentType, data, Optional.empty(), responseHandler);
    }

    public static <R> R put(String url, String contentType, String data,
        Optional<UsernamePasswordCredentials> credentials, HttpClientResponseHandler<R> responseHandler) {
        return request(HttpPut::new, url, contentType, new StringEntity(data, StandardCharsets.UTF_8), credentials,
            responseHandler);
    }

    public static <R> R post(String url, String contentType, String data,
        HttpClientResponseHandler<R> responseHandler) {
        return post(url, contentType, data, Optional.empty(), responseHandler);
    }

    public static <R> R post(String url, String contentType, String data,
        Optional<UsernamePasswordCredentials> credentials, HttpClientResponseHandler<R> responseHandler) {
        return request(HttpPost::new, url, contentType, new StringEntity(data, StandardCharsets.UTF_8), credentials,
            responseHandler);
    }

    public static <R> R patch(String url, String contentType, String data,
        HttpClientResponseHandler<R> responseHandler) {
        return patch(url, contentType, data, Optional.empty(), responseHandler);
    }

    public static <R> R patch(String url, String contentType, String data,
        Optional<UsernamePasswordCredentials> credentials, HttpClientResponseHandler<R> responseHandler) {
        return request(HttpPatch::new, url, contentType, new StringEntity(data, StandardCharsets.UTF_8), credentials,
            responseHandler);
    }

    public static <R, T extends HttpUriRequestBase> R request(Function<URI, T> requestSupp,
        String url, String contentType, HttpEntity entity, HttpClientResponseHandler<R> responseHandler) {
        return request(requestSupp, url, contentType, entity, Optional.empty(), responseHandler);
    }

    /**
     * Sets the authorization header to the provided http request object.
     *
     * Unfortunately the procedure with {@link org.apache.http.client.CredentialsProvider} is not working with the
     * DNB urn service api.
     * */
    private static HttpRequest setAuthorizationHeader(HttpRequest request,
        Optional<UsernamePasswordCredentials> credentials) {

        String auth = credentials.get().getUserName() + ":" + String.copyValueOf(credentials.get().getUserPassword());
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
        request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        return request;
    }

    public static <R, T extends HttpUriRequestBase> R request(Function<URI, T> requestSupp,
        String url, String contentType, HttpEntity entity, Optional<UsernamePasswordCredentials> credentials,
        HttpClientResponseHandler<R> responseHandler) {

        try {
            try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient().build()) {
                T request = requestSupp.apply(new URI(url));

                if (credentials.isPresent()) {
                    setAuthorizationHeader(request, credentials);
                }
                request.setHeader("content-type", contentType);
                request.setConfig(noRedirect());
                request.setEntity(entity);

                return httpClient.execute(request, responseHandler);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Wrong format for URL: {}", url, e);
        } catch (ClientProtocolException e) {
            LOGGER.error("There is a HTTP protocol error for URL: {}", url, e);
        } catch (IOException e) {
            LOGGER.error("There is a problem or the connection was aborted for URL: {}", url, e);
        }

        return null;
    }
}
