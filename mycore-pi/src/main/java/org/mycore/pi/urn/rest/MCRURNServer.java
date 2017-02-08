package org.mycore.pi.urn.rest;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.BiConsumer;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.pi.MCRPIRegistrationInfo;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by chi on 25.01.17.
 * @author shermann
 * @author Huu Chi Vu
 */
public class MCRURNServer {
    private static final Logger LOGGER = LogManager.getLogger();

    private final UsernamePasswordCredentials usernamePassword;

    private final Function<MCRPIRegistrationInfo, URL> urnURLResolver;

    /**
     * Creates a new operator with the given configuration.
     */
    public MCRURNServer(UsernamePasswordCredentials usernamePassword,
                        Function<MCRPIRegistrationInfo, URL> urnURLResolver) {
        this.usernamePassword = usernamePassword;
        this.urnURLResolver = urnURLResolver;
    }

    private CloseableHttpClient getHttpClient() {
        BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
        basicCredentialsProvider.setCredentials(new AuthScope("ipaddress", 443, "realm"), usernamePassword);

        return HttpClientBuilder
                .create()
                .setDefaultCredentialsProvider(basicCredentialsProvider)
                .build();
    }

    private String getServiceURL() {
        return "https://restapi.nbn-resolving.org/urns/";
    }

    private MCREpicurLite createEpicureLite(MCRPIRegistrationInfo urnInfo) {
        MCREpicurLite elp = new MCREpicurLite(urnInfo);
        elp.setLogin(usernamePassword.getUserName());
        elp.setPassword(usernamePassword.getPassword());
        elp.setUrl(urnURLResolver.apply(urnInfo));
        return elp;
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
        try (CloseableHttpClient httpClient = getHttpClient()) {
            return httpClient.execute(httpHead).getStatusLine().getStatusCode();
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
    public void put(MCRPIRegistrationInfo urnInfo, BiConsumer<HttpResponse, MCREpicurLite> callback) {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            MCREpicurLite elp = createEpicureLite(urnInfo);

            String content = new XMLOutputter(Format.getPrettyFormat()).outputString(elp.getEpicurLite());
            LOGGER.debug("EpicurLite \"put\" for urn " + elp.getUrn().getIdentifier() + "\n" + content);

            HttpPut httpPut = new HttpPut(getServiceURL() + elp.getUrn().getIdentifier());
            httpPut.addHeader("content-type", "application/xml");
            httpPut.setEntity(new StringEntity(content, "UTF-8"));

            callback.accept(httpClient.execute(httpPut), elp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    public int post(MCRPIRegistrationInfo urnInfo) {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            MCREpicurLite elp = createEpicureLite(urnInfo);

            String content = new XMLOutputter(Format.getPrettyFormat()).outputString(elp.getEpicurLite());
            LOGGER.debug("EpicurLite \"posted\" for urn " + elp.getUrn().getIdentifier() + "\n" + content);

            HttpPost httpPost = new HttpPost(getServiceURL() + elp.getUrn().getIdentifier() + "/links");
            httpPost.setHeader("content-type", "application/xml");
            httpPost.setEntity(new StringEntity(content, "UTF-8"));

            return httpClient.execute(httpPost).getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
