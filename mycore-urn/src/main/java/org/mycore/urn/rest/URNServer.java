/**
 * 
 */
package org.mycore.urn.rest;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.urn.epicurlite.EpicurLite;
import org.mycore.urn.hibernate.MCRURN;

/**
 * @author shermann
 *
 */
@Deprecated
public class URNServer {

    private static final Logger LOGGER = LogManager.getLogger(URNServer.class);

    protected URNServerConfiguration configuration;

    /**
     * The http client to send messages to the urn service.
     */
    protected HttpClient httpClient;

    /**
     * Creates a new operator with the given configuration.
     */
    public URNServer(URNServerConfiguration configuration) {
        this.configuration = configuration;
    }

    protected URNServerConfiguration getConfiguration() {
        return this.configuration;
    }

    protected synchronized HttpClient getHttpClient() {
        if (this.httpClient == null) {
            // configure the client
            MultiThreadedHttpConnectionManager connectionMgr = new MultiThreadedHttpConnectionManager();
            connectionMgr.getParams().setDefaultMaxConnectionsPerHost(10);
            connectionMgr.getParams().setMaxTotalConnections(10);
            // init the client
            this.httpClient = new HttpClient(connectionMgr);
            getHttpClient().getState().setCredentials(new AuthScope("ipaddress", 443, "realm"),
                new UsernamePasswordCredentials(getConfiguration().getLogin(), getConfiguration().getPassword()));
        }
        return this.httpClient;
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
    public int head(MCRURN urn) {
        HeadMethod headMethod = new HeadMethod(getConfiguration().getServiceURL() + urn);
        try {
            int status = getHttpClient().executeMethod(headMethod);
            return status;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            headMethod.releaseConnection();
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
    public int put(EpicurLite elp) {
        PutMethod put = null;
        int status = -1;
        try {
            elp.setLogin(getConfiguration().getLogin());
            elp.setPassword(getConfiguration().getPassword());
            String content = new XMLOutputter(Format.getPrettyFormat()).outputString(elp.getEpicurLite());

            LOGGER.debug("EpicurLite \"put\" for urn " + elp.getUrn().toString() + "\n" + content);

            put = new PutMethod(getConfiguration().getServiceURL() + elp.getUrn().toString());
            put.setDoAuthentication(true);
            StringRequestEntity requestEntity = new StringRequestEntity(content, "application/xml", "UTF-8");
            put.setRequestEntity(requestEntity);
            status = getHttpClient().executeMethod(put);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (put != null) {
                put.releaseConnection();
            }
        }

        return status;
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
    public int post(EpicurLite elp) {
        PostMethod post = null;
        int status = -1;
        try {
            elp.setLogin(getConfiguration().getLogin());
            elp.setPassword(getConfiguration().getPassword());
            String content = new XMLOutputter(Format.getPrettyFormat()).outputString(elp.getEpicurLite());

            LOGGER.debug("EpicurLite \"posted\" for urn " + elp.getUrn().toString() + "\n" + content);

            post = new PostMethod(getConfiguration().getServiceURL() + elp.getUrn().toString() + "/links");
            post.setDoAuthentication(true);
            StringRequestEntity requestEntity = new StringRequestEntity(content, "application/xml", "UTF-8");
            post.setRequestEntity(requestEntity);
            status = getHttpClient().executeMethod(post);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
        }
        return status;
    }

}
