package org.mycore.pi.urn.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by chi on 08.05.17.
 *
 * @author Huu Chi Vu
 */
public class MCRHttpsClient {
    private static Logger LOGGER = LogManager.getLogger();

    private static RequestConfig noRedirect() {
        return RequestConfig
            .copy(RequestConfig.DEFAULT)
            .setRedirectsEnabled(false)
            .build();
    }

    public static CloseableHttpClient getHttpsClient() {
        return HttpClientBuilder
            .create()
            .setConnectionTimeToLive(1, TimeUnit.MINUTES)
            .setSSLContext(SSLContexts.createSystemDefault())
            .build();
    }

    public static CloseableHttpResponse head(String url) {
        HttpHead httpHead = new HttpHead(url);
        try (CloseableHttpClient httpClient = getHttpsClient()) {
            return httpClient.execute(httpHead);
        } catch (IOException e) {
            LOGGER.error("There is a problem or the connection was aborted for URL: " + url, e);
        }

        return null;
    }

    public static CloseableHttpResponse put(String url, String contentType, String data) {
        return request(HttpPut::new, url, contentType, new StringEntity(data, "UTF-8"));
    }

    public static CloseableHttpResponse post(String url, String contentType, String data) {
        return request(HttpPost::new, url, contentType, new StringEntity(data, "UTF-8"));
    }

    public static <R extends HttpEntityEnclosingRequestBase> CloseableHttpResponse request(
        Supplier<R> requestSupp, String url,
        String contentType, HttpEntity entity) {

        try (CloseableHttpClient httpClient = getHttpsClient()) {
            R request = requestSupp.get();
            request.setURI(new URI(url));
            request.setHeader("content-type", contentType);
            request.setConfig(noRedirect());
            request.setEntity(entity);

            return httpClient.execute(request);
        } catch (URISyntaxException e) {
            LOGGER.error("Worng format for URL: " + url, e);
        } catch (ClientProtocolException e) {
            LOGGER.error("There is a HTTP protocol error for URL: " + url, e);
        } catch (IOException e) {
            LOGGER.error("There is a problem or the connection was aborted for URL: " + url, e);
        }

        return null;
    }
}
