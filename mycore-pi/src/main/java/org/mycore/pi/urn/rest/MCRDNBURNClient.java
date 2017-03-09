package org.mycore.pi.urn.rest;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.BiConsumer;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.urn.MCRHttpUtils;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

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
    public int head(MCRPIRegistrationInfo urnInfo, Consumer<HttpResponse> callback) {
        HttpHead httpHead = new HttpHead(getServiceURL() + urnInfo.getIdentifier());
        try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient()) {
            return httpClient.execute(httpHead).getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T head(MCRPIRegistrationInfo urnInfo, BiFunction<HttpResponse, MCRPIRegistrationInfo,  T> handler) {
        HttpHead httpHead = new HttpHead(getServiceURL() + urnInfo.getIdentifier());
        try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient()) {
            return handler.apply(httpClient.execute(httpHead), urnInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    public <T> T put(MCRPIRegistrationInfo urnInfo, BiFunction<HttpResponse, MCREpicurLite, T> handler) {
        try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient()) {
            MCREpicurLite elp = epicurProvider.apply(urnInfo);

            String content = new XMLOutputter(Format.getPrettyFormat()).outputString(elp.toXML());
            LOGGER.debug("EpicurLite \"put\" for urn " + urnInfo.getIdentifier() + "\n" + content);

            HttpPut httpPut = new HttpPut(getServiceURL() + urnInfo.getIdentifier());
            httpPut.addHeader("content-type", "application/xml");
            httpPut.setEntity(new StringEntity(content, "UTF-8"));
            httpPut.setConfig(noRedirect());

            return handler.apply(httpClient.execute(httpPut), elp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private RequestConfig noRedirect() {
        return RequestConfig
                .copy(RequestConfig.DEFAULT)
                .setRedirectsEnabled(false)
                .build();
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
    public <T> T post(MCRPIRegistrationInfo urnInfo, BiFunction<HttpResponse, MCREpicurLite, T> handler) {
        try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient()) {
            MCREpicurLite elp = epicurProvider.apply(urnInfo);

            String content = new XMLOutputter(Format.getPrettyFormat()).outputString(elp.toXML());
            LOGGER.debug("EpicurLite \"posted\" for urn " + urnInfo.getIdentifier() + "\n" + content);

            HttpPost httpPost = new HttpPost(getServiceURL() + urnInfo.getIdentifier() + "/links");
            httpPost.setHeader("content-type", "application/xml");
            httpPost.setEntity(new StringEntity(content, "UTF-8"));
            httpPost.setConfig(noRedirect());

            return handler.apply(httpClient.execute(httpPost), elp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
